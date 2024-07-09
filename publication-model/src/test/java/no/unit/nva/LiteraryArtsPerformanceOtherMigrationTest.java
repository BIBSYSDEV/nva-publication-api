package no.unit.nva;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.artistic.literaryarts.LiteraryArts;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsManifestation;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsPerformance;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsPerformanceSubtype;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

public class LiteraryArtsPerformanceOtherMigrationTest {

    public static final String OTHER_STRING = "\"Other\"";
    public static final String READING = "\"Reading\"";
    public static final String OTHER_OBJECT = "{ \"type\": \"Other\" }";
    public static final String OTHER_OBJECT_WITH_DESCRIPTION = "{ \"type\": \"Other\", "
            + "\"description\": \"Some description\"}";
    public static final String READING_OBJECT = "{ \"type\": \"Reading\" }";

    public static Stream<Named<String>> literaryArtsPerformanceProvider() {
        return Stream.of(otherString(),
                nonOtherString(),
                otherObject(),
                otherObjectWithDescription(),
                nonOtherObject());
    }

    private static Named<String> nonOtherObject() {
        return Named.of("Non-other object", publication(READING_OBJECT));
    }

    private static Named<String> otherObjectWithDescription() {
        return Named.of("Other object with description", publication(OTHER_OBJECT_WITH_DESCRIPTION));
    }

    private static Named<String> otherObject() {
        return Named.of("Other as object", publication(OTHER_OBJECT));
    }

    private static Named<String> nonOtherString() {
        return Named.of("Non-Other as simple string", publication(READING));
    }

    private static Named<String> otherString() {
        return Named.of("Other as simple string", publication(OTHER_STRING));
    }

