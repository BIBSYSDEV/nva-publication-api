package no.unit.nva.model.instancetypes.artistic.music;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.stream.Stream;

class IsrcTest {

    public static final String VALID_ISRC = "USRC17607839";

    public static Stream<String> nullIsrcPublicationProvider() {
        return Stream.of(
                isrcPublicationWithoutIsrc(),
                isrcPublicationWithNullIsrc()
        );
    }

    @Test
    void shouldAcceptValidIsrc() {
        assertDoesNotThrow(() -> new Isrc(VALID_ISRC));
    }

    @ParameterizedTest
    @NullSource
    void shouldReturnNullValueWhenNullValueIsProvided(String value) throws InvalidIsrcException {
        var isrc = new Isrc(value);
        assertThat(isrc.getValue(), is(nullValue()));
    }

    @Test
    void shouldThrowExceptionWhenIsrcIsInvalid() {
        var invalidIsrc = randomString();
        var exception = assertThrows(InvalidIsrcException.class, () -> new Isrc(invalidIsrc));
        assertThat(exception.getMessage(), is(equalTo(InvalidIsrcException.formatErrorMessage(invalidIsrc))));
    }

    @Test
    void shouldSerializeAsTypedObject() throws InvalidIsrcException, JsonProcessingException {
        var isrc = new Isrc(VALID_ISRC);
        var actualJsonString = JsonUtils.dtoObjectMapper.writeValueAsString(isrc);
        var actualJson = JsonUtils.dtoObjectMapper.readTree(actualJsonString);
        assertThat(actualJson, is(equalTo(createExpectedJson(isrc))));
    }

