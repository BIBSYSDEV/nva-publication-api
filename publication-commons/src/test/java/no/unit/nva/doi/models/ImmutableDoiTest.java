package no.unit.nva.doi.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImmutableDoiTest {

    public static final URI DOI_PROXY = URI.create("https://doi.org/");
    public static final String EXAMPLE_SUFFIX = createRandomSuffix();
    public static final String EXAMPLE_RANDOM_VALUE = "2";
    public static final String FORWARD_SLASH = "/";
    public static final String REQUIRED_ATTRIBUTES_ARE_NOT_SET = "required attributes are not set";
    public static final String ERROR_STRICT_BUILDER = "Builder of Doi is strict, attribute is already set";
    private static final URI STAGE_DOI_PROXY = URI.create("https://handle.stage.datacite.org/");
    private static final String DEMO_PREFIX = "10.5072";
    public static final String EXAMPLE_PREFIX = DEMO_PREFIX;
    public static final String EXAMPLE_IDENTIFIER = EXAMPLE_PREFIX + FORWARD_SLASH + EXAMPLE_SUFFIX;
    private static final URI EXAMPLE_PROXY = STAGE_DOI_PROXY;
    private static final String EXAMPLE_PREFIX_2 = "10.16903";
    public static final String URI_VALID_EMAILTO_BUT_INVALID_URL = "emailto:nope@example.net";

    @Test
    void copyOfWithIdenticalAttributesReturnsSameInstance() {
        var immutableDoi = createDoi();

        assertThat(ImmutableDoi.copyOf(immutableDoi), is(sameInstance(immutableDoi)));
    }

    @Test
    void getProxyReturnsDefaultProxyWhenNotSpecified() {
        var doi = Doi.builder().withPrefix(EXAMPLE_PREFIX).withSuffix(EXAMPLE_SUFFIX).build();
        assertThat(doi.getProxy(), is(equalTo(DOI_PROXY)));
    }

    @Test
    void toStringReturnsPrefixAndSuffix() {
        assertThat(createDoi().toString(), is(equalTo(EXAMPLE_PREFIX + FORWARD_SLASH + EXAMPLE_SUFFIX)));
    }

    @Test
    void copyOfWithDifferentSubclassReturnsNewImmutableInstance() {
        AnotherPojoDoi immutableDoi = getAnotherPojoDoi();
        assertThat(ImmutableDoi.copyOf(immutableDoi), not(is(sameInstance(immutableDoi))));
    }

    @Test
    void withProxyWhenValueNotChangedReturnsSameInstance() {
        var doi = createDoi();
        assertThat(doi.withProxy(STAGE_DOI_PROXY), is(sameInstance(doi)));
    }

    @Test
    void withProxyWhenValueHasChangedReturnsNewImmutableInstance() {
        var doi = createDoi();
        assertThat(doi.withProxy(DOI_PROXY), not(is(sameInstance(doi))));
    }

    @Test
    void withPrefixWhenValueNotChangedReturnsSameInstance() {
        var doi = createDoi();
        assertThat(doi.withPrefix(EXAMPLE_PREFIX), is(sameInstance(doi)));
    }

    @Test
    void withPrefixWhenValueHasChangedReturnsNewImmutableInstance() {
        var doi = createDoi();
        assertThat(doi.withPrefix(EXAMPLE_PREFIX + EXAMPLE_RANDOM_VALUE), not(is(sameInstance(doi))));
    }

    @Test
    void withSuffixWhenValueNotChangedReturnsSameInstance() {
        var doi = createDoi();
        assertThat(doi.withSuffix(EXAMPLE_SUFFIX), is(sameInstance(doi)));
    }

    @Test
    void withSuffixWhenValueHasChangedReturnsNewImmutalbeInstance() {
        var doi = createDoi();
        assertThat(doi.withSuffix(EXAMPLE_SUFFIX + EXAMPLE_RANDOM_VALUE), not(is(sameInstance(doi))));
    }

    @Test
    void hashCodeReturnsSameHashCodeForTwoDifferentInstancesButIdenticalObject() {
        var doi = createDoi();
        var doi2 = createDoi();

        assertThat(doi.hashCode(), is(equalTo(doi2.hashCode())));
    }

    @Test
    void equalsReturnsTrueForTwoDifferentInstancesButIdenticalObjects() {
        var doi = createDoi();
        var doi2 = createDoi();

        assertThat(doi, is(equalTo(doi2)));
        assertThat(doi, is(equalTo(doi)));
    }

    @Test
    void toIdThenReturnsDoiIdWhichIsProxyPrefixAndSuffix() {
        String randomSuffix = createRandomSuffix();
        var doi = createDoi(randomSuffix);
        URI expectedUri = URI.create(EXAMPLE_PROXY + EXAMPLE_PREFIX + FORWARD_SLASH + randomSuffix);
        assertThat(doi.toId(), is(equalTo(expectedUri)));
    }

    @Test
    void toIdWhereBuilderWithProxyWithoutSuffixSlashReturnsCorrectIdURI() {
        var randomSuffix = createRandomSuffix();
        var doi = ImmutableDoi.builder()
            .withProxy(URI.create("http://example.net"))
            .withPrefix(DEMO_PREFIX)
            .withSuffix(randomSuffix)
            .build();
        assertThat(doi.toId(),
            is(equalTo(URI.create("http://example.net/" + DEMO_PREFIX + FORWARD_SLASH + randomSuffix))));
    }

    @Test
    void builderWithIdentifierSetsPrefixAndSuffixCorrect() {
        Doi doi = ImmutableDoi.builder().withIdentifier(EXAMPLE_IDENTIFIER).build();
        assertThat(doi.getPrefix(), is(equalTo(EXAMPLE_PREFIX)));
        assertThat(doi.getSuffix(), is(equalTo(EXAMPLE_SUFFIX)));
    }

    @Test
    void toIdWithAnotherSubClassWithInvalidProxyUriThenThrowsIllegalStateException() {
        var doi = getAnotherPojoDoi(URI.create(URI_VALID_EMAILTO_BUT_INVALID_URL));
        var actualException = assertThrows(IllegalStateException.class, doi::toId);
        assertThat(actualException.getMessage(), is(equalTo(Doi.ERROR_PROXY_URI_MUST_BE_A_VALID_URL)));
    }

    @Test
    void withProxyWithInvalidUrlAsUriThrowsIllegalStateException() {
        var doi = getAnotherPojoDoi(URI.create(URI_VALID_EMAILTO_BUT_INVALID_URL));

        var actualException = assertThrows(IllegalArgumentException.class, () -> ImmutableDoi.copyOf(doi));
        assertThat(actualException.getMessage(), is(equalTo(Doi.ERROR_PROXY_URI_MUST_BE_A_VALID_URL)));
    }

    @Test
    void builderWithProxyWithInvalidUrlAsUriThrowsIllegalArgumentException() {
        var actualException = assertThrows(IllegalArgumentException.class, () ->
            ImmutableDoi.builder().withProxy(URI.create(URI_VALID_EMAILTO_BUT_INVALID_URL)));
        assertThat(actualException.getMessage(), is(equalTo(Doi.ERROR_PROXY_URI_MUST_BE_A_VALID_URL)));
    }

    @Test
    void builderOnlyPrefixThrowsException() {
        var actual = assertThrows(IllegalStateException.class,
            () -> ImmutableDoi.builder().withPrefix(EXAMPLE_PREFIX).build());
        assertThat(actual.getMessage(), containsString(REQUIRED_ATTRIBUTES_ARE_NOT_SET));
    }

    @Test
    void builderOnlySuffixThrowsException() {
        var actual = assertThrows(IllegalStateException.class,
            () -> ImmutableDoi.builder().withSuffix(createRandomSuffix()).build());
        assertThat(actual.getMessage(), containsString(REQUIRED_ATTRIBUTES_ARE_NOT_SET));
    }

    @Test
    void builderWithNullIdentifierThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ImmutableDoi.builder().withIdentifier(null).build());
    }

    @Test
    void builderWithInvalidDoiThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> ImmutableDoi.builder().withIdentifier(EXAMPLE_PREFIX).build());
    }

    @Test
    void toIdentifierReturnsPrefixForwardSlashAndSuffix() {
        String randomSuffix = createRandomSuffix();
        Doi doi = createDoi(randomSuffix);
        assertThat(doi.toIdentifier(), is(equalTo(EXAMPLE_PREFIX + "/" + randomSuffix)));
    }

    @Test
    void builderWithProxyCalledTwiceThrowsException() {
        var actualException = assertThrows(IllegalStateException.class, () ->
            Doi.builder()
                .withProxy(STAGE_DOI_PROXY)
                .withProxy(DOI_PROXY));
        assertThat(actualException.getMessage(), containsString(ERROR_STRICT_BUILDER));
    }

    @Test
    void builderWithPrefixCalledTwiceThrowsException() {
        var actualException = assertThrows(IllegalStateException.class, () ->
            Doi.builder()
                .withPrefix(EXAMPLE_PREFIX)
                .withPrefix(EXAMPLE_PREFIX_2));
        assertThat(actualException.getMessage(), containsString(ERROR_STRICT_BUILDER));
    }

    @Test
    void builderWithSuffixCalledTwiceThrowsException() {
        var actualException = assertThrows(IllegalStateException.class, () ->
            Doi.builder()
                .withSuffix(createRandomSuffix())
                .withSuffix(createRandomSuffix()));
        assertThat(actualException.getMessage(), containsString(ERROR_STRICT_BUILDER));
    }

    private static String createRandomSuffix() {
        return UUID.randomUUID().toString();
    }

    private AnotherPojoDoi getAnotherPojoDoi() {
        return new AnotherPojoDoi(EXAMPLE_PROXY, EXAMPLE_PREFIX, EXAMPLE_SUFFIX);
    }

    private AnotherPojoDoi getAnotherPojoDoi(URI proxy) {
        return new AnotherPojoDoi(proxy, EXAMPLE_PREFIX, EXAMPLE_SUFFIX);
    }

    private ImmutableDoi createDoi(String suffix) {
        return Doi.builder()
            .withProxy(STAGE_DOI_PROXY)
            .withPrefix(EXAMPLE_PREFIX)
            .withSuffix(suffix)
            .build();
    }

    private ImmutableDoi createDoi() {
        return Doi.builder()
            .withProxy(STAGE_DOI_PROXY)
            .withPrefix(EXAMPLE_PREFIX)
            .withSuffix(EXAMPLE_SUFFIX)
            .build();
    }

    private static class AnotherPojoDoi extends Doi {

        private final URI proxy;
        private final String prefix;
        private final String suffix;

        public AnotherPojoDoi(URI proxy, String prefix, String suffix) {
            this.proxy = proxy;
            this.prefix = prefix;
            this.suffix = prefix;
        }

        @Override
        public URI getProxy() {
            return proxy;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getSuffix() {
            return suffix;
        }
    }
}