package no.unit.nva.publication.service.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.exception.NotImplementedException;
import no.unit.nva.publication.model.PublicationSummary;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.utils.Environment;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.publication.service.impl.PublicationsDynamoDBLocal.BY_PUBLISHER_INDEX_NAME;
import static no.unit.nva.publication.service.impl.PublicationsDynamoDBLocal.NVA_RESOURCES_TABLE_NAME;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
class DynamoDBPublicationServiceTest {

    private static final UUID ID1 = UUID.randomUUID();
    private static final  UUID ID2 = UUID.randomUUID();

    private static final Instant INSTANT1 = Instant.now();
    private static final Instant INSTANT2 = INSTANT1.plusSeconds(100);
    private static final Instant INSTANT3 = INSTANT2.plusSeconds(100);
    private static final Instant INSTANT4 = INSTANT3.plusSeconds(100);

    public static final String OWNER = "owner@example.org";
    public static final URI PUBLISHER_ID = URI.create("http://example.org/123");
    public static final String TABLE_NAME_ENV = "TABLE_NAME";
    public static final String BY_PUBLISHER_INDEX_NAME_ENV = "BY_PUBLISHER_INDEX_NAME";
    public static final String INVALID_JSON = "{\"test\" = \"invalid json }";

    @Rule
    public PublicationsDynamoDBLocal publicationsDynamoDBLocal =  new PublicationsDynamoDBLocal();

