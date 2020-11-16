package no.unit.nva.doi.models;

import java.net.URI;

/**
 * Doi class for working with Dois.
 *
 * <p>Use {@link Doi#builder()} for constructing a new Doi instance.
 */
public abstract class Doi {

    public static final String DOI_PROXY = "https://doi.org/";
    private static final String FORWARD_SLASH = "/";

    public static ImmutableDoi.Builder builder() {
        return ImmutableDoi.builder();
    }

    public abstract String getPrefix();

    public abstract String getSuffix();

    public String getProxy() {
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
        return URI.create(getProxy() + getPrefix() + FORWARD_SLASH + getSuffix());
    }
}
