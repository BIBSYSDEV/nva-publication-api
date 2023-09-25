package no.unit.nva.publication.utils;

import org.junit.jupiter.api.Test;

public class CsvExportGeneratorTest {
    @Test
    void simpleTest() {
        System.out.println();
    }
//
//    private static final String DOUBLE_QUOTE = "\"";
//    private static final String COLUMN_HEADERS = DOUBLE_QUOTE + COLUMN_NAME_URL + DOUBLE_QUOTE
//                                                 + SEPARATOR
//                                                 + DOUBLE_QUOTE + COLUMN_NAME_TITLE + DOUBLE_QUOTE
//                                                 + SEPARATOR
//                                                 + DOUBLE_QUOTE + COLUMN_NAME_CATEGORY + DOUBLE_QUOTE
//                                                 + SEPARATOR
//                                                 + DOUBLE_QUOTE + COLUMN_NAME_PUBLICATION_DATE + DOUBLE_QUOTE
//                                                 + SEPARATOR
//                                                 + DOUBLE_QUOTE + COLUMN_NAME_CONTRIBUTORS + DOUBLE_QUOTE
//                                                 + SEPARATOR
//                                                 + DOUBLE_QUOTE + COLUMN_NAME_CHANNEL_TYPE + DOUBLE_QUOTE
//                                                 + SEPARATOR
//                                                 + DOUBLE_QUOTE + COLUMN_NAME_CHANNEL_IDENTIFIER + DOUBLE_QUOTE
//                                                 + SEPARATOR
//                                                 + DOUBLE_QUOTE + COLUMN_NAME_CHANNEL_NAME + DOUBLE_QUOTE
//                                                 + SEPARATOR
//                                                 + DOUBLE_QUOTE + COLUMN_NAME_CHANNEL_ONLINE_ISSN + DOUBLE_QUOTE
//                                                 + SEPARATOR
//                                                 + DOUBLE_QUOTE + COLUMN_NAME_CHANNEL_PRINT_ISSN + DOUBLE_QUOTE
//                                                 + SEPARATOR
//                                                 + DOUBLE_QUOTE + COLUMN_NAME_CHANNEL_LEVEL + DOUBLE_QUOTE
//                                                 + SEPARATOR
//                                                 + DOUBLE_QUOTE + COLUMN_NAME_FUNDING_SOURCES + DOUBLE_QUOTE;
//
//    @Test
//    void shouldGenerateEmptyCsvIfNoInput() throws IOException {
//        var result = CsvExportGenerator.generate();
//
//        var lines = result.getDataSets().get(ExportDataSet.DATA_SET_PUBLICATIONS).split("\\r\\n");
//
//        assertThat(lines[0], is(equalTo(COLUMN_HEADERS)));
//    }
//
//    @Test
//    void shouldGenerateLineForResourceIfGivenInInput() throws IOException {
//        var input = generateInput();
//        var result = CsvExportGenerator.generate(input);
//
//        System.out.println(result);
//
//        var lines = result.getDataSets().get(ExportDataSet.DATA_SET_PUBLICATIONS).split("\\r\\n");
//        assertThat(lines[0], is(equalTo(COLUMN_HEADERS)));
//        assertThat(lines[1],
//                   is(equalTo("\"https://api.test.nva.aws.unit.no/publication/0188ddb63419-927c6e4c-93da-4396-b629"
//                              + "-96a1e63ff5d9\";\"Future wood demands and ecosystem services trade-offs: A"
//                              + " policy analysis in Norway\";\"AcademicArticle\";\"2023-02-01\";"
//                              + "\"R. Astrup,M. Vergarechea,C. Blattert,N. Forsell,F. Di Fulvio,M. "
//                              + "Hartikainen,K. Øistad,C. Antón-Fernández,D. Burgas,M. Mönkkönen,C. "
//                              + "Fischer,K. Eyvindson,A. Toraño-Caicoya\";\"Journal\";\"433270\";\"Forest Policy and "
//                              + "Economics\";\"1872-7050\";\"1389-9341\";\"1\";")));
//    }
//
//    private JsonNode generateInput() throws JsonProcessingException {
//        return JsonUtils.dtoObjectMapper.readTree(publicationAsJsonString());
//    }
//
//    private String publicationAsJsonString() {
//        return "{\n"
//               + "    \"identifier\" : \"0188ddb63419-927c6e4c-93da-4396-b629-96a1e63ff5d9\",\n"
//               + "    \"nviType\" : \"NviCandidate\",\n"
//               + "    \"modelVersion\" : \"0.20.28\",\n"
//               + "    \"resourceOwner\" : {\n"
//               + "      \"owner\" : \"515051@7677.0.0.0\",\n"
//               + "      \"ownerAffiliation\" : \"https://api.test.nva.aws.unit.no/cristin/organization/7677.0.0.0\"\n"
//               + "    },\n"
//               + "    \"type\" : \"Publication\",\n"
//               + "    \"associatedArtifacts\" : [ {\n"
//               + "      \"identifier\" : \"d02bb399-4b54-461a-97db-277232b21f9d\",\n"
//               + "      \"license\" : \"https://creativecommons.org/licenses/by/4.0\",\n"
//               + "      \"size\" : 4415609,\n"
//               + "      \"publisherAuthority\" : true,\n"
//               + "      \"name\" : \"1-s2.0-S138993412200212X-main.pdf\",\n"
//               + "      \"administrativeAgreement\" : false,\n"
//               + "      \"embargoDate\" : \"2023-06-20T22:00:00Z\",\n"
//               + "      \"mimeType\" : \"application/pdf\",\n"
//               + "      \"publishedDate\" : \"2023-06-21T11:33:58.895032Z\",\n"
//               + "      \"type\" : \"PublishedFile\",\n"
//               + "      \"visibleForNonOwner\" : true\n"
//               + "    } ],\n"
//               + "    \"entityDescription\" : {\n"
//               + "      \"reference\" : {\n"
//               + "        \"type\" : \"Reference\",\n"
//               + "        \"publicationInstance\" : {\n"
//               + "          \"volume\" : \"147\",\n"
//               + "          \"pages\" : {\n"
//               + "            \"end\" : \"102899\",\n"
//               + "            \"type\" : \"Range\",\n"
//               + "            \"begin\" : \"102899\"\n"
//               + "          },\n"
//               + "          \"type\" : \"AcademicArticle\"\n"
//               + "        },\n"
//               + "        \"publicationContext\" : {\n"
//               + "          \"identifier\" : \"433270\",\n"
//               + "          \"publisherId\" : \"https://api.test.nva.aws.unit"
//               + ".no/publication-channels/publisher/18173/2023\",\n"
//               + "          \"website\" : {\n"
//               + "            \"id\" : \"http://www.sciencedirect.com/science/journal/13899341\"\n"
//               + "          },\n"
//               + "          \"level\" : \"1\",\n"
//               + "          \"name\" : \"Forest Policy and Economics\",\n"
//               + "          \"active\" : true,\n"
//               + "          \"npiDomain\" : \"Samfunnsøkonomi\",\n"
//               + "          \"language\" : \"http://lexvo.org/id/iso639-3/und\",\n"
//               + "          \"id\" : \"https://api.test.nva.aws.unit.no/publication-channels/journal/433270/2023\",\n"
//               + "          \"type\" : \"Journal\",\n"
//               + "          \"onlineIssn\" : \"1872-7050\",\n"
//               + "          \"printIssn\" : \"1389-9341\"\n"
//               + "        },\n"
//               + "        \"doi\" : \"https://doi.org/10.1016/j.forpol.2022.102899\"\n"
//               + "      },\n"
//               + "      \"metadataSource\" : \"https://doi.org/10.1016/j.forpol.2022.102899\",\n"
//               + "      \"mainTitle\" : \"Future wood demands and ecosystem services trade-offs: A policy analysis in"
//               + " Norway\",\n"
//               + "      \"alternativeAbstracts\" : { },\n"
//               + "      \"language\" : \"http://lexvo.org/id/iso639-3/eng\",\n"
//               + "      \"abstract\" : \"To mitigate climate change, several European countries have launched "
//               + "policies to promote the development of a renewable resource-based bioeconomy. These bioeconomy "
//               + "strategies plan to use renewable biological resources, which will increase timber and biomass "
//               + "demands and will potentially conflict with multiple other ecosystem services provided by forests. "
//               + "In addition, these forest ecosystem services (FES) are also influenced by other, different, policy "
//               + "strategies, causing a potential mismatch in proposed management solutions for achieving the "
//               + "different policy goals. We evaluated how Norwegian forests can meet the projected wood and biomass "
//               + "demands from the international market for achieving mitigation targets and at the same time meet "
//               + "nationally determined targets for other FES. Using data from the Norwegian national forest "
//               + "inventory (NFI) we simulated the development of Norwegian forests under different management "
//               + "regimes and defined different forest policy scenarios, according to the most relevant forest "
//               + "policies in Norway: national forest policy (NFS), biodiversity policy (BIOS), and bioeconomy policy"
//               + " (BIES). Finally, through multi-objective optimization, we identified the combination of management"
//               + " regimes matching best with each policy scenario. The results for all scenarios indicated that "
//               + "Norway will be able to satisfy wood demands of up to 17 million m3 in 2093. However, the policy "
//               + "objectives for FES under each scenario caused substantial differences in terms of the management "
//               + "regimes selected. We observed that BIES and NFS resulted in very similar forest management programs"
//               + " in Norway, with a dominance of extensive management regimes. In BIOS there was an increase of set "
//               + "aside areas and continuous cover forestry, which made it more compatible with biodiversity "
//               + "indicators. We also found multiple synergies and trade-offs between the FES, likely influenced by "
//               + "the definition of the policy targets at the national scale.\\n\\n\",\n"
//               + "      \"contributors\" : [ {\n"
//               + "        \"sequence\" : 2,\n"
//               + "        \"role\" : {\n"
//               + "          \"type\" : \"Creator\"\n"
//               + "        },\n"
//               + "        \"identity\" : {\n"
//               + "          \"name\" : \"R. Astrup\",\n"
//               + "          \"type\" : \"Identity\"\n"
//               + "        },\n"
//               + "        \"correspondingAuthor\" : false,\n"
//               + "        \"type\" : \"Contributor\"\n"
//               + "      }, {\n"
//               + "        \"sequence\" : 1,\n"
//               + "        \"role\" : {\n"
//               + "          \"type\" : \"Creator\"\n"
//               + "        },\n"
//               + "        \"identity\" : {\n"
//               + "          \"name\" : \"M. Vergarechea\",\n"
//               + "          \"type\" : \"Identity\"\n"
//               + "        },\n"
//               + "        \"correspondingAuthor\" : false,\n"
//               + "        \"type\" : \"Contributor\"\n"
//               + "      }, {\n"
//               + "        \"sequence\" : 5,\n"
//               + "        \"role\" : {\n"
//               + "          \"type\" : \"Creator\"\n"
//               + "        },\n"
//               + "        \"identity\" : {\n"
//               + "          \"name\" : \"C. Blattert\",\n"
//               + "          \"type\" : \"Identity\"\n"
//               + "        },\n"
//               + "        \"correspondingAuthor\" : false,\n"
//               + "        \"type\" : \"Contributor\"\n"
//               + "      }, {\n"
//               + "        \"sequence\" : 9,\n"
//               + "        \"role\" : {\n"
//               + "          \"type\" : \"Creator\"\n"
//               + "        },\n"
//               + "        \"identity\" : {\n"
//               + "          \"name\" : \"N. Forsell\",\n"
//               + "          \"type\" : \"Identity\"\n"
//               + "        },\n"
//               + "        \"correspondingAuthor\" : false,\n"
//               + "        \"type\" : \"Contributor\"\n"
//               + "      }, {\n"
//               + "        \"sequence\" : 8,\n"
//               + "        \"role\" : {\n"
//               + "          \"type\" : \"Creator\"\n"
//               + "        },\n"
//               + "        \"identity\" : {\n"
//               + "          \"name\" : \"F. Di Fulvio\",\n"
//               + "          \"type\" : \"Identity\"\n"
//               + "        },\n"
//               + "        \"correspondingAuthor\" : false,\n"
//               + "        \"type\" : \"Contributor\"\n"
//               + "      }, {\n"
//               + "        \"sequence\" : 6,\n"
//               + "        \"role\" : {\n"
//               + "          \"type\" : \"Creator\"\n"
//               + "        },\n"
//               + "        \"identity\" : {\n"
//               + "          \"name\" : \"M. Hartikainen\",\n"
//               + "          \"type\" : \"Identity\"\n"
//               + "        },\n"
//               + "        \"correspondingAuthor\" : false,\n"
//               + "        \"type\" : \"Contributor\"\n"
//               + "      }, {\n"
//               + "        \"sequence\" : 4,\n"
//               + "        \"role\" : {\n"
//               + "          \"type\" : \"Creator\"\n"
//               + "        },\n"
//               + "        \"identity\" : {\n"
//               + "          \"name\" : \"K. Øistad\",\n"
//               + "          \"type\" : \"Identity\"\n"
//               + "        },\n"
//               + "        \"correspondingAuthor\" : false,\n"
//               + "        \"type\" : \"Contributor\"\n"
//               + "      }, {\n"
//               + "        \"sequence\" : 13,\n"
//               + "        \"role\" : {\n"
//               + "          \"type\" : \"Creator\"\n"
//               + "        },\n"
//               + "        \"identity\" : {\n"
//               + "          \"name\" : \"C. Antón-Fernández\",\n"
//               + "          \"type\" : \"Identity\"\n"
//               + "        },\n"
//               + "        \"correspondingAuthor\" : false,\n"
//               + "        \"type\" : \"Contributor\"\n"
//               + "      }, {\n"
//               + "        \"sequence\" : 10,\n"
//               + "        \"role\" : {\n"
//               + "          \"type\" : \"Creator\"\n"
//               + "        },\n"
//               + "        \"identity\" : {\n"
//               + "          \"name\" : \"D. Burgas\",\n"
//               + "          \"type\" : \"Identity\"\n"
//               + "        },\n"
//               + "        \"correspondingAuthor\" : false,\n"
//               + "        \"type\" : \"Contributor\"\n"
//               + "      }, {\n"
//               + "        \"sequence\" : 12,\n"
//               + "        \"role\" : {\n"
//               + "          \"type\" : \"Creator\"\n"
//               + "        },\n"
//               + "        \"identity\" : {\n"
//               + "          \"name\" : \"M. Mönkkönen\",\n"
//               + "          \"type\" : \"Identity\"\n"
//               + "        },\n"
//               + "        \"correspondingAuthor\" : false,\n"
//               + "        \"type\" : \"Contributor\"\n"
//               + "      }, {\n"
//               + "        \"sequence\" : 3,\n"
//               + "        \"role\" : {\n"
//               + "          \"type\" : \"Creator\"\n"
//               + "        },\n"
//               + "        \"identity\" : {\n"
//               + "          \"name\" : \"C. Fischer\",\n"
//               + "          \"type\" : \"Identity\"\n"
//               + "        },\n"
//               + "        \"correspondingAuthor\" : false,\n"
//               + "        \"type\" : \"Contributor\"\n"
//               + "      }, {\n"
//               + "        \"sequence\" : 7,\n"
//               + "        \"role\" : {\n"
//               + "          \"type\" : \"Creator\"\n"
//               + "        },\n"
//               + "        \"identity\" : {\n"
//               + "          \"name\" : \"K. Eyvindson\",\n"
//               + "          \"type\" : \"Identity\"\n"
//               + "        },\n"
//               + "        \"correspondingAuthor\" : false,\n"
//               + "        \"type\" : \"Contributor\"\n"
//               + "      }, {\n"
//               + "        \"sequence\" : 11,\n"
//               + "        \"role\" : {\n"
//               + "          \"type\" : \"Creator\"\n"
//               + "        },\n"
//               + "        \"identity\" : {\n"
//               + "          \"name\" : \"A. Toraño-Caicoya\",\n"
//               + "          \"type\" : \"Identity\"\n"
//               + "        },\n"
//               + "        \"correspondingAuthor\" : false,\n"
//               + "        \"type\" : \"Contributor\"\n"
//               + "      } ],\n"
//               + "      \"type\" : \"EntityDescription\",\n"
//               + "      \"publicationDate\" : {\n"
//               + "        \"month\" : \"2\",\n"
//               + "        \"year\" : \"2023\",\n"
//               + "        \"type\" : \"PublicationDate\",\n"
//               + "        \"day\" : \"1\"\n"
//               + "      },\n"
//               + "      \"tags\" : [ \"Economics and Econometrics\", \"Management, Monitoring, Policy and Law\", "
//               + "\"Forestry\", \"Sociology and Political Science\" ]\n"
//               + "    },\n"
//               + "    \"createdDate\" : \"2023-06-21T11:28:05.657262Z\",\n"
//               + "    \"publicationContextUris\" : [ \"https://api.test.nva.aws.unit"
//               + ".no/publication-channels/journal/433270/2023\" ],\n"
//               + "    \"modifiedDate\" : \"2023-06-21T11:33:59.174105Z\",\n"
//               + "    \"publisher\" : {\n"
//               + "      \"id\" : \"https://api.test.nva.aws.unit.no/customer/cd925eea-7f4c-4167-9b7c-a7bf7f8eca59\",\n"
//               + "      \"type\" : \"Organization\"\n"
//               + "    },\n"
//               + "    \"id\" : \"https://api.test.nva.aws.unit"
//               + ".no/publication/0188ddb63419-927c6e4c-93da-4396-b629-96a1e63ff5d9\",\n"
//               + "    \"publishedDate\" : \"2023-06-21T11:33:59.155377Z\",\n"
//               + "    \"status\" : \"PUBLISHED\"\n"
//               + "}";
//    }
}
