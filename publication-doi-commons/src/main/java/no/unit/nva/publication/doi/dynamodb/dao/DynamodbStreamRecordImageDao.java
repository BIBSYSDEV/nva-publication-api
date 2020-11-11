package no.unit.nva.publication.doi.dynamodb.dao;

import static no.unit.nva.publication.doi.JsonPointerUtils.textFromNode;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DynamodbStreamRecordImageDao {

    public static final String PUBLICATION_TYPE = "Publication";
    public static final String ERROR_MUST_BE_PUBLICATION_TYPE = "Must be a dynamodb stream record image of type "
        + "Publication";

    private final String identifier;
    private final String publicationInstanceType;
    private final String mainTitle;
    private final String dynamodbStreamRecordType;
    private final JsonNode publicationReleaseDate;
    private final String publisherId;
    private final String doi;
    private final JsonNode doiRequest;
    private final String modifiedDate;
    private final String status;
    private final List<Identity> contributorIdentities;

    protected DynamodbStreamRecordImageDao(Builder builder) {
        this.identifier = builder.identifier;
        this.publicationInstanceType = builder.publicationInstanceType;
        this.mainTitle = builder.mainTitle;
        this.dynamodbStreamRecordType = builder.dynamodbStreamRecordImageType;
        this.publicationReleaseDate = builder.publicationReleaseDate;
        this.publisherId = builder.publisherId;
        this.doi = builder.doi;
        this.doiRequest = builder.doiRequest;
        this.modifiedDate = builder.modifiedDate;
        this.status = builder.status;
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

    public JsonNode getDoiRequest() {
        return doiRequest;
    }

    public String getModifiedDate() {
        return modifiedDate;
    }

    public String getStatus() {
        return status;
    }

    public List<Identity> getContributorIdentities() {
        return contributorIdentities;
    }

    public static class Builder {

        private String identifier;
        private String publicationInstanceType;
        private String mainTitle;
        private String dynamodbStreamRecordImageType;
        private JsonNode publicationReleaseDate;
        private String publisherId;
        private String doi;
        private JsonNode doiRequest;
        private String modifiedDate;
        private String status;
        private List<Identity> contributorIdentities;

        private final DynamodbStreamRecordJsonPointers jsonPointers;

        public Builder(DynamodbStreamRecordJsonPointers jsonPointers) {
            this.jsonPointers = jsonPointers;
        }

        /**
         * Builder for constructing a {@link DynamodbStreamRecordImageDao}.
         *
         * @param rootNode json node representing the dynamodb.oldImage or dynamodb.newImage of
         * {@link com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord}.
         * @return Builder populated fields from provided jsonNode
         */
        public Builder withDynamodbStreamRecordImage(JsonNode rootNode) {
            var typeAttribute = textFromNode(rootNode, jsonPointers.getTypeJsonPointer());
            if (!PUBLICATION_TYPE.equals(typeAttribute)) {
                throw new IllegalArgumentException(ERROR_MUST_BE_PUBLICATION_TYPE);
            }
            dynamodbStreamRecordImageType = typeAttribute;
            identifier = textFromNode(rootNode, jsonPointers.getIdentifierJsonPointer());
            publicationInstanceType = textFromNode(
                rootNode,
                jsonPointers.getEntityDescriptionReferenceTypeJsonPointer());
            publicationReleaseDate = rootNode.at(jsonPointers.getEntityDescriptionMapJsonPointer());
            mainTitle = textFromNode(rootNode, jsonPointers.getMainTitleJsonPointer());
            publisherId = textFromNode(rootNode, jsonPointers.getPublisherIdJsonPointer());
            contributorIdentities = extractContributors(rootNode);
            doi = textFromNode(rootNode, jsonPointers.getDoiJsonPointer());
            doiRequest = rootNode.at(jsonPointers.getDoiRequestJsonPointer());
            status = textFromNode(rootNode, jsonPointers.getStatusJsonPointer());
            modifiedDate = textFromNode(rootNode, jsonPointers.getModifiedDateJsonPointer());
            return this;
        }

        public Builder withDoi(String doi) {
            this.doi = doi;
            return this;
        }

        public Builder withDoiRequest(JsonNode doiRequest) {
            this.doiRequest = doiRequest;
            return this;
        }

        public Builder withStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder withModifiedDate(String modifiedDate) {
            this.modifiedDate = modifiedDate;
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

        public Builder withDynamodbStreamRecordImageType(String dynamodbStreamRecordType) {
            this.dynamodbStreamRecordImageType = dynamodbStreamRecordType;
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

        public DynamodbStreamRecordImageDao build() {
            return new DynamodbStreamRecordImageDao(this);
        }

        private static Stream<JsonNode> toStream(JsonNode node) {
            return StreamSupport.stream(node.spliterator(), false);
        }

        private Identity createContributorIdentity(JsonNode jsonNode) {
            return new Identity.Builder(jsonPointers).withJsonNode(jsonNode).build();
        }

        private List<Identity> extractContributors(JsonNode record) {
            return toStream(record.at(jsonPointers.getContributorsListJsonPointer()))
                .map(this::createContributorIdentity)
                .filter(e -> Objects.nonNull(e.getName()))
                .collect(Collectors.toList());
        }
    }
}
