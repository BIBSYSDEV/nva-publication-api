package no.unit.nva.publication.service.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.instancetypes.book.Textbook;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DataCompressor;
import no.unit.nva.publication.model.storage.DoiRequestDao;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class MigrationTests extends ResourcesLocalTest {

    public static final Map<String, AttributeValue> START_FROM_BEGINNING = null;
    private S3Client s3Client;
    private ResourceService resourceService;
    private TicketService ticketService;

    @BeforeEach
    public void init() {
        super.init();
        this.s3Client = mock(S3Client.class);
        when(s3Client.utilities()).thenReturn(S3Client.create().utilities());
        when(s3Client.getObjectAsBytes(ArgumentMatchers.any(GetObjectRequest.class))).thenAnswer(
            (Answer<ResponseBytes<GetObjectResponse>>) invocationOnMock -> getUnitsResponseBytes());
        this.resourceService = getResourceServiceBuilder().build();
        this.ticketService = new TicketService(client, uriRetriever);
    }

    @Test
    void shouldWriteBackEntryAsIsWhenMigrating() throws NotFoundException {
        var publication = PublicationGenerator.randomPublication();
        var savedPublication = resourceService.insertPreexistingPublication(publication);
        migrateResources();

        var migratedResource = resourceService.getResourceByIdentifier(savedPublication.getIdentifier());
        var migratedPublication = migratedResource.toPublication();
        assertThat(migratedPublication, is(equalTo(savedPublication)));
    }

    @Test
    void shouldMigrateNonTicketDoiRequestsObjectsInTheDatabaseLeavingTheOldObjectInPlace() {
        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        createPublicationForOldDoiRequestFormatInResources(hardCodedIdentifier);
        saveFileDirectlyToDatabase("old_doi_request.json");
        migrateResources();
        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var doiRequest = allMigratedItems.stream()
                             .map(item -> DynamoEntry.parseAttributeValuesMap(item, Dao.class))
                             .filter(dao -> dao instanceof DoiRequestDao)
                             .map(Dao::getData)
                             .map(entry -> (DoiRequest) entry)
                             .collect(Collectors.toList());
        assertThat(doiRequest, hasSize(2));
    }

    @Test
    void shouldMigrateDoiRequestTicketsWithPublicationDetailsToDoiRequestTicketWithResourceIdentifier() {
        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        createPublicationForOldDoiRequestFormatInResources(hardCodedIdentifier);
        saveFileDirectlyToDatabase("ticketentry_doirequest_with_publication_details.json");
        migrateResources();
        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var doiRequest = allMigratedItems.stream()
                             .map(item -> DynamoEntry.parseAttributeValuesMap(item, Dao.class))
                             .filter(dao -> dao instanceof DoiRequestDao)
                             .map(Dao::getData)
                             .map(entry -> (DoiRequest) entry)
                             .filter(entry -> nonNull(entry.getResourceIdentifier()))
                             .collect(Collectors.toList());
        assertThat(doiRequest, hasSize(1));
    }

    @Test
    void shouldMigrateUncompressedDoiRequestToCompressedAndAddNewFields() throws IOException {
        saveFileDirectlyToDatabase("ticketentry_doirequest_with_publication_details.json");
        migrateResources();
        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var doiRequest = allMigratedItems.stream()
                             .map(item -> DynamoEntry.parseAttributeValuesMap(item, Dao.class))
                             .filter(dao -> dao instanceof DoiRequestDao)
                             .map(DoiRequestDao.class::cast)
                             .collect(Collectors.toList());
        assertThat(doiRequest, hasSize(1));

        assertThat(doiRequest.getFirst().getIdentifier(), not(nullValue()));
        assertThat(doiRequest.getFirst().getCreatedAt(), not(nullValue()));
        assertThat(doiRequest.getFirst().getModifiedAt(), not(nullValue()));
        assertThat(doiRequest.getFirst().getOwner(), not(nullValue()));
        assertThat(doiRequest.getFirst().getResourceIdentifier(), not(nullValue()));
        assertThat(doiRequest.getFirst().getCustomerId(), not(nullValue()));
        assertThat(doiRequest.getFirst().getTicketIdentifier(), not(nullValue()));

        var dbScan = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var doiAttributeValue = dbScan.getFirst();
        assertThat(doiAttributeValue.get("data").getB(), not(nullValue()));
        assertThat(doiAttributeValue.get("data").getM(), is(nullValue()));

        var compressedData = doiAttributeValue.get("data").getB().array();
        var decompressedData = new String(DataCompressor.decompress(compressedData), UTF_8);
        var doi = JsonUtils.dtoObjectMapper.readValue(decompressedData, DoiRequest.class);
        assertThat(doi.getIdentifier(), is(equalTo(doiRequest.getFirst().getIdentifier())));
    }

    @Test
    void shouldWorkWithEmptyContributors() {
        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        var publication = createPublicationForOldDoiRequestFormatInResources(hardCodedIdentifier);
        publication.getEntityDescription().setContributors(Collections.emptyList());
        updatePublication(publication);
        migrateResources();
        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var resource = getResourceStream(allMigratedItems)
                           .findFirst()
                           .orElseThrow();

        assertThat(resource.getCuratingInstitutions(), hasSize(0));
    }

    @Test
    void shouldWorkWithEmptyEntityDescription() {
        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        var publication = createPublicationForOldDoiRequestFormatInResources(hardCodedIdentifier);
        publication.setEntityDescription(null);
        updatePublication(publication);
        migrateResources();
        var allMigratedItems = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getItems();
        var resource = getResourceStream(allMigratedItems)
                           .findFirst()
                           .orElseThrow();

        assertThat(resource.getCuratingInstitutions(), hasSize(0));
    }

    @Test
    void shouldPersistNonClaimedPublicationChannelWhenDegreeWithPublisher() throws NotFoundException {
        var channelIdentifier = randomUUID();
        var publisherId = randomPublisherId(channelIdentifier);
        var publisher = new Publisher(publisherId);

        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        var publication = randomPublication(DegreeBachelor.class);
        publication.getEntityDescription().getReference().setPublicationContext(degreeWithPublisher(publisher));
        publication.setIdentifier(hardCodedIdentifier);
        updatePublication(publication);

        var fetchedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());
        Assertions.assertTrue(fetchedResource.getPublicationChannels().isEmpty());

        migrateResources();

        var updatedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());
        Assertions.assertFalse(updatedResource.getPublicationChannels().isEmpty());

        var publicationChannel = updatedResource.getPublicationChannels().getFirst();
        Assertions.assertEquals(channelIdentifier.toString(), publicationChannel.getIdentifier().toString());
    }

    @Test
    void shouldNotPersistNonClaimedPublicationChannelWhenDegreeWithoutPublisher() throws NotFoundException {
        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        var publication = randomPublication(DegreeBachelor.class);
        publication.getEntityDescription().getReference().setPublicationContext(degreeWithPublisher(null));
        publication.setIdentifier(hardCodedIdentifier);
        updatePublication(publication);

        var fetchedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());
        Assertions.assertTrue(fetchedResource.getPublicationChannels().isEmpty());

        migrateResources();

        var updatedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());
        Assertions.assertTrue(updatedResource.getPublicationChannels().isEmpty());
    }

    @Test
    void shouldNotPersistNonClaimedPublicationChannelWhenNonDegreeWithPublisher() throws NotFoundException {
        var publisherId = randomPublisherId(randomUUID());
        var publisher = new Publisher(publisherId);

        var hardCodedIdentifier = new SortableIdentifier("0183892c7413-af720123-d7ae-4a97-a628-a3762faf8438");
        var publication = randomPublication(Textbook.class);
        publication.getEntityDescription().getReference().setPublicationContext(reportWithPublisher(publisher));
        publication.setIdentifier(hardCodedIdentifier);
        updatePublication(publication);

        var fetchedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());
        Assertions.assertTrue(fetchedResource.getPublicationChannels().isEmpty());

        migrateResources();

        var updatedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());
        Assertions.assertTrue(updatedResource.getPublicationChannels().isEmpty());
    }

    private static Degree degreeWithPublisher(Publisher publisher) {
        return attempt(() ->  new Degree(null, null, null, publisher, List.of(), null))
                   .orElseThrow();
    }

    private static Report reportWithPublisher(Publisher publisher) {
        return attempt(() ->  new Report(null, null, null, publisher, List.of()))
                   .orElseThrow();
    }

    private static URI randomPublisherId(UUID channelIdentifier) {
        return UriWrapper.fromUri(randomUri())
                   .addChild("publication-channel-v2")
                   .addChild("publisher")
                   .addChild(channelIdentifier.toString())
                   .addChild(randomInteger().toString())
                   .getUri();
    }

    private static Stream<Resource> getResourceStream(List<Map<String, AttributeValue>> allMigratedItems) {
        return allMigratedItems.stream()
                   .map(item -> DynamoEntry.parseAttributeValuesMap(item, Dao.class))
                   .filter(dao -> dao instanceof ResourceDao)
                   .map(Dao::getData)
                   .map(entry -> (Resource) entry);
    }

    private void saveFileDirectlyToDatabase(String file) {
        var jsonString = IoUtils.stringFromResources(Path.of("migration", file));
        var item = Item.fromJSON(jsonString);
        var itemMap = ItemUtils.toAttributeValues(item);
        client.putItem(new PutItemRequest().withTableName(RESOURCES_TABLE_NAME).withItem(itemMap));
    }

    private Publication createPublicationForOldDoiRequestFormatInResources(SortableIdentifier hardCodedIdentifier) {
        var publication = randomPublication();
        publication.getEntityDescription()
            .getContributors()
            .forEach(contributor ->
                         contributor.getAffiliations()
                             .forEach(affiliation -> ((Organization) affiliation).setId(
                                 URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0")))
            );
        publication.setCuratingInstitutions(null);
        publication.setIdentifier(hardCodedIdentifier);
        updatePublication(publication);

        return publication;
    }

    private void updatePublication(Publication publication) {
        var resource = Resource.fromPublication(publication);
        var dao = resource.toDao();
        client.putItem(new PutItemRequest().withTableName(RESOURCES_TABLE_NAME).withItem(dao.toDynamoFormat()));
    }

    private void migrateResources() {
        var scanResources = resourceService.scanResources(1000, START_FROM_BEGINNING, Collections.emptyList());
        resourceService.refreshResources(scanResources.getDatabaseEntries());
    }

    public static ResponseBytes getUnitsResponseBytes() {
        var result = IoUtils.stringFromResources(Path.of("cristinUnits/units-norway.json"));
        var httpResponse = mock(ResponseBytes.class);
        when(httpResponse.asUtf8String()).thenReturn(result);
        return httpResponse;
    }
}