    @ParameterizedTest
    @MethodSource("nullIsrcPublicationProvider")
    void shouldNotThrowWhenDataHasNullIsrc(String candidate) {
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(candidate, Publication.class));
    }

    private ObjectNode createExpectedJson(Isrc isrc) {
        var expectedJson = JsonUtils.dtoObjectMapper.createObjectNode();
        expectedJson.put("type", Isrc.class.getSimpleName());
        expectedJson.put("value", isrc.getValue());
        return expectedJson;
    }

    private static String isrcPublicationWithNullIsrc() {
        return "{\n"
                + "  \"type\" : \"Publication\",\n"
                + "  \"identifier\" : \"c443030e-9d56-43d8-afd1-8c89105af555\",\n"
                + "  \"status\" : \"NEW\",\n"
                + "  \"resourceOwner\" : {\n"
                + "    \"owner\" : \"RwWXJcCDJnQP7ou\",\n"
                + "    \"ownerAffiliation\" : \"https://www.example.org/utunde\"\n"
                + "  },\n"
                + "  \"publisher\" : {\n"
                + "    \"type\" : \"Organization\",\n"
                + "    \"id\" : \"https://www.example.org/officiaid\",\n"
                + "    \"labels\" : {\n"
                + "      \"da\" : \"7drvQTdo6cc2W2POtp\"\n"
                + "    }\n"
                + "  },\n"
                + "  \"createdDate\" : \"1985-12-14T04:46:53.617Z\",\n"
                + "  \"modifiedDate\" : \"1977-11-16T20:07:53.129Z\",\n"
                + "  \"publishedDate\" : \"1987-11-13T14:59:00.644Z\",\n"
                + "  \"indexedDate\" : \"2014-05-05T01:34:04.400Z\",\n"
                + "  \"handle\" : \"https://www.example.org/autemexercitationem\",\n"
                + "  \"doi\" : \"https://doi.org/10.1234/soluta\",\n"
                + "  \"link\" : \"https://www.example.org/atqueex\",\n"
                + "  \"entityDescription\" : {\n"
                + "    \"type\" : \"EntityDescription\",\n"
                + "    \"mainTitle\" : \"AmS2yjvSQnX3OC\",\n"
                + "    \"alternativeTitles\" : {\n"
                + "      \"se\" : \"4aGtUraL2s5C\"\n"
                + "    },\n"
                + "    \"language\" : \"http://lexvo.org/id/iso639-3/und\",\n"
                + "    \"publicationDate\" : {\n"
                + "      \"type\" : \"PublicationDate\",\n"
                + "      \"year\" : \"ZB0TwufwrqZ156xU\",\n"
                + "      \"month\" : \"2lMhGnmZrn0L\",\n"
                + "      \"day\" : \"hqUONIL4vyCBgnU\"\n"
                + "    },\n"
                + "    \"contributors\" : [ {\n"
                + "      \"type\" : \"Contributor\",\n"
                + "      \"identity\" : {\n"
                + "        \"type\" : \"Identity\",\n"
                + "        \"id\" : \"https://www.example.com/QlcJ3L323h\",\n"
                + "        \"name\" : \"1ebmzOBiJ2dnTQKnB\",\n"
                + "        \"nameType\" : \"Organizational\",\n"
                + "        \"orcId\" : \"eOhwVJDv71pmHDF\"\n"
                + "      },\n"
                + "      \"affiliations\" : [ {\n"
                + "        \"type\" : \"Organization\",\n"
                + "        \"id\" : \"https://www.example.com/OsG5hAol4U\",\n"
                + "        \"labels\" : {\n"
                + "          \"bg\" : \"jdUh7IMV9zYF0edir\"\n"
                + "        }\n"
                + "      } ],\n"
                + "      \"role\" : {\n"
                + "        \"type\" : \"DataManager\"\n"
                + "      },\n"
                + "      \"sequence\" : 9,\n"
                + "      \"correspondingAuthor\" : false\n"
                + "    }, {\n"
                + "      \"type\" : \"Contributor\",\n"
                + "      \"identity\" : {\n"
                + "        \"type\" : \"Identity\",\n"
                + "        \"id\" : \"https://www.example.com/Jj60na5LhODJv\",\n"
                + "        \"name\" : \"ILjjnTuCcWf\",\n"
                + "        \"nameType\" : \"Personal\",\n"
                + "        \"orcId\" : \"cnzP3ULyZgIJM9lREm\"\n"
                + "      },\n"
                + "      \"affiliations\" : [ {\n"
                + "        \"type\" : \"Organization\",\n"
                + "        \"id\" : \"https://www.example.com/g5VvIs47o79m5OR\",\n"
                + "        \"labels\" : {\n"
                + "          \"nn\" : \"GssDz5vFvMCb5\"\n"
                + "        }\n"
                + "      } ],\n"
                + "      \"role\" : {\n"
                + "        \"type\" : \"ArchitecturalPlanner\"\n"
                + "      },\n"
                + "      \"sequence\" : 3,\n"
                + "      \"correspondingAuthor\" : false\n"
                + "    } ],\n"
                + "    \"alternativeAbstracts\" : {\n"
                + "      \"ca\" : \"DClr6p2eLS3hUsqS\"\n"
                + "    },\n"
                + "    \"npiSubjectHeading\" : \"jLpJlvZpTlPW\",\n"
                + "    \"tags\" : [ \"LVET3fP7Q7T\" ],\n"
                + "    \"description\" : \"qDj6riiyd2F82ZhdvD\",\n"
                + "    \"reference\" : {\n"
                + "      \"type\" : \"Reference\",\n"
                + "      \"publicationContext\" : {\n"
                + "        \"type\" : \"Artistic\"\n"
                + "      },\n"
                + "      \"doi\" : \"https://www.example.com/zYqAF9eCt4e2zs2\",\n"
                + "      \"publicationInstance\" : {\n"
                + "        \"type\" : \"MusicPerformance\",\n"
                + "        \"manifestations\" : [ {\n"
                + "          \"type\" : \"AudioVisualPublication\",\n"
                + "          \"mediaType\" : {\n"
                + "            \"type\" : \"Streaming\"\n"
                + "          },\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"wGWMxZfZn4VvTd\"\n"
                + "          },\n"
                + "          \"catalogueNumber\" : \"WpyrgJ8BeqA\",\n"
                + "          \"trackList\" : [ {\n"
                + "            \"type\" : \"MusicTrack\",\n"
                + "            \"title\" : \"PUHHGf8HUbiQYkidFX\",\n"
                + "            \"composer\" : \"CtC14ox26FA\",\n"
                + "            \"extent\" : \"yzoMxAKxknTon\"\n"
                + "          } ],\n"
                + "          \"isrc\" : {\n"
                + "            \"type\" : \"Isrc\",\n"
                + "            \"value\" : null\n"
                + "          }\n"
                + "        }, {\n"
                + "          \"type\" : \"Concert\",\n"
                + "          \"place\" : {\n"
                + "            \"type\" : \"UnconfirmedPlace\",\n"
                + "            \"label\" : \"TycsRIi0wDZ1mFox\",\n"
                + "            \"country\" : \"RSzxZUULUnOn1\"\n"
                + "          },\n"
                + "          \"time\" : {\n"
                + "            \"type\" : \"Instant\",\n"
                + "            \"value\" : \"1986-09-08T16:48:47.662Z\"\n"
                + "          },\n"
                + "          \"extent\" : \"4cOeaOPJjzMRUS1FhMo\",\n"
                + "          \"description\" : \"VnfYTNX9iaYo3xRP\",\n"
                + "          \"concertProgramme\" : [ {\n"
                + "            \"type\" : \"MusicalWorkPerformance\",\n"
                + "            \"title\" : \"yDfcjOSVj5XoWH\",\n"
                + "            \"composer\" : \"O2EqSF15CvV0\",\n"
                + "            \"premiere\" : false\n"
                + "          } ],\n"
                + "          \"concertSeries\" : \"4PieNH5rmCxcP\"\n"
                + "        }, {\n"
                + "          \"type\" : \"MusicScore\",\n"
                + "          \"ensemble\" : \"isTE1ROYEfUIMyifb\",\n"
                + "          \"movements\" : \"LOsXoE0nbwRNG04KCC\",\n"
                + "          \"extent\" : \"XCXi6sS2jo0BBfF4rSR\",\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"G4UHf0x6j0\"\n"
                + "          },\n"
                + "          \"ismn\" : {\n"
                + "            \"type\" : \"Ismn\",\n"
                + "            \"value\" : \"M230671187\",\n"
                + "            \"formatted\" : \"M-2306-7118-7\"\n"
                + "          }\n"
                + "        }, {\n"
                + "          \"type\" : \"OtherPerformance\",\n"
                + "          \"performanceType\" : \"TUU2UJfyq4zwiXPEE4J\",\n"
                + "          \"place\" : {\n"
                + "            \"type\" : \"UnconfirmedPlace\",\n"
                + "            \"label\" : \"3FzQ7jCMa9R45i\",\n"
                + "            \"country\" : \"reiEGyPoOjQ4Kxg\"\n"
                + "          },\n"
                + "          \"extent\" : \"uGP1KCMdfL\",\n"
                + "          \"musicalWorks\" : [ {\n"
                + "            \"type\" : \"MusicalWork\",\n"
                + "            \"title\" : \"ksxdOqwKHkb\",\n"
                + "            \"composer\" : \"hZzaxm9qzj1rCY\"\n"
                + "          } ]\n"
                + "        } ],\n"
                + "        \"pages\" : {\n"
                + "          \"type\" : \"NullPages\"\n"
                + "        }\n"
                + "      }\n"
                + "    },\n"
                + "    \"metadataSource\" : \"https://www.example.com/uoOiAv1DZg38bMy1e\",\n"
                + "    \"abstract\" : \"ctVMhTVqOnSsAa4x\"\n"
                + "  },\n"
                + "  \"projects\" : [ {\n"
                + "    \"type\" : \"ResearchProject\",\n"
                + "    \"id\" : \"https://www.example.org/officiaet\",\n"
                + "    \"name\" : \"1Lu55ThEP4\",\n"
                + "    \"approvals\" : [ {\n"
                + "      \"type\" : \"Approval\",\n"
                + "      \"approvalDate\" : \"1976-09-14T10:56:47.253Z\",\n"
                + "      \"approvedBy\" : \"DIRHEALTH\",\n"
                + "      \"approvalStatus\" : \"NOTAPPLIED\",\n"
                + "      \"applicationCode\" : \"4aUoTZVzFh1Pp\"\n"
                + "    } ]\n"
                + "  } ],\n"
                + "  \"fundings\" : [ {\n"
                + "    \"type\" : \"UnconfirmedFunding\",\n"
                + "    \"source\" : \"https://www.example.org/cumab\",\n"
                + "    \"identifier\" : \"aq9ZkwOOfnrBvNs\",\n"
                + "    \"labels\" : {\n"
                + "      \"sv\" : \"2Gi8mxfUI7E\"\n"
                + "    },\n"
                + "    \"fundingAmount\" : {\n"
                + "      \"currency\" : \"EUR\",\n"
                + "      \"amount\" : 1395791485\n"
                + "    },\n"
                + "    \"activeFrom\" : \"1979-11-26T13:58:06.197Z\",\n"
                + "    \"activeTo\" : \"1988-06-24T06:39:47.529Z\"\n"
                + "  }, {\n"
                + "    \"type\" : \"ConfirmedFunding\",\n"
                + "    \"source\" : \"https://www.example.org/utsit\",\n"
                + "    \"id\" : \"https://www.example.org/hicaccusantium\",\n"
                + "    \"identifier\" : \"U9MWC7Qkw5\",\n"
                + "    \"labels\" : {\n"
                + "      \"af\" : \"neIx1EcuYhcsS2\"\n"
                + "    },\n"
                + "    \"fundingAmount\" : {\n"
                + "      \"currency\" : \"GBP\",\n"
                + "      \"amount\" : 116542712\n"
                + "    },\n"
                + "    \"activeFrom\" : \"1973-06-21T13:58:30.147Z\",\n"
                + "    \"activeTo\" : \"1986-07-02T03:18:53.792Z\"\n"
                + "  } ],\n"
                + "  \"additionalIdentifiers\" : [ {\n"
                + "    \"type\" : \"AdditionalIdentifier\",\n"
                + "    \"source\" : \"fakesource\",\n"
                + "    \"value\" : \"1234\"\n"
                + "  } ],\n"
                + "  \"subjects\" : [ \"https://www.example.org/autipsum\" ],\n"
                + "  \"associatedArtifacts\" : [ {\n"
                + "    \"type\" : \"PublishedFile\",\n"
                + "    \"identifier\" : \"2edb28f5-2063-4dd7-b0b5-2f0354abc491\",\n"
                + "    \"name\" : \"pxhYKXC3A1tWSetnxHA\",\n"
                + "    \"mimeType\" : \"1GIH23bD3mPl\",\n"
                + "    \"size\" : 1511398389,\n"
                + "    \"license\" : {\n"
                + "      \"type\" : \"License\",\n"
                + "      \"identifier\" : \"UWahzYJqz49yqFMzAh\",\n"
                + "      \"labels\" : {\n"
                + "        \"pl\" : \"fuKVVSVBXaiES\"\n"
                + "      },\n"
                + "      \"link\" : \"https://www.example.com/5PfvaXnepjt8nA\"\n"
                + "    },\n"
                + "    \"administrativeAgreement\" : false,\n"
                + "    \"publisherAuthority\" : true,\n"
                + "    \"publishedDate\" : \"2001-08-21T23:17:53.637Z\",\n"
                + "    \"visibleForNonOwner\" : true\n"
                + "  }, {\n"
                + "    \"type\" : \"AssociatedLink\",\n"
                + "    \"id\" : \"https://www.example.com/3dhudmlpje\",\n"
                + "    \"name\" : \"6WUHeva5om27c\",\n"
                + "    \"description\" : \"Y4AGCPHRYSz\"\n"
                + "  } ],\n"
                + "  \"rightsHolder\" : \"sjnrtkZywSHc1Vs\",\n"
                + "  \"modelVersion\" : \"0.20.17\"\n"
                + "}";
    }

    private static String isrcPublicationWithoutIsrc() {
        return "{\n"
                + "  \"type\" : \"Publication\",\n"
                + "  \"identifier\" : \"c443030e-9d56-43d8-afd1-8c89105af555\",\n"
                + "  \"status\" : \"NEW\",\n"
                + "  \"resourceOwner\" : {\n"
                + "    \"owner\" : \"RwWXJcCDJnQP7ou\",\n"
                + "    \"ownerAffiliation\" : \"https://www.example.org/utunde\"\n"
                + "  },\n"
                + "  \"publisher\" : {\n"
                + "    \"type\" : \"Organization\",\n"
                + "    \"id\" : \"https://www.example.org/officiaid\",\n"
                + "    \"labels\" : {\n"
                + "      \"da\" : \"7drvQTdo6cc2W2POtp\"\n"
                + "    }\n"
                + "  },\n"
                + "  \"createdDate\" : \"1985-12-14T04:46:53.617Z\",\n"
                + "  \"modifiedDate\" : \"1977-11-16T20:07:53.129Z\",\n"
                + "  \"publishedDate\" : \"1987-11-13T14:59:00.644Z\",\n"
                + "  \"indexedDate\" : \"2014-05-05T01:34:04.400Z\",\n"
                + "  \"handle\" : \"https://www.example.org/autemexercitationem\",\n"
                + "  \"doi\" : \"https://doi.org/10.1234/soluta\",\n"
                + "  \"link\" : \"https://www.example.org/atqueex\",\n"
                + "  \"entityDescription\" : {\n"
                + "    \"type\" : \"EntityDescription\",\n"
                + "    \"mainTitle\" : \"AmS2yjvSQnX3OC\",\n"
                + "    \"alternativeTitles\" : {\n"
                + "      \"se\" : \"4aGtUraL2s5C\"\n"
                + "    },\n"
                + "    \"language\" : \"http://lexvo.org/id/iso639-3/und\",\n"
                + "    \"publicationDate\" : {\n"
                + "      \"type\" : \"PublicationDate\",\n"
                + "      \"year\" : \"ZB0TwufwrqZ156xU\",\n"
                + "      \"month\" : \"2lMhGnmZrn0L\",\n"
                + "      \"day\" : \"hqUONIL4vyCBgnU\"\n"
                + "    },\n"
                + "    \"contributors\" : [ {\n"
                + "      \"type\" : \"Contributor\",\n"
                + "      \"identity\" : {\n"
                + "        \"type\" : \"Identity\",\n"
                + "        \"id\" : \"https://www.example.com/QlcJ3L323h\",\n"
                + "        \"name\" : \"1ebmzOBiJ2dnTQKnB\",\n"
                + "        \"nameType\" : \"Organizational\",\n"
                + "        \"orcId\" : \"eOhwVJDv71pmHDF\"\n"
                + "      },\n"
                + "      \"affiliations\" : [ {\n"
                + "        \"type\" : \"Organization\",\n"
                + "        \"id\" : \"https://www.example.com/OsG5hAol4U\",\n"
                + "        \"labels\" : {\n"
                + "          \"bg\" : \"jdUh7IMV9zYF0edir\"\n"
                + "        }\n"
                + "      } ],\n"
                + "      \"role\" : {\n"
                + "        \"type\" : \"DataManager\"\n"
                + "      },\n"
                + "      \"sequence\" : 9,\n"
                + "      \"correspondingAuthor\" : false\n"
                + "    }, {\n"
                + "      \"type\" : \"Contributor\",\n"
                + "      \"identity\" : {\n"
                + "        \"type\" : \"Identity\",\n"
                + "        \"id\" : \"https://www.example.com/Jj60na5LhODJv\",\n"
                + "        \"name\" : \"ILjjnTuCcWf\",\n"
                + "        \"nameType\" : \"Personal\",\n"
                + "        \"orcId\" : \"cnzP3ULyZgIJM9lREm\"\n"
                + "      },\n"
                + "      \"affiliations\" : [ {\n"
                + "        \"type\" : \"Organization\",\n"
                + "        \"id\" : \"https://www.example.com/g5VvIs47o79m5OR\",\n"
                + "        \"labels\" : {\n"
                + "          \"nn\" : \"GssDz5vFvMCb5\"\n"
                + "        }\n"
                + "      } ],\n"
                + "      \"role\" : {\n"
                + "        \"type\" : \"ArchitecturalPlanner\"\n"
                + "      },\n"
                + "      \"sequence\" : 3,\n"
                + "      \"correspondingAuthor\" : false\n"
                + "    } ],\n"
                + "    \"alternativeAbstracts\" : {\n"
                + "      \"ca\" : \"DClr6p2eLS3hUsqS\"\n"
                + "    },\n"
                + "    \"npiSubjectHeading\" : \"jLpJlvZpTlPW\",\n"
                + "    \"tags\" : [ \"LVET3fP7Q7T\" ],\n"
                + "    \"description\" : \"qDj6riiyd2F82ZhdvD\",\n"
                + "    \"reference\" : {\n"
                + "      \"type\" : \"Reference\",\n"
                + "      \"publicationContext\" : {\n"
                + "        \"type\" : \"Artistic\"\n"
                + "      },\n"
                + "      \"doi\" : \"https://www.example.com/zYqAF9eCt4e2zs2\",\n"
                + "      \"publicationInstance\" : {\n"
                + "        \"type\" : \"MusicPerformance\",\n"
                + "        \"manifestations\" : [ {\n"
                + "          \"type\" : \"AudioVisualPublication\",\n"
                + "          \"mediaType\" : {\n"
                + "            \"type\" : \"Streaming\"\n"
                + "          },\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"wGWMxZfZn4VvTd\"\n"
                + "          },\n"
                + "          \"catalogueNumber\" : \"WpyrgJ8BeqA\",\n"
                + "          \"trackList\" : [ {\n"
                + "            \"type\" : \"MusicTrack\",\n"
                + "            \"title\" : \"PUHHGf8HUbiQYkidFX\",\n"
                + "            \"composer\" : \"CtC14ox26FA\",\n"
                + "            \"extent\" : \"yzoMxAKxknTon\"\n"
                + "          } ],\n"
                + "          \"isrc\" : null\n"
                + "        }, {\n"
                + "          \"type\" : \"Concert\",\n"
                + "          \"place\" : {\n"
                + "            \"type\" : \"UnconfirmedPlace\",\n"
                + "            \"label\" : \"TycsRIi0wDZ1mFox\",\n"
                + "            \"country\" : \"RSzxZUULUnOn1\"\n"
                + "          },\n"
                + "          \"time\" : {\n"
                + "            \"type\" : \"Instant\",\n"
                + "            \"value\" : \"1986-09-08T16:48:47.662Z\"\n"
                + "          },\n"
                + "          \"extent\" : \"4cOeaOPJjzMRUS1FhMo\",\n"
                + "          \"description\" : \"VnfYTNX9iaYo3xRP\",\n"
                + "          \"concertProgramme\" : [ {\n"
                + "            \"type\" : \"MusicalWorkPerformance\",\n"
                + "            \"title\" : \"yDfcjOSVj5XoWH\",\n"
                + "            \"composer\" : \"O2EqSF15CvV0\",\n"
                + "            \"premiere\" : false\n"
                + "          } ],\n"
                + "          \"concertSeries\" : \"4PieNH5rmCxcP\"\n"
                + "        }, {\n"
                + "          \"type\" : \"MusicScore\",\n"
                + "          \"ensemble\" : \"isTE1ROYEfUIMyifb\",\n"
                + "          \"movements\" : \"LOsXoE0nbwRNG04KCC\",\n"
                + "          \"extent\" : \"XCXi6sS2jo0BBfF4rSR\",\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"G4UHf0x6j0\"\n"
                + "          },\n"
                + "          \"ismn\" : {\n"
                + "            \"type\" : \"Ismn\",\n"
                + "            \"value\" : \"M230671187\",\n"
                + "            \"formatted\" : \"M-2306-7118-7\"\n"
                + "          }\n"
                + "        }, {\n"
                + "          \"type\" : \"OtherPerformance\",\n"
                + "          \"performanceType\" : \"TUU2UJfyq4zwiXPEE4J\",\n"
                + "          \"place\" : {\n"
                + "            \"type\" : \"UnconfirmedPlace\",\n"
                + "            \"label\" : \"3FzQ7jCMa9R45i\",\n"
                + "            \"country\" : \"reiEGyPoOjQ4Kxg\"\n"
                + "          },\n"
                + "          \"extent\" : \"uGP1KCMdfL\",\n"
                + "          \"musicalWorks\" : [ {\n"
                + "            \"type\" : \"MusicalWork\",\n"
                + "            \"title\" : \"ksxdOqwKHkb\",\n"
                + "            \"composer\" : \"hZzaxm9qzj1rCY\"\n"
                + "          } ]\n"
                + "        } ],\n"
                + "        \"pages\" : {\n"
                + "          \"type\" : \"NullPages\"\n"
                + "        }\n"
                + "      }\n"
                + "    },\n"
                + "    \"metadataSource\" : \"https://www.example.com/uoOiAv1DZg38bMy1e\",\n"
                + "    \"abstract\" : \"ctVMhTVqOnSsAa4x\"\n"
                + "  },\n"
                + "  \"projects\" : [ {\n"
                + "    \"type\" : \"ResearchProject\",\n"
                + "    \"id\" : \"https://www.example.org/officiaet\",\n"
                + "    \"name\" : \"1Lu55ThEP4\",\n"
                + "    \"approvals\" : [ {\n"
                + "      \"type\" : \"Approval\",\n"
                + "      \"approvalDate\" : \"1976-09-14T10:56:47.253Z\",\n"
                + "      \"approvedBy\" : \"DIRHEALTH\",\n"
                + "      \"approvalStatus\" : \"NOTAPPLIED\",\n"
                + "      \"applicationCode\" : \"4aUoTZVzFh1Pp\"\n"
                + "    } ]\n"
                + "  } ],\n"
                + "  \"fundings\" : [ {\n"
                + "    \"type\" : \"UnconfirmedFunding\",\n"
                + "    \"source\" : \"https://www.example.org/cumab\",\n"
                + "    \"identifier\" : \"aq9ZkwOOfnrBvNs\",\n"
                + "    \"labels\" : {\n"
                + "      \"sv\" : \"2Gi8mxfUI7E\"\n"
                + "    },\n"
                + "    \"fundingAmount\" : {\n"
                + "      \"currency\" : \"EUR\",\n"
                + "      \"amount\" : 1395791485\n"
                + "    },\n"
                + "    \"activeFrom\" : \"1979-11-26T13:58:06.197Z\",\n"
                + "    \"activeTo\" : \"1988-06-24T06:39:47.529Z\"\n"
                + "  }, {\n"
                + "    \"type\" : \"ConfirmedFunding\",\n"
                + "    \"source\" : \"https://www.example.org/utsit\",\n"
                + "    \"id\" : \"https://www.example.org/hicaccusantium\",\n"
                + "    \"identifier\" : \"U9MWC7Qkw5\",\n"
                + "    \"labels\" : {\n"
                + "      \"af\" : \"neIx1EcuYhcsS2\"\n"
                + "    },\n"
                + "    \"fundingAmount\" : {\n"
                + "      \"currency\" : \"GBP\",\n"
                + "      \"amount\" : 116542712\n"
                + "    },\n"
                + "    \"activeFrom\" : \"1973-06-21T13:58:30.147Z\",\n"
                + "    \"activeTo\" : \"1986-07-02T03:18:53.792Z\"\n"
                + "  } ],\n"
                + "  \"additionalIdentifiers\" : [ {\n"
                + "    \"type\" : \"AdditionalIdentifier\",\n"
                + "    \"source\" : \"fakesource\",\n"
                + "    \"value\" : \"1234\"\n"
                + "  } ],\n"
                + "  \"subjects\" : [ \"https://www.example.org/autipsum\" ],\n"
                + "  \"associatedArtifacts\" : [ {\n"
                + "    \"type\" : \"PublishedFile\",\n"
                + "    \"identifier\" : \"2edb28f5-2063-4dd7-b0b5-2f0354abc491\",\n"
                + "    \"name\" : \"pxhYKXC3A1tWSetnxHA\",\n"
                + "    \"mimeType\" : \"1GIH23bD3mPl\",\n"
                + "    \"size\" : 1511398389,\n"
                + "    \"license\" : {\n"
                + "      \"type\" : \"License\",\n"
                + "      \"identifier\" : \"UWahzYJqz49yqFMzAh\",\n"
                + "      \"labels\" : {\n"
                + "        \"pl\" : \"fuKVVSVBXaiES\"\n"
                + "      },\n"
                + "      \"link\" : \"https://www.example.com/5PfvaXnepjt8nA\"\n"
                + "    },\n"
                + "    \"administrativeAgreement\" : false,\n"
                + "    \"publisherAuthority\" : true,\n"
                + "    \"publishedDate\" : \"2001-08-21T23:17:53.637Z\",\n"
                + "    \"visibleForNonOwner\" : true\n"
                + "  }, {\n"
                + "    \"type\" : \"AssociatedLink\",\n"
                + "    \"id\" : \"https://www.example.com/3dhudmlpje\",\n"
                + "    \"name\" : \"6WUHeva5om27c\",\n"
                + "    \"description\" : \"Y4AGCPHRYSz\"\n"
                + "  } ],\n"
                + "  \"rightsHolder\" : \"sjnrtkZywSHc1Vs\",\n"
                + "  \"modelVersion\" : \"0.20.17\"\n"
                + "}";
    }
}