    private static String publication(String other) {
        return "{\n"
                + "  \"type\" : \"Publication\",\n"
                + "  \"identifier\" : \"c443030e-9d56-43d8-afd1-8c89105af555\",\n"
                + "  \"status\" : \"NEW\",\n"
                + "  \"resourceOwner\" : {\n"
                + "    \"owner\" : \"5hvGFdljAVTx\",\n"
                + "    \"ownerAffiliation\" : \"https://www.example.org/3c7ea15a-2467-487f-8f52-2160696eb643\"\n"
                + "  },\n"
                + "  \"publisher\" : {\n"
                + "    \"type\" : \"Organization\",\n"
                + "    \"id\" : \"https://www.example.org/56c929ee-b73b-48f1-a1af-16cecfb1e381\",\n"
                + "    \"labels\" : {\n"
                + "      \"af\" : \"QphbIid4RuhTiUI4\"\n"
                + "    }\n"
                + "  },\n"
                + "  \"createdDate\" : \"2017-07-16T15:48:16.130Z\",\n"
                + "  \"modifiedDate\" : \"2019-10-15T23:51:53.071Z\",\n"
                + "  \"publishedDate\" : \"1997-01-23T03:35:54.549Z\",\n"
                + "  \"indexedDate\" : \"2011-08-09T06:35:22.415Z\",\n"
                + "  \"handle\" : \"https://www.example.org/8582e891-eafe-45b0-a7b1-67f72f7d7534\",\n"
                + "  \"doi\" : \"https://doi.org/10.1234/in\",\n"
                + "  \"link\" : \"https://www.example.org/90a115dd-de77-4a7e-b591-50768fe35423\",\n"
                + "  \"entityDescription\" : {\n"
                + "    \"type\" : \"EntityDescription\",\n"
                + "    \"mainTitle\" : \"uGgNB2zWHecouN2j\",\n"
                + "    \"alternativeTitles\" : {\n"
                + "      \"zh\" : \"zyZZFqBy8o\"\n"
                + "    },\n"
                + "    \"language\" : \"http://lexvo.org/id/iso639-3/und\",\n"
                + "    \"publicationDate\" : {\n"
                + "      \"type\" : \"PublicationDate\",\n"
                + "      \"year\" : \"ugZWbdQeDX8g2U\",\n"
                + "      \"month\" : \"3mnHFvInVz4Sf4uI2\",\n"
                + "      \"day\" : \"1cpDl54iqYzXnYiX\"\n"
                + "    },\n"
                + "    \"contributors\" : [ {\n"
                + "      \"type\" : \"Contributor\",\n"
                + "      \"identity\" : {\n"
                + "        \"type\" : \"Identity\",\n"
                + "        \"id\" : \"https://www.example.org/6a87bfa8-6ac8-458a-a5c0-28bb83719a2f\",\n"
                + "        \"name\" : \"hKKY0jQs8xLpd42h\",\n"
                + "        \"nameType\" : \"Personal\",\n"
                + "        \"orcId\" : \"NUBa4MIofAXnbxfkUi\",\n"
                + "        \"verificationStatus\" : \"CannotBeEstablished\"\n"
                + "      },\n"
                + "      \"affiliations\" : [ {\n"
                + "        \"type\" : \"Organization\",\n"
                + "        \"id\" : \"https://www.example.org/2ba05e1b-1531-46e0-9fa7-99c138c63f25\",\n"
                + "        \"labels\" : {\n"
                + "          \"nl\" : \"zWvJe1LVqdyfO\"\n"
                + "        }\n"
                + "      } ],\n"
                + "      \"role\" : {\n"
                + "        \"type\" : \"Researcher\"\n"
                + "      },\n"
                + "      \"sequence\" : 7,\n"
                + "      \"correspondingAuthor\" : false\n"
                + "    }, {\n"
                + "      \"type\" : \"Contributor\",\n"
                + "      \"identity\" : {\n"
                + "        \"type\" : \"Identity\",\n"
                + "        \"id\" : \"https://www.example.org/2b6613ca-966f-495e-86c8-3733edd13b6c\",\n"
                + "        \"name\" : \"FIpBdti7zUqw\",\n"
                + "        \"nameType\" : \"Organizational\",\n"
                + "        \"orcId\" : \"oeQLDiV7tdKuwLgzn\",\n"
                + "        \"verificationStatus\" : \"NotVerified\"\n"
                + "      },\n"
                + "      \"affiliations\" : [ {\n"
                + "        \"type\" : \"Organization\",\n"
                + "        \"id\" : \"https://www.example.org/f66ddc92-c561-4bb6-b9e6-f60e3993e01f\",\n"
                + "        \"labels\" : {\n"
                + "          \"hu\" : \"7mdg5cY6zUDYqAS\"\n"
                + "        }\n"
                + "      } ],\n"
                + "      \"role\" : {\n"
                + "        \"type\" : \"ResearchGroup\"\n"
                + "      },\n"
                + "      \"sequence\" : 0,\n"
                + "      \"correspondingAuthor\" : false\n"
                + "    } ],\n"
                + "    \"alternativeAbstracts\" : {\n"
                + "      \"de\" : \"6evJ70kOn7zJITjGwa\"\n"
                + "    },\n"
                + "    \"npiSubjectHeading\" : \"V35lhZNwag\",\n"
                + "    \"tags\" : [ \"NNtvkXZwVRa\" ],\n"
                + "    \"description\" : \"zNDXr9KxyydXSad0Wl\",\n"
                + "    \"reference\" : {\n"
                + "      \"type\" : \"Reference\",\n"
                + "      \"publicationContext\" : {\n"
                + "        \"type\" : \"Artistic\"\n"
                + "      },\n"
                + "      \"doi\" : \"https://www.example.org/4adcddb7-333f-48de-b01e-0fb4c1c827e0\",\n"
                + "      \"publicationInstance\" : {\n"
                + "        \"type\" : \"LiteraryArts\",\n"
                + "        \"subtype\" : {\n"
                + "          \"type\" : \"Poetry\"\n"
                + "        },\n"
                + "        \"manifestations\" : [ {\n"
                + "          \"type\" : \"LiteraryArtsMonograph\",\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"X8VTN8uSJDlKE4\"\n"
                + "          },\n"
                + "          \"publicationDate\" : {\n"
                + "            \"type\" : \"PublicationDate\",\n"
                + "            \"year\" : \"x5l0KxGhsYoQbwsJcyv\",\n"
                + "            \"month\" : \"3kGfkW4SaU8kVf\",\n"
                + "            \"day\" : \"zZojthqcyBycDtr9\"\n"
                + "          },\n"
                + "          \"isbnList\" : [ \"9780099470434\" ],\n"
                + "          \"pages\" : {\n"
                + "            \"type\" : \"MonographPages\",\n"
                + "            \"introduction\" : {\n"
                + "              \"type\" : \"Range\",\n"
                + "              \"begin\" : \"p1eK9FjAek2\",\n"
                + "              \"end\" : \"PBqNnyYD2ymPiiKLH4\"\n"
                + "            },\n"
                + "            \"pages\" : \"hzj0x0MTvEXHK\",\n"
                + "            \"illustrated\" : false\n"
                + "          }\n"
                + "        }, {\n"
                + "          \"type\" : \"LiteraryArtsAudioVisual\",\n"
                + "          \"subtype\" : {\n"
                + "            \"type\" : \"Podcast\"\n"
                + "          },\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"BFHfUUXCfold9q1fSB\"\n"
                + "          },\n"
                + "          \"publicationDate\" : {\n"
                + "            \"type\" : \"PublicationDate\",\n"
                + "            \"year\" : \"dcUqqnh2Hm\",\n"
                + "            \"month\" : \"RYCWMhxAWnLdynqV0\",\n"
                + "            \"day\" : \"f2ByGgYfk11XHwcs\"\n"
                + "          },\n"
                + "          \"isbnList\" : [ \"9780099470434\" ],\n"
                + "          \"extent\" : 825972264\n"
                + "        }, {\n"
                + "          \"type\" : \"LiteraryArtsPerformance\",\n"
                + "          \"subtype\" : " + other + ",\n"
                + "          \"place\" : {\n"
                + "            \"type\" : \"UnconfirmedPlace\",\n"
                + "            \"label\" : \"0ePRyMkSvUbDSIj4STT\",\n"
                + "            \"country\" : \"5w7RlMQDxVWewfD\"\n"
                + "          },\n"
                + "          \"publicationDate\" : {\n"
                + "            \"type\" : \"PublicationDate\",\n"
                + "            \"year\" : \"yqA6qcXj9oqPzG\",\n"
                + "            \"month\" : \"RLuDaKrSrOr848BLw\",\n"
                + "            \"day\" : \"g0Cb25Hxa1jFDF7Q8sd\"\n"
                + "          }\n"
                + "        }, {\n"
                + "          \"type\" : \"LiteraryArtsWeb\",\n"
                + "          \"id\" : \"https://www.example.com/E8RKQoV7yFfXyU\",\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"z4rvyCLw9Oz\"\n"
                + "          },\n"
                + "          \"publicationDate\" : {\n"
                + "            \"type\" : \"PublicationDate\",\n"
                + "            \"year\" : \"Z6elc7nTVje9ntNA1h\",\n"
                + "            \"month\" : \"Hg5LNDk177lCvYnR\",\n"
                + "            \"day\" : \"93XCBbnxwkS0\"\n"
                + "          }\n"
                + "        } ],\n"
                + "        \"description\" : \"pTceis8Zx23opE\",\n"
                + "        \"pages\" : {\n"
                + "          \"type\" : \"NullPages\"\n"
                + "        }\n"
                + "      }\n"
                + "    },\n"
                + "    \"metadataSource\" : \"https://www.example.org/4043f9c2-e9ce-47e9-a118-f0911dacdfb3\",\n"
                + "    \"abstract\" : \"rpk2PovouE6\"\n"
                + "  },\n"
                + "  \"projects\" : [ {\n"
                + "    \"type\" : \"ResearchProject\",\n"
                + "    \"id\" : \"https://www.example.org/3517b399-f6d1-4ec0-a970-76280126af43\",\n"
                + "    \"name\" : \"ibXgesrJfX\",\n"
                + "    \"approvals\" : [ {\n"
                + "      \"type\" : \"Approval\",\n"
                + "      \"approvalDate\" : \"2008-11-15T08:19:52.685Z\",\n"
                + "      \"approvedBy\" : \"DIRHEALTH\",\n"
                + "      \"approvalStatus\" : \"APPLIED\",\n"
                + "      \"applicationCode\" : \"tbQrfWiZKokxT0xbH\"\n"
                + "    } ]\n"
                + "  } ],\n"
                + "  \"fundings\" : [ {\n"
                + "    \"type\" : \"UnconfirmedFunding\",\n"
                + "    \"source\" : \"https://www.example.org/b283bf14-fc21-42c5-91b2-29061c1a49e6\",\n"
                + "    \"identifier\" : \"VrLdB244HjmBmZ2\",\n"
                + "    \"labels\" : {\n"
                + "      \"ca\" : \"RA1qo2RaDatLeszjN\"\n"
                + "    },\n"
                + "    \"fundingAmount\" : {\n"
                + "      \"currency\" : \"USD\",\n"
                + "      \"amount\" : 452094139\n"
                + "    },\n"
                + "    \"activeFrom\" : \"2003-04-04T17:08:32.847Z\",\n"
                + "    \"activeTo\" : \"2008-07-28T19:43:03.928Z\"\n"
                + "  }, {\n"
                + "    \"type\" : \"ConfirmedFunding\",\n"
                + "    \"source\" : \"https://www.example.org/474a1c34-b8c9-46ae-b7ae-9064f5420fdf\",\n"
                + "    \"id\" : \"https://www.example.org/ccfc5b63-5eaa-47ab-b291-84f6472920ad\",\n"
                + "    \"identifier\" : \"CFK5TBb9b6eL\",\n"
                + "    \"labels\" : {\n"
                + "      \"bg\" : \"ltAQCDDZgjA\"\n"
                + "    },\n"
                + "    \"fundingAmount\" : {\n"
                + "      \"currency\" : \"NOK\",\n"
                + "      \"amount\" : 1652746680\n"
                + "    },\n"
                + "    \"activeFrom\" : \"1983-01-11T13:22:34.777Z\",\n"
                + "    \"activeTo\" : \"2013-04-02T06:02:06.173Z\"\n"
                + "  } ],\n"
                + "  \"additionalIdentifiers\" : [ {\n"
                + "    \"type\" : \"AdditionalIdentifier\",\n"
                + "    \"source\" : \"fakesource\",\n"
                + "    \"value\" : \"1234\"\n"
                + "  } ],\n"
                + "  \"subjects\" : [ \"https://www.example.org/0df586c6-3086-4cad-aafc-e6e1391b364d\" ],\n"
                + "  \"associatedArtifacts\" : [ {\n"
                + "    \"type\" : \"PublishedFile\",\n"
                + "    \"identifier\" : \"52d2222a-9ab4-408b-b056-df1a7325230b\",\n"
                + "    \"name\" : \"HrHN71LLmat5D\",\n"
                + "    \"mimeType\" : \"jq9C6S9OtTGE\",\n"
                + "    \"size\" : 1929723651,\n"
                + "    \"license\" : \"https://www.example.com/x5mztfbbc91ji\",\n"
                + "    \"administrativeAgreement\" : false,\n"
                + "    \"publisherAuthority\" : true,\n"
                + "    \"publishedDate\" : \"2009-06-08T08:47:16.700Z\",\n"
                + "    \"visibleForNonOwner\" : true\n"
                + "  }, {\n"
                + "    \"type\" : \"AssociatedLink\",\n"
                + "    \"id\" : \"https://www.example.com/5oey5g9ccYghHjFpVn\",\n"
                + "    \"name\" : \"uwTpWzzdvtYphxymQbK\",\n"
                + "    \"description\" : \"jmJXV1NESS7Sr3h\"\n"
                + "  } ],\n"
                + "  \"rightsHolder\" : \"sWfQAsw214oPeSDvY4N\",\n"
                + "  \"modelVersion\" : \"0.20.25\"\n"
                + "}";
    }

    @ParameterizedTest
    @MethodSource("literaryArtsPerformanceProvider")
    void shouldMigrateKnowOtherTypes(String json) throws JsonProcessingException {
        var publication = JsonUtils.dtoObjectMapper.readValue(json, Publication.class);
        var subtype = extractLiteraryArtsPerformance(publication);
        assertThat(subtype.getType(), is(not(nullValue())));
    }

    private LiteraryArtsPerformanceSubtype extractLiteraryArtsPerformance(Publication publication) {
        var publicationInstance = publication.getEntityDescription().getReference().getPublicationInstance();
        return ((LiteraryArts) publicationInstance).getManifestations().stream()
                .filter(LiteraryArtsPerformanceOtherMigrationTest::isLiteraryArtsPerformance)
                .findFirst()
                .map(LiteraryArtsPerformance.class::cast)
                .map(LiteraryArtsPerformance::getSubtype)
                .orElse(null);
    }

    private static boolean isLiteraryArtsPerformance(LiteraryArtsManifestation i) {
        return LiteraryArtsPerformance.class.isAssignableFrom(i.getClass());
    }
}
