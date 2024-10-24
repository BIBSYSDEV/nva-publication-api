package no.unit.nva.publication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.commons.customer.Customer;

import nva.commons.core.ioutils.IoUtils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

/**
 * Test to avoid forgetting supporting new instance types in tests generating random publications.
 * This is a source for flaky tests.
 */
public class ResourcesContainsAllInstanceTypesTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "customerWithAllTypesAllowingFileAndNotAllowingAutoApprovalOfPublishingRequests.json",
                "customerWithAllTypesAllowingFileAndAllowingAutoApprovalOfPublishingRequestsOverridableRrs.json",
                "customerWithAllTypesAllowingFileAndAllowingAutoApprovalOfPublishingRequests.json"
            })
    void containsAllInstanceTypes(String resourcePath) throws Exception {
        String json = IoUtils.stringFromResources(Path.of(resourcePath));
        var customer = JsonUtils.dtoObjectMapper.readValue(json, Customer.class);
        var instanceTypesReferenced = customer.getAllowFileUploadForTypes();

        var instanceTypes =
                PublicationInstanceBuilder.listPublicationInstanceTypes().stream()
                        .map(Class::getSimpleName)
                        .toList();

        assertThat(
                instanceTypes,
                containsInAnyOrder(instanceTypesReferenced.toArray(new String[] {})));
    }
}
