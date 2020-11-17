package no.unit.nva.publication.doi.dynamodb.dao;

import com.fasterxml.jackson.core.JsonPointer;

@SuppressWarnings("PMD.TooManyFields")
public class DynamodbStreamRecordJsonPointers {


    public enum DynamodbImageType {
        NONE(""),
        OLD("/dynamodb/oldImage"),
        NEW("/dynamodb/newImage");

        private final String value;

        DynamodbImageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static final String IDENTIFIER = "/identifier/s";

    private static final String STATUS = "/status/s";
    private static final String MODIFIED_DATE = "/modifiedDate/s";

    private static final String DOI_REQUEST = "/doiRequest/m";
    private static final String DOI_REQUEST_STATUS = "/status/s";
    private static final String DOI_REQUEST_MODIFIED_DATE = "/modifiedDate/s";

    /**
     * Publisher ID aka INSTITUTION_OWNER_ID.
     */
    private static final String PUBLISHER_ID = "/publisherId/s";

    private static final String ENTITY_DESCRIPTION_REFERENCE_TYPE =
        "/entityDescription/m/reference/m/publicationInstance/m/type/s";

    private static final String ENTITY_DESCRIPTION_PUBLICATION_DATE = "/entityDescription/m/date/m";
    private static final String ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR = "/entityDescription/m/date/m/year/s";
    private static final String ENTITY_DESCRIPTION_PUBLICATION_DATE_MONTH = "/entityDescription/m/date/m/month/s";
    private static final String ENTITY_DESCRIPTION_PUBLICATION_DATE_DAY = "/entityDescription/m/date/m/day/s";

    private static final String ENTITY_DESCRIPTION_MAIN_TITLE = "/entityDescription/m/mainTitle/s";
    private static final String TYPE_POINTER = "/type/s";

    private static final String ENTITY_DESCRIPTION_REFERENCE_DOI = "/entityDescription/m/reference/m/doi/s";

    private static final String ENTITY_DESCRIPTION_CONTRIBUTORS = "/entityDescription/m/contributors";
    private static final String ENTITY_DESCRIPTION_CONTRIBUTORS_LIST = "/entityDescription/m/contributors/l";
    private static final String IDENTITY_ARP_ID = "/m/identity/m/arpId/s";
    private static final String IDENTITY_ORC_ID = "/m/identity/m/orcId/s";
    private static final String IDENTITY_NAME = "/m/identity/m/name/s";

    public static final String DYNAMODB_TYPE_STRING = "s";
    public static final String DYNAMODB_TYPE_LIST = "l";

    private final JsonPointer identifierJsonPointer;
    private final JsonPointer statusJoinPointer;
    private final JsonPointer doiRequestJsonPointer;
    private final JsonPointer doiRequestStatusJsonPointer;
    private final JsonPointer doiRequestModifiedDateJsonPointer;
    private final JsonPointer modifiedDateJsonPointer;
    private final JsonPointer publisherIdJsonPointer;
    private final JsonPointer entityDescriptionReferenceTypeJsonPointer;

    private final JsonPointer entityDescriptionDateJsonPointer;
    private final JsonPointer entityDescriptionDateYearJsonPointer;
    private final JsonPointer entityDescriptionDateMonthJsonPointer;
    private final JsonPointer entityDescritpionDateDayJsonPointer;
    private final JsonPointer entityDescriptionMainTitle;
    private final JsonPointer typeJsonPointer;
    private final JsonPointer doiJsonPointer;
    private final JsonPointer contributorsJsonPointer;
    private final JsonPointer contributorsListJsonPointer;
    private final JsonPointer identityArpIdJsonPointer;
    private final JsonPointer identityOrcIdJsonPointer;
    private final JsonPointer identityNameJsonPointer;

