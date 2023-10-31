package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedOrganization;
import no.unit.nva.expansion.model.ExpandedPerson;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedTicket;
import no.unit.nva.expansion.model.cristin.CristinPerson;
import no.unit.nva.expansion.utils.NviCalculator;
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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;

import static java.util.Objects.isNull;
import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;

public class ResourceExpansionServiceImpl implements ResourceExpansionService {

    public static final Logger logger = LoggerFactory.getLogger(ResourceExpansionServiceImpl.class);
    public static final String CONTENT_TYPE = "application/json";
    public static final String UNSUPPORTED_TYPE = "Expansion is not supported for type:";
    public static final String CRISTIN = "cristin";
    public static final String PERSON = "person";
    public static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String CONTEXT_URI = new UriWrapper(UriWrapper.HTTPS, API_HOST)
                                                 .addChild("publication", "context")
                                                 .toString();
    public static final String CRISTIN_ID_DELIMITER = "@";
    private static final String EXPAND_PERSON_DEFAULT_INFO = "Could not retrieve Cristin Person. Creating default "
                                                             + "expanded person for owner";
    public static final String FIRST_NAME_CRISTIN_TYPE = "FirstName";
    public static final String PREFERRED_FIRST_NAME_CRISTIN_TYPE = "PreferredFirstName";
    public static final String LAST_NAME_CRISTIN_TYPE = "LastName";
    public static final String PREFERRED_LAST_NAME_CRISTIN_TYPE = "PreferredLastName";
    private static final String PART_OF_PROPERTY = "https://nva.sikt.no/ontology/publication#partOf";
    public static final String CONTEXT_NODE_SELECTOR = "/@context";
    public static final String CONTEXT_FIELD_PROPERTY_NAME = "@context";
    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final UriRetriever personRetriever;
    private final UriRetriever organizationRetriever;

    public ResourceExpansionServiceImpl(ResourceService resourceService,
                                        TicketService ticketService) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.personRetriever = new UriRetriever();
        this.organizationRetriever = new UriRetriever();
    }

    public ResourceExpansionServiceImpl(ResourceService resourceService,
                                        TicketService ticketService,
                                        UriRetriever uriRetriever) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.personRetriever = uriRetriever;
        this.organizationRetriever = uriRetriever;
    }

    public ResourceExpansionServiceImpl(ResourceService resourceService,
                                        TicketService ticketService,
                                        UriRetriever personRetriever,
                                        UriRetriever organizationRetriever) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.personRetriever = personRetriever;
        this.organizationRetriever = organizationRetriever;
    }

    private ExpandedPerson toExpandedPerson(String response, User owner) throws JsonProcessingException {
        var cristinPerson = JsonUtils.dtoObjectMapper.readValue(response, CristinPerson.class);
        var nameMap = cristinPerson.getNameTypeMap();
        return new ExpandedPerson.Builder()
                .withFirstName(nameMap.get(FIRST_NAME_CRISTIN_TYPE))
                .withPreferredFirstName(nameMap.get(PREFERRED_FIRST_NAME_CRISTIN_TYPE))
                .withLastName(nameMap.get(LAST_NAME_CRISTIN_TYPE))
                .withPreferredLastName(nameMap.get(PREFERRED_LAST_NAME_CRISTIN_TYPE))
                .withUser(owner)
                .build();

    }

    @Override
    public ExpandedDataEntry expandEntry(Entity dataEntry) throws JsonProcessingException, NotFoundException {
        if (dataEntry instanceof Resource resource) {
            logger.info("Expanding Resource: {}", resource.getIdentifier());
            var expandedResource = ExpandedResource.fromPublication(personRetriever,
                                                                    resource.toPublication(resourceService));
            var resourceWithNviType = NviCalculator.calculateNviType(expandedResource);
            var resourceWithContextUri = replaceInlineContextWithUriContext(resourceWithNviType);
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

    private ObjectNode replaceInlineContextWithUriContext(String resourceWithNviValues) {
        var objectNode =  attempt(() -> (ObjectNode) objectMapper.readTree(resourceWithNviValues)).orElseThrow();
        if (!objectNode.at(CONTEXT_NODE_SELECTOR).isMissingNode()) {
            objectNode.remove(CONTEXT_FIELD_PROPERTY_NAME);
            objectNode.put(CONTEXT_FIELD_PROPERTY_NAME, CONTEXT_URI);
        }
        return objectNode;
    }

    @Override
    public ExpandedOrganization getOrganization(Entity dataEntry) throws NotFoundException {
        if (dataEntry instanceof TicketEntry ticketEntry) {
            var resourceIdentifier = ticketEntry.getResourceIdentifier();
            var resource = resourceService.getResourceByIdentifier(resourceIdentifier);
            var organizationIds = resource.getResourceOwner().getOwnerAffiliation();

            var partOf = attempt(() -> this.organizationRetriever.getRawContent(organizationIds, CONTENT_TYPE)).map(
                    Optional::orElseThrow)
                       .map(str -> createModel(stringToStream(str)))
                       .map(model -> model.listObjectsOfProperty(model.createProperty(PART_OF_PROPERTY)))
                       .map(nodeIterator -> nodeIterator.toList()
                                                .stream().map(RDFNode::toString).map(URI::create).toList())
                       .orElseThrow();


            return new ExpandedOrganization(organizationIds, partOf);

        }
        return null;
    }

    @Override
    public ExpandedPerson expandPerson(User owner) {
        return attempt(() -> constructUri(owner))
                .map(uri -> personRetriever.getRawContent(uri, CONTENT_TYPE))
                .map(response -> toExpandedPerson(response.orElse(null), owner))
                .orElse(failure -> getDefaultExpandedPerson(owner));
    }

    private ExpandedPerson getDefaultExpandedPerson(User owner) {
        logger.info(EXPAND_PERSON_DEFAULT_INFO);
        return ExpandedPerson.defaultExpandedPerson(owner);
    }

    @Override
    public ExpandedMessage expandMessage(Message message) {
        return ExpandedMessage.createEntry(message, this);
    }

    private URI constructUri(User owner) {
        return UriWrapper.fromHost(API_HOST)
                .addChild(CRISTIN)
                .addChild(PERSON)
                .addChild(extractCristinId(owner))
                .getUri();
    }

    private static String extractCristinId(User owner) {
        return owner.toString().split(CRISTIN_ID_DELIMITER)[0];
    }

    private Model createModel(InputStream inputStream) {
        var model = ModelFactory.createDefaultModel();

        if (isNull(inputStream)) {
            return model;
        }
        try {
            RDFDataMgr.read(model, inputStream, Lang.JSONLD);
        } catch (RiotException e) {
            logger.warn("Invalid JSON LD input encountered: ", e);
        }
        return model;
    }
}
