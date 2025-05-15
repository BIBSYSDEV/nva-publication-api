package no.sikt.nva.brage.migration.mapper;

import static no.sikt.nva.brage.migration.mapper.Customer.Environment.PROD;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import no.sikt.nva.brage.migration.record.UnknownCustomerException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Organization;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import nva.commons.core.SingletonCollector;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;

public record Customer(String name,
                       String cristinIdentifier,
                       String username,
                       String shortName,
                       Map<Environment, String> identifiers) {

    public static final String CUSTOMERS_JSON_STRING = IoUtils.stringFromResources(Path.of("customers.json"));
    public static final String CUSTOMER = "customer";
    public static final String CRISTIN = "cristin";
    public static final String ORGANIZATION = "organization";

    public static Customer fromBrageArchiveName(String customerShortName) {
        var customers = readCustomers();
        return Arrays.stream(customers)
                   .filter(entry -> entry.name().equals(customerShortName))
                   .findFirst()
                   .orElseThrow(() -> new UnknownCustomerException(customerShortName));
    }

    private static Customer[] readCustomers() {
        return attempt(
            () -> JsonUtils.dtoObjectMapper.readValue(CUSTOMERS_JSON_STRING, Customer[].class)).orElseThrow();
    }

    public Organization toPublisher(String host) {
        var uri = UriWrapper.fromHost(host).addChild(CUSTOMER).addChild(identifierFromHost(host)).getUri();
        return new Organization.Builder().withId(uri).build();
    }

    public ResourceOwner toResourceOwner(String host) {
        return new ResourceOwner(new Username(username), constructCristinOrganizationId(host));
    }

    private URI constructCristinOrganizationId(String host) {
        return UriWrapper.fromHost(host).addChild(CRISTIN).addChild(ORGANIZATION).addChild(cristinIdentifier).getUri();
    }

    private String identifierFromHost(String host) {
        return identifiers.entrySet()
                   .stream()
                   .filter(entry -> host.contains(entry.getKey().getValue()))
                   .findFirst()
                   .orElseGet(this::getProductionIdentifier)
                   .getValue();
    }

    private Entry<Environment, String> getProductionIdentifier() {
        return identifiers.entrySet()
                   .stream()
                   .filter(environmentStringEntry -> PROD.equals(environmentStringEntry.getKey()))
                   .findFirst()
                   .orElseThrow();
    }

    public enum Environment {
        SANDBOX("sandbox"), E2E("e2e"), DEV("dev"), TEST("test"), PROD("prod");

        private final String value;

        Environment(String value) {
            this.value = value;
        }

        @JsonCreator
        public static Environment fromValue(String value) {
            return Arrays.stream(Environment.values())
                       .filter(item -> item.getValue().equals(value))
                       .collect(SingletonCollector.collect());
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }
}
