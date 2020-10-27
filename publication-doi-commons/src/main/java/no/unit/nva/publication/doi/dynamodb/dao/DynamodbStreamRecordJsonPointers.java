package no.unit.nva.publication.doi.dynamodb.dao;

import com.fasterxml.jackson.core.JsonPointer;

public class DynamodbStreamRecordJsonPointers {

    public static final String DYNAMODB_OLD_IMAGE_BASE = "/dynamodb/oldImage";
    public static final String DYNAMODB_NEW_IMAGE_BASE = "/dynamodb/newImage";

    private static final String IDENTIFIER = "/identifier/s";

    private static final String STATUS = "/status/s";

    /**
     * Publisher ID aka INSTITUTION_OWNER_ID.
     */
    private static final String PUBLISHER_ID = "/publisherId/s";

    private static final String ENTITY_DESCRIPTION_REFERENCE_TYPE =
        "/entityDescription/m/reference/m/publicationInstance/m/type/s";
    private static final String ENTITY_DESCRIPTION_MAP = "/entityDescription/m";
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

    public static final JsonPointer TYPE_JSON_POINTER = JsonPointer.compile("/type/s");

    public static final String DYNAMODB_TYPE_STRING = "s";
    public static final String DYNAMODB_TYPE_LIST = "l";

    private final String base;

    public DynamodbStreamRecordJsonPointers(String base) {
        this.base = base;
    }

    public JsonPointer getImageIdentifierJsonPointer() {
        return JsonPointer.compile(base + IDENTIFIER);
    }

    public  JsonPointer getPublicationStatusJsonPointer() {
        return JsonPointer.compile(base + STATUS);
    }

    public JsonPointer getPublisherIdJsonPointer() {
        return JsonPointer.compile(base + PUBLISHER_ID);
    }

    public JsonPointer getEntityDescriptionReferenceTypeJsonPointer() {
        return JsonPointer.compile(base + ENTITY_DESCRIPTION_REFERENCE_TYPE);
    }

    public JsonPointer getEntityDescriptionMapJsonPointer() {
        return  JsonPointer.compile(base + ENTITY_DESCRIPTION_MAP);
    }

    public JsonPointer getEntityDescriptionPublicationDateYearJsonPointer() {
        return JsonPointer.compile(base + ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR);
    }

    public JsonPointer getEntityDescriptionPublicationDateMonthJsonPointer() {
        return JsonPointer.compile(base + ENTITY_DESCRIPTION_PUBLICATION_DATE_MONTH);
    }

    public JsonPointer getEntityDescriptionPublicationDateYDayJsonPointer() {
        return JsonPointer.compile(base + ENTITY_DESCRIPTION_PUBLICATION_DATE_DAY);
    }

    public JsonPointer getMainTitleJsonPointer() {
        return JsonPointer.compile(base + ENTITY_DESCRIPTION_MAIN_TITLE);
    }

    public JsonPointer getTypeJsonPointer() {
        return JsonPointer.compile(base + TYPE_POINTER);
    }

    public JsonPointer getDoiJsonPointer() {
        return JsonPointer.compile(base + ENTITY_DESCRIPTION_REFERENCE_DOI);
    }

    public JsonPointer getContributorsJsonPointer() {
        return JsonPointer.compile(base + ENTITY_DESCRIPTION_CONTRIBUTORS);
    }

    public JsonPointer getContributorsListJsonPointer() {
        return JsonPointer.compile(base + ENTITY_DESCRIPTION_CONTRIBUTORS_LIST);
    }

    public JsonPointer getContributorArpIdJsonPointer() {
        return JsonPointer.compile(IDENTITY_ARP_ID);
    }

    public JsonPointer getContributorOrcidJsonPointer() {
        return JsonPointer.compile(IDENTITY_ORC_ID);
    }

    public JsonPointer getContributorNameJsonPointer() {
        return JsonPointer.compile(IDENTITY_NAME);
    }
}
