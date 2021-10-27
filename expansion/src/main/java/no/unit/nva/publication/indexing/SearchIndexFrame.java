package no.unit.nva.publication.indexing;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class SearchIndexFrame {

    //For some unknown reason the frame was not being picked up by some lambda functions.
    //TODO: After making indexing work, move this to a resource file.
    public static final String FRAME_SRC = "{\n"
                                           + "  \"@context\": {\n"
                                           + "    \"@vocab\":  \"https://bibsysdev.github.io/src/nva/ontology.ttl#\",\n"
                                           + "    \"id\": \"@id\",\n"
                                           + "    \"type\": \"@type\",\n"
                                           + "    \"publicationContext\": {\n"
                                           + "      \"@embed\": \"@always\"\n"
                                           + "    },\n"
                                           + "    \"contributors\": {\n"
                                           + "      \"@container\": \"@set\"\n"
                                           + "    },\n"
                                           + "    \"additionalIdentifiers\": {\n"
                                           + "      \"@container\": \"@set\"\n"
                                           + "    },\n"
                                           + "    \"affiliations\": {\n"
                                           + "      \"@container\": \"@set\"\n"
                                           + "    },\n"
                                           + "    \"subjects\": {\n"
                                           + "      \"@container\": \"@set\"\n"
                                           + "    },\n"
                                           + "    \"projects\": {\n"
                                           + "      \"@container\": \"@set\"\n"
                                           + "    },\n"
                                           + "    \"tags\": {\n"
                                           + "      \"@container\": \"@set\"\n"
                                           + "    },\n"
                                           + "    \"isbnList\": {\n"
                                           + "      \"@container\": \"@set\"\n"
                                           + "    },\n"
                                           + "    \"venues\": {\n"
                                           + "      \"@container\": \"@set\"\n"
                                           + "    },\n"
                                           + "    \"files\": {\n"
                                           + "      \"@container\": \"@set\"\n"
                                           + "    },\n"
                                           + "    \"grants\": {\n"
                                           + "      \"@container\": \"@set\"\n"
                                           + "    },\n"
                                           + "    \"approvals\": {\n"
                                           + "      \"@container\": \"@set\"\n"
                                           + "    }\n"
                                           + "  },\n"
                                           + "  \"@type\": \"Publication\",\n"
                                           + "  \"entityDescription\": {\n"
                                           + "    \"reference\": {\n"
                                           + "      \"publicationContext\": {\n"
                                           + "      }\n"
                                           + "    }\n"
                                           + "  }\n"
                                           + "}\n";

    private SearchIndexFrame() {
    }

}
