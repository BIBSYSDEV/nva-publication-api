package no.unit.nva.publication.model.storage;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublicationWithStatus;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.model.storage.DaoUtils.toPutItemRequest;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.text.IsEmptyString.emptyString;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy;
import no.unit.nva.publication.model.business.publicationchannel.ChannelType;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.Constraint;
import no.unit.nva.publication.model.business.publicationchannel.NonClaimedPublicationChannel;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DaoTest extends ResourcesLocalTest {

    public static final String DATA_APPROVED_FILES = "data.approvedFiles";
    private static final String DATA_ASSIGNEE = "data.assignee";
    public static final String DATA_FILES_FOR_APPROVAL = "data.filesForApproval";
    private static final String DATA_FINALIZED_BY = "data.finalizedBy";
    private static final String DATA_FINALIZED_DATE = "data.finalizedDate";
    public static final String DATA_IMPORT_STATUS = "data.importStatus";
    private static final String DATA_OWNER_AFFILIATION = "data.ownerAffiliation";
    private static final String DATA_STATE = "data.resourceEvent";
    private static final String RESOURCE_STATE = "resource.resourceEvent";
    public static final String DATA_REVISION = "data.entityDescription.reference.publicationContext.revision";
    public static final String RESOURCE_IMPORT_STATUS = "resource.importStatus";
    public static final String RESOURCE_REVISION = "resource.entityDescription.reference.publicationContext.revision";
    public static final String RESOURCE_FILES = ".resource.files";
    private static final String DATA_FILES = ".data.files";
    private static final String DATA_FILE_ENTRIES = ".data.fileEntries";
    private static final String DATA_RESPONSIBILITY_AREA = "data.responsibilityArea";
    private static final String RESOURCE_FILE_ENTRIES = ".resource.fileEntries";
    public static final String DATA_TICKET_EVENT = "data.ticketEvent";
    private static final String DATA_VIEWED_BY = "data.viewedBy";
    protected static final String DATA_PUBLICATION_CHANNELS = "data.publicationChannels";
    protected static final String RESOURCE_PUBLICATION_CHANNELS = "resource.publicationChannels";
    public static final String RESOURCE_IMPORT_DETAILS = "resource.importDetails";
    public static final String IMPORT_DETAILS = ".importDetails";
    public static final String DATA_IMPORT_DETAILS = "data.importDetails";
    public static final String RESOURCE_ASSOCIATED_CUSTOMERS = "resource.associatedCustomers";
    public static final String DATA_ASSOCIATED_CUSTOMERS = "data.associatedCustomers";
    public static final Set<String> IGNORED_FIELDS = Set.of(DATA_OWNER_AFFILIATION,
                                                            DATA_RESPONSIBILITY_AREA,
                                                            DATA_ASSIGNEE,
                                                            DATA_FINALIZED_BY,
                                                            DATA_FINALIZED_DATE, DATA_IMPORT_STATUS,
                                                            RESOURCE_IMPORT_STATUS, RESOURCE_REVISION,
                                                            DATA_REVISION,
                                                            DATA_APPROVED_FILES,
                                                            DATA_FILES_FOR_APPROVAL,
                                                            DATA_STATE, RESOURCE_STATE, RESOURCE_FILES, DATA_FILES,
                                                            RESOURCE_FILE_ENTRIES, DATA_FILE_ENTRIES,
                                                            DATA_TICKET_EVENT,
                                                            DATA_VIEWED_BY,
                                                            DATA_PUBLICATION_CHANNELS,
                                                            RESOURCE_PUBLICATION_CHANNELS,
                                                            RESOURCE_IMPORT_DETAILS,
                                                            IMPORT_DETAILS,
                                                            DATA_IMPORT_DETAILS,
                                                            RESOURCE_ASSOCIATED_CUSTOMERS,
                                                            DATA_ASSOCIATED_CUSTOMERS);

    public static Stream<Named<Class<?>>> entityProvider() {
        return TypeProvider.listSubTypes(Entity.class);
    }

    public static Stream<Named<Class<?>>> ticketProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }

    @Override
    @BeforeEach
    public void init() {
        super.init();
    }

    @ParameterizedTest(name = "dataType returns name of the contained object: {0}")
    @MethodSource("instanceProvider")
    void getTypeReturnsNameOfTheContainedObject(Dao daoInstance) {
        String expectedType = daoInstance.getData().getClass().getSimpleName();
        assertThat(daoInstance.dataType(), is(equalTo(expectedType)));
    }

    @ParameterizedTest(name = "getIdentifier returns the identifier of the contained object: {0}")
    @MethodSource("instanceProvider")
    void getIdentifierReturnsTheIdentifierOfTheContainedObject(Dao daoInstance) {
        String expectedIdentifier = daoInstance.getData().getIdentifier().toString();
        assertThat(expectedIdentifier, is(not(emptyString())));

        assertThat(daoInstance.getIdentifier().toString(), is(equalTo(expectedIdentifier)));
    }

    @ParameterizedTest(name = "getCustomerId returns the customerId of the contained object: {0}")
    @MethodSource("instanceProvider")
    void getCustomerIdReturnsTheCustomerIdOfTheContainedObject(Dao dao) {
        String expectedCustomerId = dao.getData().getCustomerId().toString();
        assertThat(expectedCustomerId, is(not(emptyString())));

        assertThat(dao.getCustomerId().toString(), is(equalTo(expectedCustomerId)));
    }

    @ParameterizedTest(name = "daoPrimaryKeyPartitionKey contains only Type, CustomerIdentifier, and Owner "
                              + "in that order: {0}")
    @MethodSource("instanceProvider")
    void daoPrimaryKeyPartitionKeyContainsOnlyTypeCustomerIdentifierAndOwnerInThatOrder(Dao daoInstance)
        throws JsonProcessingException {
        JsonNode jsonNode = serializeInstance(daoInstance);

        assertThat(jsonNode.get(PRIMARY_KEY_PARTITION_KEY_NAME), is(not(nullValue())));
        String primaryKeyPartitionKey = jsonNode.get(PRIMARY_KEY_PARTITION_KEY_NAME).textValue();

        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
                                            daoInstance.indexingType(),
                                            daoInstance.getCustomerIdentifier(),
                                            daoInstance.getOwner().toString()
        );

        assertThat(primaryKeyPartitionKey, is(equalTo(expectedFormat)));
    }

    @ParameterizedTest(name = "daoPrimaryKeySortKey contains only Type and Identifier in that order: {0}")
    @MethodSource("instanceProvider")
    void daoPrimaryKeySortKeyContainsOnlyTypeAndIdentifierInThatOrder(Dao daoInstance)
        throws JsonProcessingException {
        JsonNode jsonNode = serializeInstance(daoInstance);
        assertThat(jsonNode.get(PRIMARY_KEY_SORT_KEY_NAME), is(not(nullValue())));
        String primaryKeySortKey = jsonNode.get(PRIMARY_KEY_SORT_KEY_NAME).textValue();

        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
                                            daoInstance.indexingType(),
                                            daoInstance.getIdentifier().toString());
        assertThat(primaryKeySortKey, is(equalTo(expectedFormat)));
    }

    @ParameterizedTest(name = "dao can be retrieved by primary-key from dynamo: {0}")
    @MethodSource("instanceProvider")
    void daoCanBeRetrievedByPrimaryKeyFromDynamo(Dao originalResource) {
        client.putItem(toPutItemRequest(originalResource));
        GetItemResult getItemResult = client.getItem(
            new GetItemRequest().withTableName(RESOURCES_TABLE_NAME)
                .withKey(originalResource.primaryKey()));
        Dao retrievedResource = parseAttributeValuesMap(getItemResult.getItem(), originalResource.getClass());

        assertThat(originalResource, doesNotHaveEmptyValuesIgnoringFields(IGNORED_FIELDS));
        assertThat(originalResource, is(equalTo(retrievedResource)));
    }

    @ParameterizedTest
    @MethodSource("instanceProvider")
    void parseAttributeValuesMapCreatesDaoWithoutLossOfInformation(Dao originalDao) {

        assertThat(originalDao, doesNotHaveEmptyValuesIgnoringFields(IGNORED_FIELDS));
        Map<String, AttributeValue> dynamoMap = originalDao.toDynamoFormat();
        Dao parsedDao = parseAttributeValuesMap(dynamoMap, originalDao.getClass());
        assertThat(parsedDao, is(equalTo(originalDao)));
    }

    @ParameterizedTest(name = "toDynamoFormat creates a Dynamo object preserving all information")
    @MethodSource("instanceProvider")
    void toDynamoFormatCreatesADynamoJsonFormatObjectPreservingAllInformation(Dao originalDao) {

        Map<String, AttributeValue> dynamoMap = originalDao.toDynamoFormat();
        client.putItem(RESOURCES_TABLE_NAME, dynamoMap);
        Map<String, AttributeValue> savedMap = client
                                                   .getItem(RESOURCES_TABLE_NAME, originalDao.primaryKey())
                                                   .getItem();
        assertThat(dynamoMap, is(equalTo(savedMap)));

        Dao retrievedDao = parseAttributeValuesMap(savedMap, originalDao.getClass());
        assertThat(retrievedDao, doesNotHaveEmptyValuesIgnoringFields(IGNORED_FIELDS));
        assertThat(retrievedDao, is(equalTo(originalDao)));
    }

    @ParameterizedTest(name = "Dao type:{0}")
    @DisplayName("should generate a new version whenever it is instantiated through a Business Object")
    @MethodSource("entityProvider")
    void shouldGenerateNewVersionWheneverIsInstantiatedThroughBusinessObject(Class<?> entityType)
        throws ConflictException {
        var entity = (Entity) generateEntity(entityType);
        var dao = entity.toDao();
        var dao2 = entity.toDao();
        assertThat(dao.getVersion(), is(not(nullValue())));
        assertThat(dao2.getVersion(), is(not(nullValue())));
        assertThat(dao.getVersion(), is(not(equalTo(dao2.getVersion()))));
    }

    @ParameterizedTest(name = "Dao type:{0}")
    @MethodSource("ticketProvider")
    void ticketsShouldHaveAllDesiredFieldsInDao(Class<?> entityType)
        throws JsonProcessingException, ConflictException {
        var entity = (Entity) generateEntity(entityType);
        var dao = entity.toDao();
        String stringValue = dynamoDbObjectMapper.writeValueAsString(dao);
        ObjectNode jsonNode = (ObjectNode) dynamoDbObjectMapper.readTree(stringValue);
        Iterator<String> fieldNames = jsonNode.fieldNames();
        List<String> fieldNameList = new ArrayList<>();
        fieldNames.forEachRemaining(fieldNameList::add);
        assertThat(fieldNameList, everyItem(anyOf(
                       startsWith("PK"),
                       startsWith("SK"),
                       equalTo("identifier"),
                       equalTo("data"),
                       equalTo("type"),
                       equalTo("version"),
                       equalTo("status"),
                       equalTo("owner"),
                       equalTo("createdDate"),
                       equalTo("modifiedDate"),
                       equalTo("customerId"),
                       equalTo("ticketIdentifier"),
                       equalTo("resourceIdentifier")
                   ))
        );
    }

    @Test
    void resourceShouldHaveAllDesiredFieldsInDao()
        throws JsonProcessingException, ConflictException {
        var entity = (Entity) generateEntity(Resource.class);
        var dao = entity.toDao();
        String stringValue = dynamoDbObjectMapper.writeValueAsString(dao);
        ObjectNode jsonNode = (ObjectNode) dynamoDbObjectMapper.readTree(stringValue);
        Iterator<String> fieldNames = jsonNode.fieldNames();
        List<String> fieldNameList = new ArrayList<>();
        fieldNames.forEachRemaining(fieldNameList::add);
        assertThat(fieldNameList, everyItem(anyOf(
                       startsWith("PK"),
                       startsWith("SK"),
                       equalTo("identifier"),
                       equalTo("data"),
                       equalTo("type"),
                       equalTo("version"),
                       equalTo("status"),
                       equalTo("doi"),
                       equalTo("modifiedDate")
                   ))
        );
    }

    private static TicketEntry createTicket(Class<? extends TicketEntry> entityType) throws ConflictException {
        return TicketEntry.createNewTicket(randomDegreePublication().copy().withStatus(PUBLISHED).build(), entityType,
                                           SortableIdentifier::next)
                   .withOwner(randomString());
    }

    private static Stream<Dao> instanceProvider() {
        return DaoUtils.instanceProvider();
    }

    @SuppressWarnings("unchecked")
    private Object generateEntity(Class<?> entityType)
        throws ConflictException {

        if (Resource.class.equals(entityType)) {
            return Resource.fromPublication(randomPublicationWithStatus(PUBLISHED));
        } else if (ImportCandidate.class.equals(entityType)) {
            return Resource.fromImportCandidate(randomImportCandidate());
        } else if (TicketEntry.class.isAssignableFrom(entityType)) {
            return createTicket((Class<? extends TicketEntry>) entityType);
        } else if (Message.class.equals(entityType)) {
            var ticket = createTicket(DoiRequest.class);
            return Message.create(ticket, UserInstance.fromTicket(ticket), randomString());
        } else if (FileEntry.class.equals(entityType)) {
            return createRandomFileEntry();
        } else if (ClaimedPublicationChannel.class.equals(entityType)) {
            return createRandomClaimedPublicationChannel();
        } else if (NonClaimedPublicationChannel.class.equals(entityType)) {
            return createRandomNonClaimedPublicationChannel();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private ClaimedPublicationChannel createRandomClaimedPublicationChannel() {
        var randomConstraint = new Constraint(ChannelPolicy.EVERYONE, ChannelPolicy.OWNER_ONLY, List.of());
        return new ClaimedPublicationChannel(randomUri(), randomUri(), randomUri(), randomConstraint,
                                             ChannelType.PUBLISHER, SortableIdentifier.next(),
                                             SortableIdentifier.next(), Instant.now(), Instant.now());
    }

    private NonClaimedPublicationChannel createRandomNonClaimedPublicationChannel() {
        return new NonClaimedPublicationChannel(randomUri(), ChannelType.PUBLISHER, SortableIdentifier.next(),
                                                SortableIdentifier.next(), Instant.now(), Instant.now());
    }

    private FileEntry createRandomFileEntry() {
        return FileEntry.create(randomOpenFile(), SortableIdentifier.next(),
                                UserInstance.fromPublication(randomPublication()));
    }

    private ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder()
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .withEntityDescription(randomEntityDescription())
                   .withModifiedDate(Instant.now())
                   .withCreatedDate(Instant.now())
                   .withPublisher(new Organization.Builder().withId(randomUri()).build())
                   .withIdentifier(SortableIdentifier.next())
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(randomString(), randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of())
                   .build();
    }

    private EntityDescription randomEntityDescription() {
        return new EntityDescription.Builder()
                   .withPublicationDate(new PublicationDate.Builder().withYear("2020").build())
                   .withAbstract(randomString())
                   .withDescription(randomString())
                   .withContributors(List.of(randomContributor()))
                   .withMainTitle(randomString())
                   .build();
    }

    private Contributor randomContributor() {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .build();
    }

    private JsonNode serializeInstance(Dao daoInstance) throws JsonProcessingException {
        String json = dynamoDbObjectMapper.writeValueAsString(daoInstance);
        return dynamoDbObjectMapper.readTree(json);
    }
}