package no.unit.nva.expansion;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedOrganization;
import no.unit.nva.expansion.model.ExpandedPerson;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedTicket;
import no.unit.nva.expansion.model.cristin.CristinPerson;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.utils.RdfUtils;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceExpansionServiceImpl implements ResourceExpansionService {

    public static final Logger logger = LoggerFactory.getLogger(ResourceExpansionServiceImpl.class);
    public static final String UNSUPPORTED_TYPE = "Expansion is not supported for type:";
    public static final String CRISTIN = "cristin";
    public static final String PERSON = "person";
    public static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String CONTEXT_URI = new UriWrapper(UriWrapper.HTTPS, API_HOST)
                                                 .addChild("publication", "context")
                                                 .toString();
    public static final String CRISTIN_ID_DELIMITER = "@";
    public static final String FIRST_NAME_CRISTIN_TYPE = "FirstName";
    public static final String PREFERRED_FIRST_NAME_CRISTIN_TYPE = "PreferredFirstName";
    public static final String LAST_NAME_CRISTIN_TYPE = "LastName";
    public static final String PREFERRED_LAST_NAME_CRISTIN_TYPE = "PreferredLastName";
    public static final String CONTEXT_NODE_SELECTOR = "/@context";
    public static final String CONTEXT_FIELD_PROPERTY_NAME = "@context";
    private static final String EXPAND_PERSON_DEFAULT_INFO = "Could not retrieve Cristin Person. Creating default "
                                                             + "expanded person for owner";
    private static final String APPLICATION_JSON = "application/json";
    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final RawContentRetriever authorizedUriRetriever;
    private final UriRetriever uriRetriever;

    public ResourceExpansionServiceImpl(ResourceService resourceService,
                                        TicketService ticketService) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.authorizedUriRetriever = new UriRetriever();
        this.uriRetriever = new UriRetriever();
    }

    public ResourceExpansionServiceImpl(ResourceService resourceService,
                                        TicketService ticketService,
                                        RawContentRetriever authorizedUriRetriever,
                                        UriRetriever uriRetriever) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.authorizedUriRetriever = authorizedUriRetriever;
        this.uriRetriever = uriRetriever;
    }

    @Override
    public ExpandedDataEntry expandEntry(Entity dataEntry) throws JsonProcessingException, NotFoundException {
        if (dataEntry instanceof Resource resource) {
            logger.info("Expanding Resource: {}", resource.getIdentifier());
            var expandedResource = ExpandedResource.fromPublication(authorizedUriRetriever,
                                                                    resource.toPublication(resourceService));
            var resourceWithContextUri = replaceInlineContextWithUriContext(expandedResource);
            return attempt(
                () -> objectMapper.readValue(resourceWithContextUri.toString(), ExpandedResource.class)).orElseThrow();
        } else if (dataEntry instanceof TicketEntry ticketEntry) {
            logger.info("Expanding TicketEntry: {}", ticketEntry.getIdentifier());
            return ExpandedTicket.create((TicketEntry) dataEntry, resourceService, this, ticketService);
        } else if (dataEntry instanceof Message message) {
            logger.info("Expanding Message: {}", message.getIdentifier());
            var ticket = ticketService.fetchTicketByIdentifier(message.getTicketIdentifier());
            return expandEntry(ticket);
        }
        // will throw exception if we want to index a new type that we are not handling yet
        throw new UnsupportedOperationException(UNSUPPORTED_TYPE + dataEntry.getClass().getSimpleName());
    }

    @Override
    public ExpandedOrganization getOrganization(Entity dataEntry) throws NotFoundException {
        if (dataEntry instanceof TicketEntry ticketEntry) {

            var organizationId = Objects.nonNull(ticketEntry.getOwnerAffiliation())
                                      ? ticketEntry.getOwnerAffiliation()
                                      : resourceService.getResourceByIdentifier(ticketEntry.getResourceIdentifier())
                                          .getResourceOwner().getOwnerAffiliation();

            var organizationIdentifier = Optional.ofNullable(organizationId)
                                             .map(UriWrapper::fromUri)
                                             .map(UriWrapper::getLastPathElement)
                                             .orElse(null);

            var partOf = RdfUtils.getAllNestedPartOfs(uriRetriever, organizationId);
            return new ExpandedOrganization(organizationId, organizationIdentifier, partOf);
        }
        return null;
    }

    @Override
    public ExpandedPerson expandPerson(User owner) {
        return attempt(() -> constructUri(owner))
                   .map(uri -> authorizedUriRetriever.getRawContent(uri, APPLICATION_JSON))
                   .map(response -> toExpandedPerson(response.orElse(null), owner))
                   .orElse(failure -> getDefaultExpandedPerson(owner));
    }

    @Override
    public ExpandedMessage expandMessage(Message message) {
        return ExpandedMessage.createEntry(message, this);
    }

    private static boolean hasContextNode(ObjectNode objectNode) {
        return !objectNode.at(CONTEXT_NODE_SELECTOR).isMissingNode();
    }

    private static String extractCristinId(User owner) {
        return owner.toString().split(CRISTIN_ID_DELIMITER)[0];
    }

    private ExpandedPerson toExpandedPerson(String response, User owner) throws JsonProcessingException {
        var cristinPerson = objectMapper.readValue(response, CristinPerson.class);
        var nameMap = cristinPerson.getNameTypeMap();
        return new ExpandedPerson.Builder()
                   .withFirstName(nameMap.get(FIRST_NAME_CRISTIN_TYPE))
                   .withPreferredFirstName(nameMap.get(PREFERRED_FIRST_NAME_CRISTIN_TYPE))
                   .withLastName(nameMap.get(LAST_NAME_CRISTIN_TYPE))
                   .withPreferredLastName(nameMap.get(PREFERRED_LAST_NAME_CRISTIN_TYPE))
                   .withUser(owner)
                   .build();
    }

    private ObjectNode replaceInlineContextWithUriContext(ExpandedResource expandedResource) {
        var objectNode = (ObjectNode) attempt(
            () -> objectMapper.readTree(expandedResource.toJsonString())).orElseThrow();
        if (hasContextNode(objectNode)) {
            objectNode.put(CONTEXT_FIELD_PROPERTY_NAME, CONTEXT_URI);
        }
        return objectNode;
    }

    private ExpandedPerson getDefaultExpandedPerson(User owner) {
        logger.info(EXPAND_PERSON_DEFAULT_INFO);
        return ExpandedPerson.defaultExpandedPerson(owner);
    }

    private URI constructUri(User owner) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(CRISTIN)
                   .addChild(PERSON)
                   .addChild(extractCristinId(owner))
                   .getUri();
    }
}
