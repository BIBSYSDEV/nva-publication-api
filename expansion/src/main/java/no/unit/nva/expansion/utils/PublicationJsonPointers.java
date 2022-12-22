package no.unit.nva.expansion.utils;

public final class PublicationJsonPointers {
    
    public static final String CONTEXT_TYPE_JSON_PTR = "/entityDescription/reference/publicationContext/type";
    public static final String ID_JSON_PTR = "/id";
    public static final String IDENTIFIER_JSON_PTR = "/identifier";
    public static final String PUBLISHER_ID_JSON_PTR = "/entityDescription/reference/publicationContext/publisher/id";
    public static final String JOURNAL_ID_JSON_PTR = "/entityDescription/reference/publicationContext/id";
    public static final String SERIES_ID_JSON_PTR = "/entityDescription/reference/publicationContext/series/id";
    public static final String CONTRIBUTORS_POINTER = "/entityDescription/contributors";
    public static final String AFFILIATIONS_POINTER = "/affiliations";

    private PublicationJsonPointers() {
    
    }
}
