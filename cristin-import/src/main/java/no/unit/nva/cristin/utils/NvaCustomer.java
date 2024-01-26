package no.unit.nva.cristin.utils;

import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.PATH_CUSTOMER;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Organization;
import no.unit.nva.publication.external.services.RawContentRetriever;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public class NvaCustomer {

    private static final String APPLICATION_JSON = "application/json";
    private static final String API_HOST = new Environment().readEnv("DOMAIN_NAME");
    private static final Map<String, String> CUSTOMER_MAP = Map.of("api.sandbox.nva.aws.unit.no",
                                                                   "bb3d0c0c-5065-4623-9b98-5810983c2478",
                                                                   "api.dev.nva.aws.unit.no",
                                                                   "bb3d0c0c-5065-4623-9b98-5810983c2478",
                                                                   "api.test.nva.aws.unit.no",
                                                                   "0baf8fcb-b18d-4c09-88bb-956b4f659103",
                                                                   "api.e2e.nva.aws.unit.no",
                                                                   "bb3d0c0c-5065-4623-9b98-5810983c2478",
                                                                   "api.nva.unit.no",
                                                                   "22139870-8d31-4df9-bc45-14eb68287c4a");
    public static final String TOP = "top";
    public static final String DEPTH = "depth";
    public static final String CUSTOMER = "customer";
    public static final String CRISTIN_ID = "cristinId";
    public static final String PATH_DELIMITER = "/";
    private URI id;
    private URI cristinId;

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return new Builder().withCristinId(this.cristinId).withId(this.id);
    }

    public static NvaCustomer fromCristinOrganization(URI cristinOrganizationId) {
        return NvaCustomer.builder().withCristinId(cristinOrganizationId).build();
    }

    public NvaCustomer fetch(RawContentRetriever uriRetriever) {
        return attempt(() -> uriRetriever.getRawContent(fetchCristinOrganizationUri(), APPLICATION_JSON))
                   .map(Optional::orElseThrow)
                   .map(NvaCustomer::toCristinOrganization)
                   .map(CristinOrganization::getTopLevelOrganization)
                   .map(CristinOrganization::id)
                   .map(NvaCustomer::encode)
                   .map(NvaCustomer::constructFetchNvaCustomerUri)
                   .map(uri -> uriRetriever.getRawContent(uri, APPLICATION_JSON))
                   .map(Optional::orElseThrow)
                   .map(NvaCustomer::toNvaCustomerResponse)
                   .map(NvaCustomerResponse::id)
                   .map(id -> copy().withId(id).build())
                   .orElse(failure -> emptyNvaCustomer());
    }

    private URI fetchCristinOrganizationUri() {
        return UriWrapper.fromUri(cristinId).addQueryParameter(DEPTH, TOP).getUri();
    }

    private NvaCustomer emptyNvaCustomer() {
        return new NvaCustomer();
    }

    private static NvaCustomerResponse toNvaCustomerResponse(String value) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, NvaCustomerResponse.class)).orElseThrow();
    }

    private static URI constructFetchNvaCustomerUri(String value) {
        var uri = UriWrapper.fromHost(API_HOST)
                      .addChild(CUSTOMER)
                      .addChild(CRISTIN_ID)
                      .getUri();

        return URI.create(uri.toString() + PATH_DELIMITER + value);
    }

    private static String encode(URI uri) {
        return URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8);
    }

    private static CristinOrganization toCristinOrganization(String body) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(body, CristinOrganization.class)).orElseThrow();
    }

    public Organization toOrganization() {
        return nonNull(id) ? new Organization.Builder().withId(id).build() : siktOrganization();
    }

    private Organization siktOrganization() {
        var customerId = UriWrapper.fromUri(NVA_API_DOMAIN).addChild(PATH_CUSTOMER, getSiktCustomerId());
        return new Organization.Builder().withId(customerId.getUri()).build();
    }

    private String getSiktCustomerId() {
        return Optional.ofNullable(CUSTOMER_MAP.get(API_HOST)).orElseThrow();
    }

    public static final class Builder {

        private URI id;
        private URI cristinId;

        private Builder() {
        }

        public Builder withId(URI id) {
            this.id = id;
            return this;
        }

        public Builder withCristinId(URI cristinId) {
            this.cristinId = cristinId;
            return this;
        }

        public NvaCustomer build() {
            NvaCustomer nvaCustomer = new NvaCustomer();
            nvaCustomer.cristinId = this.cristinId;
            nvaCustomer.id = this.id;
            return nvaCustomer;
        }
    }
}
