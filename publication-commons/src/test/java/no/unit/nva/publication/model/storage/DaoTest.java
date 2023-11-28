package no.unit.nva.publication.model.storage;

import com.amazonaws.services.dynamodbv2.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.*;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.model.storage.DaoUtils.toPutItemRequest;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.*;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.text.IsEmptyString.emptyString;

class DaoTest extends ResourcesLocalTest {

    private static final String DATA_FINALIZED_BY = "data.finalizedBy";
    private static final String DATA_FINALIZED_DATE = "data.finalizedDate";
    private static final String DATA_ASSIGNEE = "data.assignee";
    public static final String DATA_IMPORT_STATUS = "data.importStatus";
    public static final String RESOURCE_IMPORT_STATUS = "resource.importStatus";
    private static final String DATA_OWNER_AFFILIATION = "data.ownerAffiliation";

    public static Stream<Class<?>> entityProvider() {
        return TypeProvider.listSubTypes(Entity.class);
    }

    public static Stream<Class<?>> ticketProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }
    
    public static Publication draftPublicationWithoutDoi() {
        return randomPublication().copy()
                   .withStatus(PublicationStatus.DRAFT)
                   .withDoi(null)
                   .build();
    }
    
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
    
    @ParameterizedTest
        (name = "daoByCustomerAndStatusIndexPartitionKey contains only Type, CustomerIdentifier and Status "
                + "in that order: {0}")
    @MethodSource("instanceProvider")
    void daoByCustomerAndStatusIndexPartitionKeyContainsOnlyTypeCustomerIdentifierAndStatusInThatOrder(Dao dao)
        throws JsonProcessingException {
        
        JsonNode jsonNode = serializeInstance(dao);
        assertThat(jsonNode.get(BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME), is(not(nullValue())));
        String byTypeCustomerStatusIndexPartitionKey = dao.getByTypeCustomerStatusPartitionKey();
        
        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
            dao.indexingType(),
            CUSTOMER_INDEX_FIELD_PREFIX,
            dao.getCustomerIdentifier(),
            STATUS_INDEX_FIELD_PREFIX,
            dao.getData().getStatusString());
        
        assertThat(byTypeCustomerStatusIndexPartitionKey, is(equalTo(expectedFormat)));
    }
    
    @ParameterizedTest(name = "daoByCustomerAndStatusIndexSortKey contains only type and identifier: {0}")
    @MethodSource("instanceProvider")
    void daoByCustomerAndStatusIndexSortKeyContainsOnlyTypeAndIdentifier(Dao dao)
        throws JsonProcessingException {
        JsonNode jsonNode = serializeInstance(dao);
        assertThat(jsonNode.get(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME), is(not(nullValue())));
        String byTypeCustomerStatusIndexPartitionKey = dao.getByTypeCustomerStatusSortKey();
        
        String expectedFormat = String.join(KEY_FIELDS_DELIMITER,
            dao.indexingType(),
            dao.getIdentifier().toString());
        
        assertThat(byTypeCustomerStatusIndexPartitionKey, is(equalTo(expectedFormat)));
    }
    
    @ParameterizedTest(name = "dao can be retrieved by primary-key from dynamo: {0}")
    @MethodSource("instanceProvider")
    void daoCanBeRetrievedByPrimaryKeyFromDynamo(Dao originalResource) {
        
        client.putItem(toPutItemRequest(originalResource));
        GetItemResult getItemResult = client.getItem(
            new GetItemRequest().withTableName(RESOURCES_TABLE_NAME)
                .withKey(originalResource.primaryKey()));
        Dao retrievedResource = parseAttributeValuesMap(getItemResult.getItem(), originalResource.getClass());
        
        assertThat(originalResource, doesNotHaveEmptyValuesIgnoringFields(Set.of(DATA_OWNER_AFFILIATION,
                                                                                 DATA_ASSIGNEE, DATA_FINALIZED_BY,
                                                                                 DATA_FINALIZED_DATE, DATA_IMPORT_STATUS,
                RESOURCE_IMPORT_STATUS)));
        assertThat(originalResource, is(equalTo(retrievedResource)));
    }
    
    @ParameterizedTest(name = "dao can be retrieved by the ByTypePublisherStatus index: {0}")
    @MethodSource("instanceProvider")
    void daoCanBeRetrievedByTypePublisherStatusIndex(Dao originalDao) {
        client.putItem(toPutItemRequest(originalDao));
        QueryResult queryResult = client.query(queryByTypeCustomerStatusIndex(originalDao));
        Dao retrievedDao = queryResult.getItems()
                               .stream()
                               .map(map -> parseAttributeValuesMap(map, originalDao.getClass()))
                               .collect(SingletonCollector.collect());
        assertThat(retrievedDao, is(equalTo(originalDao)));
    }
    
    @ParameterizedTest
    @MethodSource("instanceProvider")
    void parseAttributeValuesMapCreatesDaoWithoutLossOfInformation(Dao originalDao) {
        
        assertThat(originalDao, doesNotHaveEmptyValuesIgnoringFields(Set.of(DATA_OWNER_AFFILIATION, DATA_ASSIGNEE,
                                                                            DATA_FINALIZED_BY,
                                                                            DATA_FINALIZED_DATE, DATA_IMPORT_STATUS, RESOURCE_IMPORT_STATUS)));
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
        assertThat(retrievedDao, doesNotHaveEmptyValuesIgnoringFields(
                Set.of(DATA_OWNER_AFFILIATION, DATA_ASSIGNEE, DATA_FINALIZED_BY, DATA_FINALIZED_DATE,
                       DATA_IMPORT_STATUS, RESOURCE_IMPORT_STATUS)));
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
        return TicketEntry.createNewTicket(draftPublicationWithoutDoi(), entityType, SortableIdentifier::next);
    }
    
    private static Stream<Dao> instanceProvider() {
        return DaoUtils.instanceProvider();
    }
    
    @SuppressWarnings("unchecked")
    private Object generateEntity(Class<?> entityType)
        throws ConflictException {
        
        if (Resource.class.equals(entityType)) {
            return Resource.fromPublication(randomPublication());
        } else if (ImportCandidate.class.equals(entityType)) {
            return Resource.fromImportCandidate(randomImportCandidate());
        } else if (TicketEntry.class.isAssignableFrom(entityType)) {
            return createTicket((Class<? extends TicketEntry>) entityType);
        } else if (Message.class.equals(entityType)) {
            var ticket = createTicket(DoiRequest.class);
            return Message.create(ticket, UserInstance.fromTicket(ticket), randomString());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder()
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .withEntityDescription(randomEntityDescription())
                   .withLink(randomUri())
                   .withDoi(randomDoi())
                   .withIndexedDate(Instant.now())
                   .withPublishedDate(Instant.now())
                   .withHandle(randomUri())
                   .withModifiedDate(Instant.now())
                   .withCreatedDate(Instant.now())
                   .withPublisher(new Organization.Builder().withId(randomUri()).build())
                   .withSubjects(List.of(randomUri()))
                   .withIdentifier(SortableIdentifier.next())
                   .withRightsHolder(randomString())
                   .withProjects(List.of(new ResearchProject.Builder().withId(randomUri()).build()))
                   .withFundings(List.of(new FundingBuilder().withIdentifier(randomString()).build()))
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

    private QueryRequest queryByTypeCustomerStatusIndex(Dao originalResource) {
        return new QueryRequest()
                   .withTableName(RESOURCES_TABLE_NAME)
                   .withIndexName(BY_TYPE_CUSTOMER_STATUS_INDEX_NAME)
                   .withKeyConditions(originalResource.fetchEntryByTypeCustomerStatusKey());
    }
    
    private JsonNode serializeInstance(Dao daoInstance) throws JsonProcessingException {
        String json = dynamoDbObjectMapper.writeValueAsString(daoInstance);
        return dynamoDbObjectMapper.readTree(json);
    }
}