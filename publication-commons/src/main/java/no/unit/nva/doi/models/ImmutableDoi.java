package no.unit.nva.doi.models;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable implementation of {@link Doi}.
 *
 * <p>Use the builder to create immutable instances: {@code ImmutableDoi.builder()}.
 */
@SuppressWarnings("PMD.UnnecessaryModifier") // methods with final.
public final class ImmutableDoi extends Doi {

    public static final String MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_PROXY = "proxy";
    public static final String MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_PREFIX = "prefix";
    public static final String MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_SUFFIX = "suffix";
    public static final String MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_IDENTIFIER = "identifier";
    public static final String HANDLE_DOI_PREFIX = "10.";
    public static final String CANNOT_BUILD_DOI_PREFIX_MUST_START_WITH = "Cannot build Doi, prefix must start with ";
    public static final String CANNOT_BUILD_DOI_PROXY_IS_NOT_A_VALID_PROXY =
        "Cannot build Doi, proxy is not a valid proxy.";
    private final URI proxy;
    private final String prefix;
    private final String suffix;

    @SuppressWarnings("PMD.CallSuperInConstructor")
    private ImmutableDoi(ImmutableDoi.Builder builder) {
        this.prefix = builder.prefix;
        this.suffix = builder.suffix;
        this.proxy = builder.proxyIsSet()
            ? builder.proxy
            : Objects.requireNonNull(super.getProxy(), MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_PROXY);
    }

