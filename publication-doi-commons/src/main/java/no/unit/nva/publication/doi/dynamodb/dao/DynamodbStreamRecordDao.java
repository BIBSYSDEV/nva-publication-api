package no.unit.nva.publication.doi.dynamodb.dao;

import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.CONTRIBUTORS_LIST_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.DOI_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.IMAGE_IDENTIFIER_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.MAIN_TITLE_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.PUBLICATION_ENTITY_DESCRIPTION_MAP_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.PUBLISHER_ID;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.TYPE_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.textFromNode;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DynamodbStreamRecordDao {

    public static final String PUBLICATION_TYPE = "Publication";
    public static final String ERROR_MUST_BE_PUBLICATION_TYPE = "Must be a dynamodb stream record of type Publication";

    private final String identifier;
    private final String publicationInstanceType;
    private final String mainTitle;
    private final String dynamodbStreamRecordType;
    private final JsonNode publicationReleaseDate;
    private final String publisherId;
    private final String doi;
    private final List<Identity> contributorIdentities;

    protected DynamodbStreamRecordDao(Builder builder) {
        this.identifier = builder.identifier;
        this.publicationInstanceType = builder.publicationInstanceType;
        this.mainTitle = builder.mainTitle;
        this.dynamodbStreamRecordType = builder.dynamodbStreamRecordType;
        this.publicationReleaseDate = builder.publicationReleaseDate;
        this.publisherId = builder.publisherId;
        this.doi = builder.doi;
        this.contributorIdentities = builder.contributorIdentities;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getPublicationInstanceType() {
        return publicationInstanceType;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public String getDynamodbStreamRecordType() {
        return dynamodbStreamRecordType;
    }

    public JsonNode getPublicationReleaseDate() {
        return publicationReleaseDate;
    }

    public String getPublisherId() {
        return publisherId;
    }

    public String getDoi() {
        return doi;
    }

    public List<Identity> getContributorIdentities() {
        return contributorIdentities;
    }

    public static class Builder {

        private String identifier;
        private String publicationInstanceType;
        private String mainTitle;
        private String dynamodbStreamRecordType;
        private JsonNode publicationReleaseDate;
        private String publisherId;
        private String doi;
        private List<Identity> contributorIdentities;

        public Builder() {

        }

        private static Stream<JsonNode> toStream(JsonNode node) {
            return StreamSupport.stream(node.spliterator(), false);
        }

        private static Identity createContributorIdentity(JsonNode jsonNode) {
            return new Identity.Builder().withJsonNode(jsonNode).build();
        }

        private static List<Identity> extractContributors(JsonNode record) {
            return toStream(record.at(CONTRIBUTORS_LIST_POINTER))
                .map(Builder::createContributorIdentity)
                .filter(e -> Objects.nonNull(e.getName()))
                .collect(Collectors.toList());
        }

        /**
         * Builder for constructing a {@link DynamodbStreamRecordDao}.
         *
         * @param rootNode json node representing the root of
         * {@link com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord}.
         * @return Builder populated fields from provided rootNode
         */
        public Builder withDynamodbStreamRecord(JsonNode rootNode) {
            var typeAttribute = textFromNode(rootNode, TYPE_POINTER);
            if (!PUBLICATION_TYPE.equals(typeAttribute)) {
                throw new IllegalArgumentException(ERROR_MUST_BE_PUBLICATION_TYPE);
            }
            dynamodbStreamRecordType = typeAttribute;
            identifier = textFromNode(rootNode, IMAGE_IDENTIFIER_POINTER);
            publicationInstanceType = textFromNode(rootNode, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE);
            publicationReleaseDate = rootNode.at(PUBLICATION_ENTITY_DESCRIPTION_MAP_POINTER);
            mainTitle = textFromNode(rootNode, MAIN_TITLE_POINTER);
            publisherId = textFromNode(rootNode, PUBLISHER_ID);
            contributorIdentities = extractContributors(rootNode);
            doi = textFromNode(rootNode, DOI_POINTER);
            return this;
        }

        public Builder withDoi(String doi) {
            this.doi = doi;
            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withPublicationInstanceType(String publicationInstanceType) {
            this.publicationInstanceType = publicationInstanceType;
            return this;
        }

        public Builder withMainTitle(String mainTitle) {
            this.mainTitle = mainTitle;
            return this;
        }

        public Builder withDynamodbStreamRecordType(String dynamodbStreamRecordType) {
            this.dynamodbStreamRecordType = dynamodbStreamRecordType;
            return this;
        }

        public Builder withPublicationReleaseDate(JsonNode publicationReleaseDate) {
            this.publicationReleaseDate = publicationReleaseDate;
            return this;
        }

        public Builder withPublisherId(String publisherId) {
            this.publisherId = publisherId;
            return this;
        }

        public Builder withContributorIdentities(List<Identity> contributorIdentities) {
            this.contributorIdentities = contributorIdentities;
            return this;
        }

        public DynamodbStreamRecordDao build() {
            return new DynamodbStreamRecordDao(this);
        }
    }
}
