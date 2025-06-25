package no.unit.nva.expansion;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedOrganization;
import no.unit.nva.expansion.model.ExpandedPerson;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedTicket;
import no.unit.nva.expansion.model.cristin.CristinPerson;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.ReceivingOrganizationDetails;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.queue.QueueClient;
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
    private final RawContentRetriever uriRetriever;
    private final QueueClient queueClient;

    public ResourceExpansionServiceImpl(ResourceService resourceService,
                                        TicketService ticketService,
                                        RawContentRetriever authorizedUriRetriever,
                                        RawContentRetriever uriRetriever,
                                        QueueClient queueClient) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.authorizedUriRetriever = authorizedUriRetriever;
        this.uriRetriever = uriRetriever;
        this.queueClient = queueClient;
    }

    @Override
    public Optional<ExpandedDataEntry> expandEntry(Entity dataEntry, boolean useUriContext)
        throws JsonProcessingException, NotFoundException {
        if (dataEntry instanceof Resource resourceFromEvent) {
            // we need to fetch the resource from the database to make sure the files in associatedArtifacts are
            // up-to-date as that is needed in OpenSearch:
            var resourceQueryObject = Resource.resourceQueryObject(resourceFromEvent.getIdentifier());
            var resource = resourceQueryObject.fetch(resourceService).orElseThrow();
            return expandResource(resource, useUriContext);
        } else if (dataEntry instanceof TicketEntry ticketEntry) {
            logger.info("Expanding TicketEntry: {}", ticketEntry.getIdentifier());
            return Optional.of(ExpandedTicket.create(ticketEntry, resourceService, this, ticketService));
        } else if (dataEntry instanceof Message message) {
            logger.info("Expanding Message: {}", message.getIdentifier());
            var ticket = ticketService.fetchTicketByIdentifier(message.getTicketIdentifier());
            return expandEntry(ticket, true);
        } else if (dataEntry instanceof FileEntry fileEntry) {
            logger.info("Expanding resource {} based on update of FileEntry {}",
                        fileEntry.getResourceIdentifier(), fileEntry.getIdentifier());
            var resourceQueryObject = Resource.resourceQueryObject(fileEntry.getResourceIdentifier());
            return resourceQueryObject.fetch(resourceService)
                       .map(resource -> attempt(() -> expandResource(resource, true)).orElseThrow())
                       .orElseGet(() -> logAndProvideEmptyOptional(fileEntry));
        }
        // will throw exception if we want to index a new type that we are not handling yet
        throw new UnsupportedOperationException(UNSUPPORTED_TYPE + dataEntry.getClass().getSimpleName());
    }

    private Optional<ExpandedDataEntry> logAndProvideEmptyOptional(FileEntry fileEntry) {
        logger.info("Resource {} is no longer present -> nothing to expand for fileEntry {}",
                    fileEntry.getResourceIdentifier(), fileEntry.getIdentifier());
        return Optional.empty();
    }

    private Optional<ExpandedDataEntry> expandResource(Resource resource, boolean useUriContext)
        throws JsonProcessingException {
        logger.info("Expanding Resource: {} - {}",
                    resource.getIdentifier(),
                    attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(resource)).orElseThrow());
        var expandedResource = ExpandedResource.fromPublication(uriRetriever,
                                                                resourceService,
                                                                queueClient,
                                                                resource.toPublication());
        var resourceWithContextUri = useUriContext
                                         ? replaceInlineContextWithUriContext(expandedResource)
                                         : replaceContextWithInlineContext(expandedResource);

        return Optional.of(attempt(
            () -> objectMapper.readValue(resourceWithContextUri.toString(), ExpandedResource.class)).orElseThrow());
    }

    @Override
    public ExpandedOrganization getOrganization(Entity dataEntry) throws NotFoundException {
        if (dataEntry instanceof TicketEntry ticketEntry) {
            var organizationId = Optional.ofNullable(ticketEntry.getReceivingOrganizationDetails())
                                     .map(this::resolveOrganizationBySpecificity)
                                     .orElse(getAffiliationFromResourceOwner(ticketEntry));

            var organizationIdentifier = Optional.ofNullable(organizationId)
                                             .map(UriWrapper::fromUri)
                                             .map(UriWrapper::getLastPathElement)
                                             .orElse(null);

            var partOf = RdfUtils.getAllNestedPartOfs(uriRetriever, organizationId)
                             .stream()
                             .map(uri -> new ExpandedOrganization(uri,
                                                                  UriWrapper.fromUri(uri).getLastPathElement(),
                                                                  null))
                             .toList();

            return new ExpandedOrganization(organizationId, organizationIdentifier, partOf);
        }
        return null;
    }

    private URI resolveOrganizationBySpecificity(ReceivingOrganizationDetails receivingOrganizationDetails) {
        return Optional.ofNullable(receivingOrganizationDetails.subOrganizationId())
                   .orElse(receivingOrganizationDetails.topLevelOrganizationId());
    }

    private URI getAffiliationFromResourceOwner(TicketEntry ticketEntry) throws NotFoundException {
        return resourceService.getResourceByIdentifier(ticketEntry.getResourceIdentifier())
                   .getResourceOwner().getOwnerAffiliation();
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

    private ObjectNode replaceContextWithInlineContext(ExpandedResource expandedResource) {
        var objectNode = expandedResource.asJsonNode();
        var jsonLdContext = Publication.getJsonLdContext(UriWrapper.fromHost(API_HOST).getUri());
        objectNode.set(CONTEXT_FIELD_PROPERTY_NAME,
                       attempt(() -> JsonUtils.dtoObjectMapper.readTree(jsonLdContext)).orElseThrow());
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
