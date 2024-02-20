package no.unit.nva.publication.permission.strategy.grant;

import static java.util.Objects.isNull;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.UserInstance;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CuratorPermissionStrategy extends GrantPermissionStrategy {

    public static final Logger logger = LoggerFactory.getLogger(CuratorPermissionStrategy.class);
    private static final String PART_OF_PROPERTY = "https://nva.sikt.no/ontology/publication#partOf";
    public static final String APPLICATION_JSON = "application/json";

    public CuratorPermissionStrategy(Publication publication, UserInstance userInstance, UriRetriever uriRetriever) {
        super(publication, userInstance, uriRetriever);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        if (!canManageStandardResources() || !userRelatesToPublication()) {
            return false;
        }

        return switch (permission) {
            case UPDATE -> true;
            case TICKET_PUBLISH -> isPublishableState() && canManagePublishingRequests();
            case UNPUBLISH -> isPublished();
            default -> false;
        };
    }

    private boolean isPublishableState() {
        return isUnpublished() || isDraft();
    }

    private boolean canManageStandardResources() {
        return hasAccessRight(MANAGE_RESOURCES_STANDARD);
    }

    private boolean canManagePublishingRequests() {
        return hasAccessRight(MANAGE_PUBLISHING_REQUESTS);
    }

    private boolean userRelatesToPublication() {
        return userIsFromSameInstitutionAsPublication() || userSharesTopLevelOrgWithAtLeastOneContributor();
    }

    private boolean userIsFromSameInstitutionAsPublication() {
        return userInstance.getCustomerId() != null && publication.getPublisher() != null &&
               userInstance.getCustomerId().equals(publication.getPublisher().getId());
    }

    private boolean userSharesTopLevelOrgWithAtLeastOneContributor() {
        var contributorTopLevelOrgs = getContributorTopLevelOrgs();
        var userTopLevelOrg = getTopLevelOrgUri(userInstance.getCustomerId());


        logger.info("found topLevels {} for user with {} ", contributorTopLevelOrgs, userTopLevelOrg);

        return contributorTopLevelOrgs.stream().anyMatch(org -> org.equals(userTopLevelOrg));
    }

    private Set<URI> getContributorTopLevelOrgs() {
        return publication.getEntityDescription().getContributors()
                   .stream()
                   .filter(contributor -> contributor.getIdentity() != null)
                   .flatMap(contributor ->
                                contributor.getAffiliations().stream()
                                    .filter(Organization.class::isInstance)
                                    .map(Organization.class::cast)
                                    .map(Organization::getId))
                   .collect(Collectors.toSet())
                   .stream().map(this::getTopLevelOrgUri)
                   .collect(Collectors.toSet());
    }

    private URI getTopLevelOrgUri(URI id) {
        var data = attempt(() -> uriRetriever.getRawContent(id, APPLICATION_JSON)).orElseThrow();

        if (data.isEmpty()) {
            return id;
        }

        var model = createModel(stringToStream(data.get()));
        var query = QueryFactory.create("prefix : <https://nva.sikt.no/ontology/publication#> "
                                        + "SELECT ?organization WHERE {"
                                        + "?organization a :Organization ."
                                        + "OPTIONAL {?somethingelse :hasPart ?organization}"
                                        + "OPTIONAL {?organization :partOf ?somethingelse}"
                                        + "FILTER (!BOUND(?somethingelse))"
                                        + "}");

        try( var qe = QueryExecutionFactory.create(query, model)) {
            var result = qe.execSelect();
            if (result.hasNext()) {
                return URI.create(result.next().get("organization").asResource().getURI());
            }
        }
        return id;
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
