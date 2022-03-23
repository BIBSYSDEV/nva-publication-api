package no.unit.nva.expansion;

import static java.util.Objects.nonNull;
import static no.unit.nva.expansion.ExpansionConstants.API_HOST;
import static no.unit.nva.expansion.ExpansionConstants.API_SCHEME;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class OrganizationResponseObject {

    public static final String MOST_COMMON_INSTITUTION_IDENTIFIER_TEMPLATE = "%s.0.0.0";
    public static final String INSTITUTIONS_PATH_IN_BARE_PROXY = "institutions";
    public static final String PART_OF = "partOf";
    private static final String ID = "id";
    private static final URI ORGANIZATION_PROXY_URI = constructInstitutionProxyHostUri();
    @JsonProperty(ID)
    private final URI id;
    @JsonProperty(PART_OF)
    private final List<OrganizationResponseObject> partOf;

    @JsonCreator
    public OrganizationResponseObject(@JsonProperty(ID) URI id,
                                      @JsonProperty(PART_OF) List<OrganizationResponseObject> partOf) {
        this.id = id;
        this.partOf = partOf;
    }

    public static Set<URI> retrieveAllRelatedOrganizations(HttpClient httpClient, URI organizationUri) {
        var newUri = transformUriFromPersonProxyToCristinProxy(organizationUri);
        var organization = new OrganizationResponseObject(newUri, Collections.emptyList());
        return organization.retrieveAllRelatedOrganizations(httpClient);
    }

    @JacocoGenerated
    public URI getId() {
        return id;
    }

    @JacocoGenerated
    public List<OrganizationResponseObject> getPartOf() {
        return nonNull(partOf) ? partOf : Collections.emptyList();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(id, partOf);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrganizationResponseObject)) {
            return false;
        }
        OrganizationResponseObject that = (OrganizationResponseObject) o;
        return Objects.equals(id, that.id) && Objects.equals(partOf, that.partOf);
    }

    private static URI constructInstitutionProxyHostUri() {
        return new UriWrapper(API_SCHEME, API_HOST)
            .addChild("cristin")
            .addChild("organization")
            .getUri();
    }

    private static URI transformUriFromPersonProxyToCristinProxy(URI organizationUri) {
        if (isInstitutionUri(organizationUri)) {
            return transformInstitutionUriToOrgUri(organizationUri);
        } else {
            return transformOrganizationUri(organizationUri);
        }
    }

    private static URI transformOrganizationUri(URI organizationUri) {
        var institutionIdentifier = UriWrapper.fromUri(organizationUri).getLastPathElement();
        return UriWrapper.fromUri(ORGANIZATION_PROXY_URI)
            .addChild(institutionIdentifier)
            .getUri();
    }

    private static URI transformInstitutionUriToOrgUri(URI organizationUri) {
        var institutionNumber = UriWrapper.fromUri(organizationUri).getLastPathElement();
        var institutionIdentifier = String.format(MOST_COMMON_INSTITUTION_IDENTIFIER_TEMPLATE, institutionNumber);
        return UriWrapper.fromUri(ORGANIZATION_PROXY_URI)
            .addChild(institutionIdentifier)
            .getUri();
    }

    private static boolean isInstitutionUri(URI organizationUri) {
        return organizationUri.getPath().contains(INSTITUTIONS_PATH_IN_BARE_PROXY);
    }

    private Set<URI> retrieveAllRelatedOrganizations(HttpClient httpClient) {
        try {
            var resultSet = new Stack<URI>();
            var notYetVisited = new Stack<URI>();
            notYetVisited.add(id);
            while (thereAreMoreOrganizationsToVisit(notYetVisited)) {
                var currentOrganization = pickNextOrganization(notYetVisited);
                addImmediateAncestorsToOrganizationsForVisiting(httpClient, notYetVisited, currentOrganization);
                resultSet.push(currentOrganization);
            }
            return new HashSet<>(resultSet);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void addImmediateAncestorsToOrganizationsForVisiting(HttpClient httpClient,
                                                                 Stack<URI> notYetVisited,
                                                                 URI currentUri)
        throws IOException, InterruptedException {
        HttpResponse<String> response = resolveUri(httpClient, currentUri);
        if (responseWasSuccessful(response)) {
            var currentOrg = parseResponse(response);
            if (currentOrganizationIsNotAtTopLevel(currentOrg)) {
                addAncestorsToOrganizationsForVisiting(currentOrg, notYetVisited);
            }
        }
    }

    private URI pickNextOrganization(Stack<URI> notYetVisited) {
        return notYetVisited.pop();
    }

    private boolean thereAreMoreOrganizationsToVisit(Stack<URI> notYetVisited) {
        return !notYetVisited.isEmpty();
    }

    private void addAncestorsToOrganizationsForVisiting(OrganizationResponseObject currentOrg,
                                                        Stack<URI> notYetVisited) {
        currentOrg.getPartOf().forEach(ancestor -> notYetVisited.add(ancestor.getId()));
    }

    private boolean currentOrganizationIsNotAtTopLevel(OrganizationResponseObject currentOrg) {
        return !currentOrg.getPartOf().isEmpty();
    }

    private boolean responseWasSuccessful(HttpResponse<String> response) {
        return HttpURLConnection.HTTP_OK == response.statusCode();
    }

    private OrganizationResponseObject parseResponse(HttpResponse<String> response) throws JsonProcessingException {
        return ExpansionConfig.objectMapper.readValue(response.body(), OrganizationResponseObject.class);
    }

    private HttpResponse<String> resolveUri(HttpClient httpClient, URI currentUri) throws IOException,
                                                                                          InterruptedException {
        var httpRequest = HttpRequest.newBuilder(currentUri).GET().build();
        return httpClient.send(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
