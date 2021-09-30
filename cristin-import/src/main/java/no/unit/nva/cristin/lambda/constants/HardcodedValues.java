package no.unit.nva.cristin.lambda.constants;

import java.net.URI;

public final class HardcodedValues {

    //COMMON
    public static final URI HARDCODED_NVA_CUSTOMER =
        URI.create("https://api.dev.nva.aws.unit.no/customer/f54c8aa9-073a-46a1-8f7c-dde66c853934");
    public static final String HARDCODED_PUBLICATIONS_OWNER = "someone@unit.no";
    public static final URI HARDCODED_SAMPLE_DOI = URI.create("https://doi.org/10.1145/1132956.1132959");

    //BOOK
    public static final boolean HARDCODED_BOOK_PEER_REVIEWED = false;

    //RESEARCH_PROJECT
    public static final String HARDCODED_RESEARCH_PROJECT_NAME = "Some project name";


    private HardcodedValues() {

    }
}
