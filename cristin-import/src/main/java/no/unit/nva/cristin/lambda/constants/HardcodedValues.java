package no.unit.nva.cristin.lambda.constants;

import java.net.URI;
import no.unit.nva.model.Level;

public final class HardcodedValues {

    public static final String HARDCODED_BOOK_PUBLISHER = "SomePublisher";
    public static final Level HARDCODED_LEVEL = Level.LEVEL_0;
    public static final String HARDCODED_PAGE = "1";

    public static final URI HARDCODED_URI = URI.create("https://www.example.com/");
    public static final String HARDCODED_NPI_SUBJECT = "1007";
    public static final URI HARDCODED_NVA_CUSTOMER =
        URI.create("https://api.dev.nva.aws.unit.no/customer/f54c8aa9-073a-46a1-8f7c-dde66c853934");

    public static final URI HARDCODED_SAMPLE_DOI = URI.create("https://doi.org/10.1145/1132956.1132959");

    public static final boolean HARDCODED_ILLUSTRATED = false;
    public static final boolean HARDCODED_PEER_REVIEWED = false;
    public static final boolean HARDCODED_TEXTBOOK_CONTENT = false;

    private HardcodedValues() {

    }
}
