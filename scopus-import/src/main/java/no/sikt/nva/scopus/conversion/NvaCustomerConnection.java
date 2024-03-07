package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public class NvaCustomerConnection {

    public static final String API_HOST = "API_HOST";
    public static final String CUSTOMER = "customer";
    public static final String CRISTIN_ID = "cristinId";
    public static final URI FETCH_CUSTOMER_ENDPOINT = UriWrapper.fromHost(new Environment().readEnv(API_HOST))
                                                          .addChild(CUSTOMER)
                                                          .addChild(CRISTIN_ID)
                                                          .getUri();
    public static final String PATH_DELIMITER = "/";
    public static final String CONTENT_TYPE = "application/json";
    public static final String FETCH_CUSTOMER_ERROR_MESSAGE = "Something went wrong fetching nva customer: ";
    private final AuthorizedBackendUriRetriever uriRetriever;

    public NvaCustomerConnection(AuthorizedBackendUriRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }

    public boolean isNvaCustomer(List<CristinOrganization> cristinOrganizations) {
        return cristinOrganizations
                   .stream()
                   .filter(NvaCustomerConnection::isPresentAndHasTopLevelOrg)
                   .anyMatch(this::isCustomer);
    }

    private static boolean isPresentAndHasTopLevelOrg(CristinOrganization cristinOrganization) {
        return nonNull(cristinOrganization) && nonNull(cristinOrganization.getTopLevelOrg());
    }

    private static boolean isHttpOk(Integer code) {
        return HttpURLConnection.HTTP_OK == code;
    }

    private static URI toFetchCustomerUri(URI topLevelOrganizationUri) {
        return URI.create(FETCH_CUSTOMER_ENDPOINT + PATH_DELIMITER + encodeUri(topLevelOrganizationUri));
    }

    private static String encodeUri(URI topLevelOrganization) {
        return URLEncoder.encode(topLevelOrganization.toString(), StandardCharsets.UTF_8);
    }

    private Boolean isCustomer(CristinOrganization cristinOrganization) {
        return attempt(cristinOrganization::getTopLevelOrg)
                   .map(CristinOrganization::id)
                   .map(NvaCustomerConnection::toFetchCustomerUri)
                   .map(this::fetchResponse)
                   .map(HttpResponse::statusCode)
                   .map(NvaCustomerConnection::isHttpOk)
                   .orElseThrow();
    }

    private HttpResponse<String> fetchResponse(URI uri) {
        var response = uriRetriever.fetchResponse(uri, CONTENT_TYPE);
        if (response.isPresent()) {
            return response.get();
        } else {
            throw new RuntimeException(FETCH_CUSTOMER_ERROR_MESSAGE + uri.toString());
        }
    }
}
