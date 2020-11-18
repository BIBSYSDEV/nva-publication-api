package no.unit.nva.doi.models;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;

/**
 * Doi class for working with Dois.
 *
 * <p>Use {@link Doi#builder()} for constructing a new Doi instance.
 */
public abstract class Doi {

    public static final String ERROR_PROXY_URI_MUST_BE_A_VALID_URL = "Proxy URI must be a valid URL";
    public static final String DOI_ORG = "doi.org";
    public static final String HTTPS = "https://";
    public static final String HANDLE_STAGE_DATACITE_ORG = "handle.stage.datacite.org";
    public static final String DX_DOI_ORG = "dx.doi.org";
    public static final List<String> VALID_PROXIES = List.of(DOI_ORG, DX_DOI_ORG, HANDLE_STAGE_DATACITE_ORG);
    protected static final String PATH_SEPARATOR = "/";
    public static final URI DOI_PROXY = URI.create(HTTPS.concat(DOI_ORG).concat(PATH_SEPARATOR));

    public static ImmutableDoi.Builder builder() {
        return ImmutableDoi.builder();
    }

    public abstract String getPrefix();

    public abstract String getSuffix();

    public URI getProxy() {
        // default value
        return DOI_PROXY;
    }

    /**
     * Represents the DOI with ${prefix}/${suffix}.
     *
     * @return prefix/suffix (DOI identifier)
     */
    public String toIdentifier() {
        return getPrefix() + PATH_SEPARATOR + getSuffix();
    }

    /**
     * Represents the DOI as an URI, this includes proxy, prefix and suffix.
     *
     * @return DOI as URI with proxy, prefix and suffix.
     */
    public URI toId() {
        String schemeWithAuthorityAndHost = extractSchemeWithAuthorityAndHost();
        return URI.create(schemeWithAuthorityAndHost + getPrefix() + PATH_SEPARATOR + getSuffix());
    }

    /**
     * Extracts scheme, authority and host with last character being forward slash after host from {@code #getProxy}().
     *
     * <p>It converts URI to URL to have helper methods to extract scheme, authority and host. Copied from {@code
     * java.net.URL#toExternalForm}
     *
     * @return scheme with authority and host and forward slash at the end.
     * @see java.net.URL#toExternalForm
     */
    @SuppressWarnings("PMD.UseStringBufferForStringAppends") // since we copy JDK method!
    private String extractSchemeWithAuthorityAndHost() {
        try {
            var proxyUrl = getProxy().toURL();
            String schemeWithAuthorityAndHost;
            schemeWithAuthorityAndHost =
                proxyUrl.getProtocol()
                    + ':'
                    + (
                    (schemeWithAuthorityAndHost = proxyUrl.getAuthority()) != null
                        && !schemeWithAuthorityAndHost.isEmpty()
                        ? "//" + schemeWithAuthorityAndHost : "")
                    + PATH_SEPARATOR;
            return schemeWithAuthorityAndHost;
        } catch (MalformedURLException e) {
            throw new IllegalStateException(ERROR_PROXY_URI_MUST_BE_A_VALID_URL, e);
        }
    }
}
