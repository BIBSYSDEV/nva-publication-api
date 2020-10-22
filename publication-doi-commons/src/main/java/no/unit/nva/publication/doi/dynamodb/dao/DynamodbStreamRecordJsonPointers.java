package no.unit.nva.publication.doi.dynamodb.dao;

import com.fasterxml.jackson.core.JsonPointer;

public final class DynamodbStreamRecordJsonPointers {

    public static final JsonPointer IMAGE_IDENTIFIER_POINTER
        = JsonPointer.compile("/dynamodb/newImage/identifier/s");

    public static final JsonPointer PUBLICATION_STATUS_JSON_POINTER = JsonPointer.compile(
        "/dynamodb/newImage/status/s");

    /**
     * Publisher ID aka INSTITUTION_OWNER_ID.
     */
    public static final JsonPointer PUBLISHER_ID = JsonPointer.compile(
        "/dynamodb/newImage/publisherId/s");

    public static final JsonPointer ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE = JsonPointer.compile(
        "/dynamodb/newImage/entityDescription/m/reference/m/publicationInstance/m/type/s");
    public static final JsonPointer PUBLICATION_ENTITY_DESCRIPTION_MAP_POINTER = JsonPointer.compile(
        "/dynamodb/newImage/entityDescription/m");
    public static final JsonPointer ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR = JsonPointer.compile(
        "/dynamodb/newImage/entityDescription/m/date/m/year/s");
    public static final JsonPointer ENTITY_DESCRIPTION_PUBLICATION_DATE_MONTH = JsonPointer.compile(
        "/dynamodb/newImage/entityDescription/m/date/m/month/s");
    public static final JsonPointer ENTITY_DESCRIPTION_PUBLICATION_DATE_DAY = JsonPointer.compile(
        "/dynamodb/newImage/entityDescription/m/date/m/day/s");

    public static final JsonPointer MAIN_TITLE_POINTER = JsonPointer.compile(
        "/dynamodb/newImage/entityDescription/m/mainTitle/s");
    public static final JsonPointer TYPE_POINTER = JsonPointer.compile("/dynamodb/newImage/type/s");

    public static final JsonPointer DOI_POINTER = JsonPointer.compile(
        "/dynamodb/newImage/entityDescription/m/reference/m/doi/s");

    public static final String CONTRIBUTOR_POINTER = "/dynamodb/newImage/entityDescription/m/contributors";
    public static final JsonPointer CONTRIBUTORS_LIST_POINTER = JsonPointer.compile(
        "/dynamodb/newImage/entityDescription/m/contributors/l");
    public static final JsonPointer CONTRIBUTOR_ARP_ID_JSON_POINTER = JsonPointer.compile("/m/identity/m/arpId/s");
    public static final JsonPointer CONTRIBUTOR_ORC_ID_JSON_POINTER = JsonPointer.compile("/m/identity/m/orcId/s");
    public static final JsonPointer CONTRIBUTOR_NAME_JSON_POINTER = JsonPointer.compile("/m/identity/m/name/s");

    public static final String DYNAMODB_TYPE_STRING = "s";
    public static final String DYNAMODB_TYPE_LIST = "l";

    private DynamodbStreamRecordJsonPointers() {
    }
}
