package no.unit.nva.publication.storage.model;

public final class DatabaseConstants {

    //TODO: remove when functionality is in place and read table name from Environment
    public static final String RESOURCES_TABLE_NAME = "OrestisResources";

    public static final String DELIMITER = ":";
    public static final String STRING_PLACEHOLDER = "%s";

    public static final String BY_TYPE_CUSTOMER_STATUS_INDEX_NAME = "byTypeCustomerStatus";

    public static final String PRIMARY_KEY_PARTITION_KEY_NAME = "PK0";
    public static final String PRIMARY_KEY_SORT_KEY_NAME = "SK0";
    public static final String BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME = "PK1";
    public static final String BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME = "SK1";

    private static final String OWNER_IDENTIFIER = STRING_PLACEHOLDER;
    private static final String RECORD_TYPE = STRING_PLACEHOLDER;
    private static final String CUSTOMER_IDENTIFIER = STRING_PLACEHOLDER;
    private static final String STATUS = STRING_PLACEHOLDER;
    private static final String ENTRY_IDENTIFIER = STRING_PLACEHOLDER;

    public static final String PRIMARY_KEY_PARTITION_KEY_FORMAT =
        String.join(DELIMITER, RECORD_TYPE, CUSTOMER_IDENTIFIER,OWNER_IDENTIFIER);

    public static final String PRIMARY_KEY_SORT_KEY_FORMAT = String.join(DELIMITER, RECORD_TYPE, ENTRY_IDENTIFIER);

    public static final String BY_TYPE_CUSTOMER_STATUS_PK_FORMAT =
        String.join(DELIMITER, RECORD_TYPE, CUSTOMER_IDENTIFIER, RECORD_TYPE, STATUS);

    public static final String BY_TYPE_CUSTOMER_STATUS_SK_FORMAT =
        String.join(DELIMITER, RECORD_TYPE, ENTRY_IDENTIFIER);


}