    private DynamoDBPublicationService publicationService;
    private Environment environment;
    private AmazonDynamoDB client;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        client = DynamoDBEmbedded.create().amazonDynamoDB();
        environment = mock(Environment.class);
        publicationService = new DynamoDBPublicationService(
                objectMapper,
                publicationsDynamoDBLocal.getByPublisherIndex()
        );
    }

    @Test
    @DisplayName("calling Constructor When Missing Env Throws Exception")
    public void callingConstructorWhenMissingEnvThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new DynamoDBPublicationService(client, objectMapper, environment)
        );
    }

    @Test
    @DisplayName("missing Table Env")
    public void missingTableEnv() {
        when(environment.readEnv(TABLE_NAME_ENV)).thenReturn(NVA_RESOURCES_TABLE_NAME);
        assertThrows(IllegalArgumentException.class,
            () -> new DynamoDBPublicationService(client, objectMapper, environment)
        );
    }

    @Test
    @DisplayName("missing Index Env")
    public void missingIndexEnv() {
        when(environment.readEnv(BY_PUBLISHER_INDEX_NAME_ENV)).thenReturn(BY_PUBLISHER_INDEX_NAME);
        assertThrows(IllegalArgumentException.class,
            () -> new DynamoDBPublicationService(client, objectMapper, environment)
        );
    }

    @Test
    @DisplayName("calling Constructor With All Env")
    public void callingConstructorWithAllEnv() {
        Environment environment = Mockito.mock(Environment.class);
        when(environment.readEnv(DynamoDBPublicationService.TABLE_NAME_ENV)).thenReturn(TABLE_NAME_ENV);
        when(environment.readEnv(DynamoDBPublicationService.BY_PUBLISHER_INDEX_NAME_ENV))
                .thenReturn(BY_PUBLISHER_INDEX_NAME);
        new DynamoDBPublicationService(client, objectMapper, environment);
    }

    @Test
    @DisplayName("empty Table Returns No Publications")
    public void emptyTableReturnsNoPublications() throws ApiGatewayException {
        List<PublicationSummary> publications = publicationService.getPublicationsByOwner(
                OWNER,
                PUBLISHER_ID,
                null);

        assertEquals(0, publications.size());
    }

    @Test
    @DisplayName("nonEmpty Table Returns Publications")
    public void nonEmptyTableReturnsPublications() throws IOException, ApiGatewayException {
        Publication publication = publication();
        insertPublication(publication);

        List<PublicationSummary> publications = publicationService.getPublicationsByOwner(
                OWNER,
                PUBLISHER_ID,
                null);

        assertEquals(1, publications.size());
        assertEquals(publication.getEntityDescription().getMainTitle(), publications.get(0).getMainTitle());
        assertEquals(publication.getOwner(), publications.get(0).getOwner());

    }

    @Test
    @DisplayName("invalid Item Json")
    public void invalidItemJson() {
        Item item = mock(Item.class);
        when(item.toJSON()).thenReturn(INVALID_JSON);
        Optional<PublicationSummary> publicationSummary = publicationService.toPublicationSummary(item);
        Assertions.assertTrue(publicationSummary.isEmpty());
    }

    @Test
    @DisplayName("notImplemented Methods Throws RunTimeException")
    public void notImplementedMethodsThrowsRunTimeException() {
        assertThrows(NotImplementedException.class, () ->  {
            publicationService.getPublication(null,null);
        });
        assertThrows(NotImplementedException.class, () ->  {
            publicationService.updatePublication(null, null, null);
        });
        assertThrows(NotImplementedException.class, () ->  {
            publicationService.getPublicationsByPublisher(null, null);
        });
    }

    @Test
    @DisplayName("filterOutOlderVersionsOfPublications returns only the latest version of each publication")
    public void filterOutOlderVersionsOfPublicationsReturnsOnlyTheLatestVersionOfEachPublication() {
        List<PublicationSummary> publications = publicationSummariesWithDuplicateUuuIds();
        ArrayList<PublicationSummary> expected = new ArrayList<>();
        expected.add(createPublication(ID1, INSTANT2));
        expected.add(createPublication(ID2, INSTANT4));
        List<PublicationSummary> actual = DynamoDBPublicationService.filterOutOlderVersionsOfPublications(publications);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
        assertThat(expected, containsInAnyOrder(actual.toArray()));
    }

    @Test
    @DisplayName("filterOutOlderVersionsOfPublications returns only the single version of a publication")
    public void filterOutOlderVersionsOfPublicationsReturnsTheSingleVersionOfAPublication() {
        List<PublicationSummary> publications = publicationSummariesWithoutDuplicateUuIds();
        ArrayList<PublicationSummary> expected = new ArrayList<>();
        expected.add(createPublication(ID1, INSTANT1));
        expected.add(createPublication(ID2, INSTANT3));
        List<PublicationSummary> actual = DynamoDBPublicationService.filterOutOlderVersionsOfPublications(publications);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
        assertThat(expected, containsInAnyOrder(actual.toArray()));
    }

    private List<PublicationSummary> publicationSummariesWithDuplicateUuuIds() {
        List<PublicationSummary> publicationSummaries = new ArrayList<>();

        publicationSummaries.add(createPublication(ID1, INSTANT1));
        publicationSummaries.add(createPublication(ID1, INSTANT2));
        publicationSummaries.add(createPublication(ID2, INSTANT3));
        publicationSummaries.add(createPublication(ID2, INSTANT4));

        return publicationSummaries;
    }

    private List<PublicationSummary> publicationSummariesWithoutDuplicateUuIds() {
        List<PublicationSummary> publicationSummaries = new ArrayList<>();
        publicationSummaries.add(createPublication(ID1, INSTANT1));
        publicationSummaries.add(createPublication(ID2, INSTANT3));
        return publicationSummaries;
    }

    private PublicationSummary createPublication(UUID id, Instant modifiedDate) {
        return new PublicationSummary.Builder()
            .withIdentifier(id)
            .withModifiedDate(modifiedDate)
            .withCreatedDate(INSTANT1)
            .withOwner("junit")
            .withMainTitle("Some main title")
            .withStatus(DRAFT)
            .build();
    }

    private Publication publication() {
        Instant now = Instant.now();
        return new Publication.Builder()
                .withIdentifier(UUID.randomUUID())
                .withCreatedDate(now)
                .withModifiedDate(now)
                .withOwner(OWNER)
                .withStatus(PublicationStatus.DRAFT)
                .withPublisher(new Organization.Builder().withId(PUBLISHER_ID).build())
                .withEntityDescription(new EntityDescription.Builder().withMainTitle("DynamoDB Local Testing").build())
                .build();
    }

    private void insertPublication(Publication publication) throws IOException {
        publicationsDynamoDBLocal.getTable().putItem(Item.fromJSON(objectMapper.writeValueAsString(publication)));
    }
}