    @SuppressWarnings("PMD.CallSuperInConstructor")
    private ImmutableDoi(URI proxy, String prefix, String suffix) {
        this.proxy = proxy;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    /**
     * Creates an immutable copy of a {@link Doi} value. Uses accessors to get values to initialize the new immutable
     * instance. If an instance is already immutable, it is returned as is.
     *
     * @param instance The instance to copy
     * @return A copied immutable Doi instance
     */
    public static ImmutableDoi copyOf(Doi instance) {
        if (instance instanceof ImmutableDoi) {
            return (ImmutableDoi) instance;
        }
        return ImmutableDoi.builder()
            .withProxy(instance.getProxy())
            .withPrefix(instance.getPrefix())
            .withSuffix(instance.getSuffix())
            .build();
    }

    /**
     * Creates a builder for {@link ImmutableDoi ImmutableDoi}.
     * <pre>
     * ImmutableDoi.builder()
     *    .proxy(String) // optional {@link Doi#getProxy() proxy}
     *    .prefix(String) // required {@link Doi#getPrefix()} () prefix}
     *    .suffix(String) // required {@link Doi#getSuffix()} () suffix}
     *    .build();
     * </pre>
     *
     * @return A new ImmutableDoi builder
     */
    public static ImmutableDoi.Builder builder() {
        return new ImmutableDoi.Builder();
    }

    /**
     * Retrieve the value of the ${@code proxy} attribute.
     *
     * @return The value of the {@code proxy} attribute
     */
    @Override
    public URI getProxy() {
        return proxy;
    }

    /**
     * Retrieve the value of the ${@code prefix} attribute.
     *
     * @return The value of the {@code prefix} attribute
     */
    @Override
    public String getPrefix() {
        return prefix;
    }

    /**
     * Retrieve the value of the ${@code suffix} attribute.
     *
     * @return The value of the {@code suffix} attribute
     */
    @Override
    public String getSuffix() {
        return suffix;
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Doi#getProxy() proxy} attribute. An equals
     * check used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for proxy
     * @return A modified copy of the {@code this} object
     */
    public final ImmutableDoi withProxy(URI value) {
        URI newValue = Objects.requireNonNull(value, MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_PROXY);
        if (this.proxy.equals(newValue)) {
            return this;
        }
        validateProxyUri(newValue);
        return new ImmutableDoi(newValue, this.prefix, this.suffix);
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Doi#getPrefix()} () prefix} attribute. An
     * equals check used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for prefix
     * @return A modified copy of the {@code this} object
     */
    public final ImmutableDoi withPrefix(String value) {
        String newValue = Objects.requireNonNull(value, MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_PREFIX);
        if (this.prefix.equals(newValue)) {
            return this;
        }
        return new ImmutableDoi(this.proxy, newValue, this.suffix);
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Doi#getSuffix()} () suffix} attribute. An
     * equals check used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for suffix
     * @return A modified copy of the {@code this} object
     */
    public final ImmutableDoi withSuffix(String value) {
        String newValue = Objects.requireNonNull(value, MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_SUFFIX);
        if (this.suffix.equals(newValue)) {
            return this;
        }
        return new ImmutableDoi(this.proxy, this.prefix, newValue);
    }

    /**
     * This instance is equal to all instances of {@code ImmutableDoi} that have equal attribute values.
     *
     * @return {@code true} if {@code this} is equal to {@code another} instance
     */
    @Override
    public boolean equals(Object another) {
        if (this == another) {
            return true;
        }
        return another instanceof ImmutableDoi
            && equalTo((ImmutableDoi) another);
    }

    /**
     * Computes a hash code from attributes: {@code proxy}, {@code prefix}, {@code suffix}.
     *
     * @return hashCode value
     */
    @Override
    public int hashCode() {
        return Objects.hash(proxy, prefix, suffix);
    }

    /**
     * Prints the immutable value {@code Doi} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
        return getPrefix() + PATH_SEPARATOR + getSuffix();
    }

    private static void validateProxyUri(URI proxy) {
        try {
            proxy.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(ERROR_PROXY_URI_MUST_BE_A_VALID_URL, e);
        }
    }

    private boolean equalTo(ImmutableDoi another) {
        return proxy.equals(another.proxy)
            && prefix.equals(another.prefix)
            && suffix.equals(another.suffix);
    }

    /**
     * Builds instances of type {@link ImmutableDoi ImmutableDoi}. Initialize attributes and then invoke the {@link
     * #build()} method to create an immutable instance.
     *
     * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
     * but instead used immediately to create instances.</em>
     */
    public static final class Builder {

        private static final long INIT_BIT_PREFIX = 0x1L;
        private static final long INIT_BIT_SUFFIX = 0x2L;
        private static final long OPT_BIT_PROXY = 0x1L;
        private long initBits = 0x3L;
        private long optBits;

        private URI proxy;
        private String prefix;
        private String suffix;

        private Builder() {
        }

        /**
         * Initializes the value for the {@link Doi#getProxy() proxy} attribute.
         *
         * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link
         * Doi#getProxy() proxy}.</em>
         *
         * @param proxy The value for proxy
         * @return {@code this} builder for use in a chained invocation
         */

        public final Builder withProxy(URI proxy) {
            checkNotIsSet(proxyIsSet(), "proxy");
            validateProxyUri(proxy);
            this.proxy = Objects.requireNonNull(proxy, MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_PROXY);
            optBits |= OPT_BIT_PROXY;
            return this;
        }

        /**
         * Initializes the value for the {@link Doi#getPrefix()} () prefix} attribute.
         *
         * @param prefix The value for prefix
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder withPrefix(String prefix) {
            checkNotIsSet(prefixIsSet(), "prefix");
            this.prefix = Objects.requireNonNull(prefix, MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_PREFIX);
            initBits &= ~INIT_BIT_PREFIX;
            return this;
        }

        /**
         * Initializes the value for the {@link Doi#getSuffix()}  suffix} attribute.
         *
         * @param suffix The value for suffix
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder withSuffix(String suffix) {
            checkNotIsSet(suffixIsSet(), "suffix");
            this.suffix = Objects.requireNonNull(suffix, MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_SUFFIX);
            initBits &= ~INIT_BIT_SUFFIX;
            return this;
        }

        /**
         * Initializes the value for the {@link Doi#getPrefix()} and {@link Doi#getSuffix()} attributes.
         *
         * @param identifier The value (doi identifier: prefix/suffix) that can be parsed into prefix and suffix.
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder withIdentifier(String identifier) {
            Objects.requireNonNull(identifier, MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_IDENTIFIER);
            int indexOfDivider = identifier.indexOf(PATH_SEPARATOR);
            if (indexOfDivider == -1) {
                throw new IllegalArgumentException("Invalid DOI identifier");
            }
            withPrefix(identifier.substring(0, indexOfDivider));
            withSuffix(identifier.substring(++indexOfDivider));
            return this;
        }

        /**
         * Builds a new {@link ImmutableDoi ImmutableDoi}.
         *
         * @return An immutable instance of Doi
         * @throws java.lang.IllegalStateException if any required attributes are missing
         */
        public ImmutableDoi build() {
            checkRequiredAttributes();
            validateProxy();
            validatePrefix();
            return new ImmutableDoi(this);
        }

        private void validatePrefix() {
            if (!prefix.startsWith(HANDLE_DOI_PREFIX) || prefix.length() <= HANDLE_DOI_PREFIX.length()) {
                throw new IllegalStateException(CANNOT_BUILD_DOI_PREFIX_MUST_START_WITH.concat(HANDLE_DOI_PREFIX)
                    .concat(" and contain some repository id"));
            }
        }

        private void validateProxy() {
            if (!VALID_PROXIES.contains(proxy.getHost().toLowerCase())) {
                throw new IllegalStateException(CANNOT_BUILD_DOI_PROXY_IS_NOT_A_VALID_PROXY);
            }
        }

        private static void checkNotIsSet(boolean isSet, String name) {
            if (isSet) {
                throw new IllegalStateException("Builder of Doi is strict, attribute is already set: ".concat(name));
            }
        }

        private boolean proxyIsSet() {
            return (optBits & OPT_BIT_PROXY) != 0;
        }

        private boolean prefixIsSet() {
            return (initBits & INIT_BIT_PREFIX) == 0;
        }

        private boolean suffixIsSet() {
            return (initBits & INIT_BIT_SUFFIX) == 0;
        }

        private void checkRequiredAttributes() {
            if (initBits != 0) {
                throw new IllegalStateException(formatRequiredAttributesMessage());
            }
        }

        private String formatRequiredAttributesMessage() {
            List<String> attributes = new ArrayList<>();
            if (!prefixIsSet()) {
                attributes.add("prefix");
            }
            if (!suffixIsSet()) {
                attributes.add("suffix");
            }
            return "Cannot build Doi, some of required attributes are not set " + attributes;
        }
    }
}
