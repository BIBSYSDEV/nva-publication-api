package no.unit.nva.doi.models;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.net.URISyntaxException;

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
    protected static final char PATH_SEPARATOR = '/';
    protected static final String PATH_SEPARATOR_STRING = String.valueOf(PATH_SEPARATOR);
    public static final URI DOI_PROXY = URI.create(HTTPS.concat(DOI_ORG).concat(PATH_SEPARATOR_STRING));

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
    public URI toUri() {
        try {
            URI uri = createDoi();
            uri.toURL(); // validate
            return uri;
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalStateException(ERROR_PROXY_URI_MUST_BE_A_VALID_URL, e);
        }
    }

    private URI createDoi() throws URISyntaxException {
        return new URI(getProxy().getScheme(),
            getProxy().getUserInfo(),
            getProxy().getHost(),
            getProxy().getPort(),
            PATH_SEPARATOR + toIdentifier(),
            null,
            null);
    }
}
