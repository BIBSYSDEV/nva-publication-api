package no.unit.nva.expansion;

import static no.unit.nva.expansion.model.ExpandedPerson.FIRST_NAME;
import static no.unit.nva.expansion.model.ExpandedPerson.LAST_NAME;
import static no.unit.nva.expansion.model.ExpandedPerson.PREFERRED_FIRST_NAME;
import static no.unit.nva.expansion.model.ExpandedPerson.PREFERRED_LAST_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedPerson;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedTicket;
import no.unit.nva.expansion.model.cristin.CristinPerson;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceExpansionServiceImpl implements ResourceExpansionService {

    public static final Logger logger = LoggerFactory.getLogger(ResourceExpansionServiceImpl.class);
    public static final String CONTENT_TYPE = "application/json";
    public static final String UNSUPPORTED_TYPE = "Expansion is not supported for type:";
    public static final String CRISTIN = "cristin";
    public static final String PERSON = "person";
    public static final String API_HOST = "API_HOST";
    public static final String CRISTIN_ID_DELIMITER = "@";
    private static final String EXPAND_PERSON_CRISTIN_ERROR = "Could not retrieve Cristin Person. Creating default "
                                                              + "expanded person for owner";
    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final UriRetriever uriRetriever;

    public ResourceExpansionServiceImpl(ResourceService resourceService,
                                        TicketService ticketService) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.uriRetriever = new UriRetriever();
    }

    public ResourceExpansionServiceImpl(ResourceService resourceService,
                                        TicketService ticketService,
                                        UriRetriever uriRetriever) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.uriRetriever = uriRetriever;
    }

    private ExpandedPerson toExpandedPerson(String response, User owner) throws JsonProcessingException {
        var cristinPerson = JsonUtils.dtoObjectMapper.readValue(response, CristinPerson.class);
        var nameMap = cristinPerson.getNameTypeMap();
        return new ExpandedPerson.Builder()
                .withFirstName(nameMap.get(FIRST_NAME))
                .withPreferredFirstName(nameMap.get(PREFERRED_FIRST_NAME))
                .withLastName(nameMap.get(LAST_NAME))
                .withPreferredLastName(nameMap.get(PREFERRED_LAST_NAME))
                .withUser(owner)
                .build();

    }

    @Override
    public ExpandedDataEntry expandEntry(Entity dataEntry) throws JsonProcessingException, NotFoundException {
        if (dataEntry instanceof Resource) {
            return ExpandedResource.fromPublication(uriRetriever, dataEntry.toPublication(resourceService));
        } else if (dataEntry instanceof TicketEntry) {
            return ExpandedTicket.create((TicketEntry) dataEntry, resourceService, this, ticketService);
        } else if (dataEntry instanceof Message) {
            var message = (Message) dataEntry;
            var ticket = ticketService.fetchTicketByIdentifier(message.getTicketIdentifier());
            return expandEntry(ticket);
        }
        // will throw exception if we want to index a new type that we are not handling yet
        throw new UnsupportedOperationException(UNSUPPORTED_TYPE + dataEntry.getClass().getSimpleName());
    }

    @Override
    public Set<URI> getOrganizationIds(Entity dataEntry) throws NotFoundException {
        if (dataEntry instanceof TicketEntry) {
            var resourceIdentifier = ((TicketEntry) dataEntry).extractPublicationIdentifier();
            var resource = resourceService.getResourceByIdentifier(resourceIdentifier);
            return Optional.ofNullable(resource.getResourceOwner().getOwnerAffiliation())
                    .stream()
                    .map(
                            this::retrieveAllHigherLevelOrgsInTheFutureWhenResourceOwnerAffiliationIsNotAlwaysTopLevelOrg)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    //TODO: does not do what the name says it does?
    private List<URI> retrieveAllHigherLevelOrgsInTheFutureWhenResourceOwnerAffiliationIsNotAlwaysTopLevelOrg(
            URI affiliation) {
        return List.of(affiliation);
    }

    @Override
    public ExpandedPerson expandPerson(User owner) {
        return attempt(() -> constructUri(owner))
                .map(uri -> uriRetriever.getRawContent(uri, CONTENT_TYPE))
                .map(response -> toExpandedPerson(response.orElse(null), owner))
                .orElse(failure -> getDefaultExpandedPerson(owner));
    }

    private ExpandedPerson getDefaultExpandedPerson(User owner) {
        logger.info(EXPAND_PERSON_CRISTIN_ERROR);
        return ExpandedPerson.defaultExpandedPerson(owner);
    }

    @Override
    public ExpandedMessage expandMessage(Message message) {
        return ExpandedMessage.createEntry(message, this);
    }

    private URI constructUri(User owner) {
        return UriWrapper.fromHost(new Environment().readEnv(API_HOST))
                .addChild(CRISTIN)
                .addChild(PERSON)
                .addChild(extractCristinId(owner))
                .getUri();
    }

    private static String extractCristinId(User owner) {
        return owner.toString().split(CRISTIN_ID_DELIMITER)[0];
    }
}
