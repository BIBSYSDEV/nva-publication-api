package no.unit.nva.expansion;

import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.nio.file.Path;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationResponseObjectTest {

    public static final URI CRISTIN_ORG_ID = URI.create(
        "https://api.dev.nva.aws.unit.no/cristin/organization/194.63.10.0");
    public static final URI CRISTIN_ORG_PARENT_ID = URI.create(
        "https://api.dev.nva.aws.unit.no/cristin/organization/194.63.0.0");
    public static final URI CRISTIN_ORG_GRAND_PARENT_ID = URI.create(
        "https://api.dev.nva.aws.unit.no/cristin/organization/194.0.0.0");

    private static final String CRISTIN_ORG_RESPONSE_OBJECT =
        stringFromResources(Path.of("cristin_org.json"));
    private static final String CRISTIN_ORG_PARENT_RESPONSE_OBJECT =
        stringFromResources(Path.of("cristin_parent_org.json"));
    private static final String CRISTIN_ORG_GRAND_PARENT_RESPONSE_OBJECT =
        stringFromResources(Path.of("cristin_grand_parent_org.json"));
    private FakeHttpClient<String> httpClient;

    @BeforeEach
    public void init() {
        httpClient = new FakeHttpClient<>(CRISTIN_ORG_RESPONSE_OBJECT,
                                          CRISTIN_ORG_PARENT_RESPONSE_OBJECT,
                                          CRISTIN_ORG_GRAND_PARENT_RESPONSE_OBJECT);
    }

    @Test
    void organizationResponseShouldBeDeserializable() throws JsonProcessingException {
        var json = stringFromResources(Path.of("cristin_org.json"));
        var parseObject = ExpansionConfig.objectMapper.readValue(json, OrganizationResponseObject.class);
        assertThat(parseObject.getId(), is(equalTo(CRISTIN_ORG_ID)));
        assertThat(parseObject.getPartOf(), is(not(empty())));
        assertThat(parseObject.getPartOf().get(0).getId(), is(equalTo(CRISTIN_ORG_PARENT_ID)));
    }

    @Test
    void getAncestorsReturnsAllAncestorsOfOrganization() {
        var result = OrganizationResponseObject.retrieveAllRelatedOrganizations(httpClient, CRISTIN_ORG_ID);
        assertThat(result, containsInAnyOrder(
            CRISTIN_ORG_ID,
            CRISTIN_ORG_PARENT_ID,
            CRISTIN_ORG_GRAND_PARENT_ID
        ));
    }
}