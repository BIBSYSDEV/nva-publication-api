package no.unit.nva.cristin.mapper.nva.exceptions;

public class UnsupportedMainCategoryException extends RuntimeException {
    
    public static final String ERROR_PARSING_MAIN_CATEGORY = "Error parsing main category";
    
    public UnsupportedMainCategoryException() {
        super(ERROR_PARSING_MAIN_CATEGORY);
    }
}
