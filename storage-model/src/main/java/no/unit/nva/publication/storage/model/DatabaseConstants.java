package no.unit.nva.publication.storage.model;

import nva.commons.core.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DatabaseConstants {

    public static final String ENVIRONMENT_VARIABLE_NOT_SET_WARNING =
        "Environment variable not set: {}. Defaulting to {}";
    public static final Logger logger = LoggerFactory.getLogger(DatabaseConstants.class);
    public static final Environment environment = new Environment();
    public static final String RESOURCES_TABLE_NAME_ENV_VARIABLE = "TABLE_NAME";
    public static final String DEFAULT_RESOURCES_TABLE_NAME = "NonExistingTable";
    public static final String RESOURCES_TABLE_NAME = readTableNameFromEnvironment();

    public static final String KEY_FIELDS_DELIMITER = ":";
    public static final String STRING_PLACEHOLDER = "%s";
    public static final String BY_TYPE_CUSTOMER_STATUS_INDEX_NAME = "ByTypeCustomerStatus";
    public static final String BY_CUSTOMER_RESOURCE_INDEX_NAME = "ByCustomerResource";
    public static final String RESOURCES_BY_IDENTIFIER_INDEX_NAME = "ResourcesByIdentifier";
    public static final String PRIMARY_KEY_PARTITION_KEY_NAME = "PK0";
    public static final String PRIMARY_KEY_SORT_KEY_NAME = "SK0";
    public static final String BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME = "PK1";
    public static final String BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME = "SK1";
    public static final String BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME = "PK2";
    public static final String BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME = "SK2";
    public static final String RESOURCES_BY_IDENTIFIER_INDEX_PARTITION_KEY_NAME = "PK3";
    public static final String RESOURCES_BY_IDENTIFIER_INDEX_SORT_KEY_NAME = "SK3";
    public static final String CUSTOMER_INDEX_FIELD_PREFIX = "Customer";
    public static final String STATUS_INDEX_FIELD_PREFIX = "Status";
    public static final String RESOURCE_INDEX_FIELD_PREFIX = "Resource";
    private static final String OWNER_IDENTIFIER = STRING_PLACEHOLDER;
    private static final String RECORD_TYPE = STRING_PLACEHOLDER;
    private static final String CUSTOMER_IDENTIFIER = STRING_PLACEHOLDER;
    public static final String PRIMARY_KEY_PARTITION_KEY_FORMAT =
        String.join(KEY_FIELDS_DELIMITER, RECORD_TYPE, CUSTOMER_IDENTIFIER, OWNER_IDENTIFIER);

    private static final String STATUS = STRING_PLACEHOLDER;

    public static final String BY_TYPE_CUSTOMER_STATUS_PK_FORMAT =
        //Do not refactor to method, declaration order of static variables is important.
        String.join(KEY_FIELDS_DELIMITER,
            RECORD_TYPE,
            CUSTOMER_INDEX_FIELD_PREFIX,
            CUSTOMER_IDENTIFIER,
            STATUS_INDEX_FIELD_PREFIX,
            STATUS);
    private static final String ENTRY_IDENTIFIER = STRING_PLACEHOLDER;

    public static final String PRIMARY_KEY_SORT_KEY_FORMAT =
        String.join(KEY_FIELDS_DELIMITER, RECORD_TYPE, ENTRY_IDENTIFIER);

    public static final String BY_TYPE_CUSTOMER_STATUS_SK_FORMAT =
        String.join(KEY_FIELDS_DELIMITER, RECORD_TYPE, ENTRY_IDENTIFIER);

    private DatabaseConstants() {
    }

    private static String readTableNameFromEnvironment() {
        try {
            return environment.readEnv(RESOURCES_TABLE_NAME_ENV_VARIABLE);
        } catch (Exception e) {
            return defaultValue();
        }
    }

    private static String defaultValue() {
        logWarning();
        return DEFAULT_RESOURCES_TABLE_NAME;
    }

    private static void logWarning() {
        logger.warn(ENVIRONMENT_VARIABLE_NOT_SET_WARNING,
            RESOURCES_TABLE_NAME_ENV_VARIABLE,
            DEFAULT_RESOURCES_TABLE_NAME
        );
    }
}