    /**
     * Construct for DynamodbStreamRecordJsonPointers DynamodbImageType to determine JsonPointer base.
     *
     * @param imageType imageType
     */
    public DynamodbStreamRecordJsonPointers(DynamodbImageType imageType) {
        String base = imageType.getValue();
        this.identifierJsonPointer = JsonPointer.compile(base + IDENTIFIER);
        this.statusJoinPointer = JsonPointer.compile(base + STATUS);
        this.doiRequestJsonPointer = JsonPointer.compile(base + DOI_REQUEST);
        this.doiRequestStatusJsonPointer = JsonPointer.compile(DOI_REQUEST_STATUS);
        this.doiRequestModifiedDateJsonPointer = JsonPointer.compile(DOI_REQUEST_MODIFIED_DATE);
        this.modifiedDateJsonPointer = JsonPointer.compile(base + MODIFIED_DATE);
        this.publisherIdJsonPointer = JsonPointer.compile(base + PUBLISHER_ID);
        this.entityDescriptionReferenceTypeJsonPointer = JsonPointer.compile(base + ENTITY_DESCRIPTION_REFERENCE_TYPE);
        this.entityDescriptionDateJsonPointer = JsonPointer.compile(base + ENTITY_DESCRIPTION_PUBLICATION_DATE);
        this.entityDescriptionDateYearJsonPointer = JsonPointer.compile(base
            + ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR);
        this.entityDescriptionDateMonthJsonPointer = JsonPointer.compile(base
            + ENTITY_DESCRIPTION_PUBLICATION_DATE_MONTH);
        this.entityDescritpionDateDayJsonPointer = JsonPointer.compile(base + ENTITY_DESCRIPTION_PUBLICATION_DATE_DAY);
        this.entityDescriptionMainTitle = JsonPointer.compile(base + ENTITY_DESCRIPTION_MAIN_TITLE);
        this.typeJsonPointer = JsonPointer.compile(base + TYPE_POINTER);
        this.doiJsonPointer = JsonPointer.compile(base + ENTITY_DESCRIPTION_REFERENCE_DOI);
        this.contributorsJsonPointer = JsonPointer.compile(base + ENTITY_DESCRIPTION_CONTRIBUTORS);
        this.contributorsListJsonPointer = JsonPointer.compile(base + ENTITY_DESCRIPTION_CONTRIBUTORS_LIST);

        this.identityArpIdJsonPointer = JsonPointer.compile(IDENTITY_ARP_ID);
        this.identityOrcIdJsonPointer = JsonPointer.compile(IDENTITY_ORC_ID);
        this.identityNameJsonPointer = JsonPointer.compile(IDENTITY_NAME);
    }

    public JsonPointer getIdentifierJsonPointer() {
        return identifierJsonPointer;
    }

    public  JsonPointer getStatusJsonPointer() {
        return statusJoinPointer;
    }

    public JsonPointer getDoiRequestJsonPointer() {
        return doiRequestJsonPointer;
    }

    public JsonPointer getDoiRequestStatusJsonPointer() {
        return doiRequestStatusJsonPointer;
    }

    public JsonPointer getDoiRequestModifiedDateJsonPointer() {
        return doiRequestModifiedDateJsonPointer;
    }

    public JsonPointer getModifiedDateJsonPointer() {
        return modifiedDateJsonPointer;
    }

    public JsonPointer getPublisherIdJsonPointer() {
        return publisherIdJsonPointer;
    }

    public JsonPointer getEntityDescriptionReferenceTypeJsonPointer() {
        return entityDescriptionReferenceTypeJsonPointer;
    }

    public JsonPointer getEntityDescriptionDateJsonPointer() {
        return entityDescriptionDateJsonPointer;
    }

    public JsonPointer getEntityDescriptionDateYearJsonPointer() {
        return entityDescriptionDateYearJsonPointer;
    }

    public JsonPointer getEntityDescriptionDateMonthJsonPointer() {
        return entityDescriptionDateMonthJsonPointer;
    }

    public JsonPointer getEntityDescriptionDateDayJsonPointer() {
        return entityDescritpionDateDayJsonPointer;
    }

    public JsonPointer getMainTitleJsonPointer() {
        return entityDescriptionMainTitle;
    }

    public JsonPointer getTypeJsonPointer() {
        return typeJsonPointer;
    }

    public JsonPointer getDoiJsonPointer() {
        return doiJsonPointer;
    }

    public JsonPointer getContributorsJsonPointer() {
        return contributorsJsonPointer;
    }

    public JsonPointer getContributorsListJsonPointer() {
        return contributorsListJsonPointer;
    }

    public JsonPointer getIdentityArpIdJsonPointer() {
        return identityArpIdJsonPointer;
    }

    public JsonPointer getIdentityOrcIdJsonPointer() {
        return identityOrcIdJsonPointer;
    }

    public JsonPointer getIdentityNameJsonPointer() {
        return identityNameJsonPointer;
    }
}
