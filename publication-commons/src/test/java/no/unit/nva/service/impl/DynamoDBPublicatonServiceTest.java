package no.unit.nva.service.impl;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.Environment;
import no.unit.nva.PublicationHandler;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.PublicationSummary;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.unit.nva.service.impl.PublicationsDynamoDBLocal.BY_PUBLISHER_INDEX_NAME;
import static no.unit.nva.service.impl.PublicationsDynamoDBLocal.NVA_RESOURCES_TABLE_NAME;
import static no.unit.nva.service.impl.RestPublicationService.NOT_IMPLEMENTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
public class DynamoDBPublicatonServiceTest {

    public static final String OWNER = "owner@example.org";
    public static final URI PUBLISHER_ID = URI.create("http://example.org/123");
    public static final String TABLE_NAME_ENV = "TABLE_NAME";
    public static final String BY_PUBLISHER_INDEX_NAME_ENV = "BY_PUBLISHER_INDEX_NAME";
    public static final String INVALID_JSON = "{\"test\" = \"invalid json }";

    @Rule
    public PublicationsDynamoDBLocal publicationsDynamoDBLocal =  new PublicationsDynamoDBLocal();

    private ObjectMapper objectMapper = PublicationHandler.createObjectMapper();
    private DynamoDBPublicationService publicationService;
    private Environment environment;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        environment = mock(Environment.class);
        publicationService = new DynamoDBPublicationService(
                objectMapper,
                publicationsDynamoDBLocal.getByPublisherIndex()
        );
    }

    @Test
    @DisplayName("calling Constructor When Missing Env Throws Exception")
    public void callingConstructorWhenMissingEnvThrowsException() {
        assertThrows(IllegalStateException.class,
            () -> new DynamoDBPublicationService(objectMapper, environment)
        );
    }

    @Test
    @DisplayName("calling Constructor With ApiHost Env Missing Throws Exception")
    public void callingConstructorWithApiHostEnvMissingThrowsException() {
        Environment environment = Mockito.mock(Environment.class);
        when(environment.get(DynamoDBPublicationService.TABLE_NAME_ENV)).thenReturn(Optional.of(TABLE_NAME_ENV));
        assertThrows(IllegalStateException.class,
            () -> new DynamoDBPublicationService(objectMapper, environment)
        );
    }

    @Test
    @DisplayName("calling Constructor With All Env")
    public void callingConstructorWithAllEnv() {
        Environment environment = Mockito.mock(Environment.class);
        when(environment.get(DynamoDBPublicationService.TABLE_NAME_ENV)).thenReturn(Optional.of(TABLE_NAME_ENV));
        when(environment.get(DynamoDBPublicationService.BY_PUBLISHER_INDEX_NAME_ENV))
                .thenReturn(Optional.of(BY_PUBLISHER_INDEX_NAME));
        new DynamoDBPublicationService(objectMapper, environment);
    }

    @Test
    @DisplayName("missing Table Env")
    public void missingTableEnv() {
        when(environment.get(TABLE_NAME_ENV)).thenReturn(Optional.of(NVA_RESOURCES_TABLE_NAME));
        assertThrows(IllegalStateException.class,
            () -> new DynamoDBPublicationService(objectMapper, new Environment()));
    }

    @Test
    @DisplayName("missing Index Env")
    public void missingIndexEnv() {
        when(environment.get(BY_PUBLISHER_INDEX_NAME_ENV)).thenReturn(Optional.of(BY_PUBLISHER_INDEX_NAME));
        assertThrows(IllegalStateException.class,
            () -> new DynamoDBPublicationService(objectMapper, new Environment())
        );
    }

    @Test
    @DisplayName("empty Table Returns No Publications")
    public void emptyTableReturnsNoPublications() throws IOException, InterruptedException {
        List<PublicationSummary> publications = publicationService.getPublicationsByOwner(
                OWNER,
                PUBLISHER_ID,
                null);

        assertEquals(0, publications.size());
    }

    @Test
    @DisplayName("nonEmpty Table Returns Publications")
    public void nonEmptyTableReturnsPublications() throws IOException, InterruptedException {
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
        assertThrows(RuntimeException.class, () ->  {
            publicationService.getPublication(null, null);
        }, NOT_IMPLEMENTED);
        assertThrows(RuntimeException.class, () ->  {
            publicationService.updatePublication(null, null);
        }, NOT_IMPLEMENTED);
        assertThrows(RuntimeException.class, () ->  {
            publicationService.getPublicationsByPublisher(null, null);
        }, NOT_IMPLEMENTED);
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
