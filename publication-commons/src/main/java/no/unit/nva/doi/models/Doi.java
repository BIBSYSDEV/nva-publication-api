package no.unit.nva.doi.models;

import java.net.MalformedURLException;
import java.net.URI;
import nva.commons.utils.JacocoGenerated;

/**
 * Doi class for working with Dois.
 *
 * <p>Use {@link Doi#builder()} for constructing a new Doi instance.
 */
public abstract class Doi {

    public static final URI DOI_PROXY = URI.create("https://doi.org/");
    public static final String ERROR_PROXY_URI_MUST_BE_A_VALID_URL = "Proxy URI must be a valid URL";
    private static final String FORWARD_SLASH = "/";

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
        return getPrefix() + FORWARD_SLASH + getSuffix();
    }

    /**
     * Represents the DOI as an URI, this includes proxy, prefix and suffix.
     *
     * @return DOI as URI with proxy, prefix and suffix.
     */
    public URI toId() {
        String schemeWithAuthorityAndHost = extractSchemeWithAuthorityAndHost();
        return URI.create(schemeWithAuthorityAndHost + getPrefix() + FORWARD_SLASH + getSuffix());
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
    @JacocoGenerated
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
                    + FORWARD_SLASH;
            return schemeWithAuthorityAndHost;
        } catch (MalformedURLException e) {
            throw new IllegalStateException(ERROR_PROXY_URI_MUST_BE_A_VALID_URL, e);
        }
    }
}
