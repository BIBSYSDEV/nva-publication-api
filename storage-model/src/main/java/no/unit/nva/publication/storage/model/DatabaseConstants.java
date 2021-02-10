package no.unit.nva.publication.storage.model;

public final class DatabaseConstants {

    //TODO: remove when functionality is in place and read table name from Environment
    public static final String RESOURCES_TABLE_NAME = "OrestisResources";

    public static final String KEY_FIELDS_DELIMITER = ":";
    public static final String STRING_PLACEHOLDER = "%s";

    public static final String BY_TYPE_CUSTOMER_STATUS_INDEX_NAME = "byTypeCustomerStatus";
    public static final String BY_CUSTOMER_RESOURCE_INDEX_NAME = "ByCustomerResource";

    public static final String PRIMARY_KEY_PARTITION_KEY_NAME = "PK0";
    public static final String PRIMARY_KEY_SORT_KEY_NAME = "SK0";
    public static final String BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME = "PK1";
    public static final String BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME = "SK1";
    public static final String BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME = "PK2";
    public static final String BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME = "SK2";

    public static final String CUSTOMER_INDEX_FIELD_PREFIX = "Customer";
    public static final String STATUS_INDEX_FIELD_PREFIX = "Status";
    public static final String RESOURCE_INDEX_FIELD_PREFIX = "Resource";

    private static final String OWNER_IDENTIFIER = STRING_PLACEHOLDER;
    private static final String RECORD_TYPE = STRING_PLACEHOLDER;
    private static final String CUSTOMER_IDENTIFIER = STRING_PLACEHOLDER;
    private static final String STATUS = STRING_PLACEHOLDER;
    private static final String ENTRY_IDENTIFIER = STRING_PLACEHOLDER;

    public static final String PRIMARY_KEY_PARTITION_KEY_FORMAT =
        String.join(KEY_FIELDS_DELIMITER, RECORD_TYPE, CUSTOMER_IDENTIFIER, OWNER_IDENTIFIER);

    public static final String PRIMARY_KEY_SORT_KEY_FORMAT = String.join(KEY_FIELDS_DELIMITER, RECORD_TYPE,
        ENTRY_IDENTIFIER);

    public static final String BY_TYPE_CUSTOMER_STATUS_PK_FORMAT =
        String.join(KEY_FIELDS_DELIMITER,
            RECORD_TYPE,
            CUSTOMER_INDEX_FIELD_PREFIX,
            CUSTOMER_IDENTIFIER,
            STATUS_INDEX_FIELD_PREFIX,
            STATUS);

    public static final String BY_TYPE_CUSTOMER_STATUS_SK_FORMAT =
        String.join(KEY_FIELDS_DELIMITER, RECORD_TYPE, ENTRY_IDENTIFIER);
}
