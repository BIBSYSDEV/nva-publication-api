package no.sikt.nva.brage.migration.record;

import static java.util.Objects.nonNull;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.funding.FundingBuilder;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public record Project(String identifier, String name, FundingSource fundingSource) {

    public static final String CRISTIN = "cristin";
    public static final String FUNDING_SOURCES = "funding-sources";
    public static final String VERIFIED_FUNDING = "verified-funding";
    public static final String NFR_PATH_PARAM = "nfr";
    private static final String API_HOST = new Environment().readEnv("API_HOST");
    private static final String NFR = "NFR";

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Project project = (Project) o;
        return Objects.equals(identifier(), project.identifier()) && Objects.equals(name(), project.name());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(identifier(), name());
    }

    public Funding toFunding() {
        if (nonNull(fundingSource)) {
            return constructFundingWithSource();
        } else {
            return constructUnconfirmedFundingWithoutSource();
        }
    }

    private Funding constructFundingWithSource() {
        var source = constructSource();
        if (NFR.equals(fundingSource.identifier())) {
            return constructConfirmedNfrFunding(source);
        } else {
            return constructUnconfirmedFunding(source);
        }
    }

    private Funding constructUnconfirmedFunding(URI source) {
        return new FundingBuilder().withSource(source).withIdentifier(identifier).build();
    }

    private Funding constructConfirmedNfrFunding(URI source) {
        var id = constructNfrFundingsWithId(identifier);
        return new FundingBuilder().withSource(source).withId(id).withIdentifier(identifier).build();
    }

    private Funding constructUnconfirmedFundingWithoutSource() {
        return new FundingBuilder().withIdentifier(identifier).withLabels(Map.of("nb", name)).build();
    }

    private URI constructSource() {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(CRISTIN)
                   .addChild(FUNDING_SOURCES)
                   .addChild(URLEncoder.encode(fundingSource.identifier(), StandardCharsets.UTF_8))
                   .getUri();
    }

    private URI constructNfrFundingsWithId(String identifier) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(VERIFIED_FUNDING)
                   .addChild(NFR_PATH_PARAM)
                   .addChild(identifier)
                   .getUri();
    }
}
