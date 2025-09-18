package no.unit.nva.publication.uriretriever;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.HttpStatus.SC_MOVED_PERMANENTLY;
import static org.apache.http.HttpStatus.SC_OK;
import com.google.common.net.MediaType;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Artistic;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.ExhibitionContent;
import no.unit.nva.model.contexttypes.GeographicalContent;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.MediaContribution;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.funding.ConfirmedFunding;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings("PMD.GodClass")
public final class FakeUriResponse {

    private static final String API_HOST = new Environment().readEnv("API_HOST");
    private static final String ORGANIZATION_BASE = "123";
    public static final URI HARD_CODED_TOP_LEVEL_ORG_URI = constructCristinOrgUri(ORGANIZATION_BASE + ".0.0.0");
    public static final URI HARD_CODED_LEVEL_2_ORG_URI = constructCristinOrgUri(ORGANIZATION_BASE + ".1.0.0");
    public static final URI HARD_CODED_LEVEL_3_ORG_URI = constructCristinOrgUri(ORGANIZATION_BASE + ".1.1.0");
    private static final String PENDING_NVI_RESPONSE = """
        {
            "reportStatus": {
                "status": "PENDING_REVIEW",
                "description": "Pending review. Awaiting approval from all institutions"
            },
            "period": "2024"
        }""";
    private static final URI CHANNEL_SAME_AS =
        URI.create("https://kanalregister.hkdir.no/publiseringskanaler/KanalForlagInfo"
                   + "?pid=91CE8D38-5C23-4A97-A429-EDECBD8586B5");

    private FakeUriResponse() {
        // NO-OP
    }

    /**
     * This setup mutes the anthology identifier to mock the response of the parent publication.
     */
    public static void setupFakeForType(Publication publication, FakeUriRetriever fakeUriRetriever,
                                        ResourceService resourceService, boolean publicationContextRedirects) {
        fakeContributorResponses(publication, fakeUriRetriever);
        fakeOwnerResponse(fakeUriRetriever, publication.getResourceOwner().getOwnerAffiliation());
        fakePendingNviResponse(fakeUriRetriever, publication);
        fakeFundingResponses(fakeUriRetriever, publication);
        fakeProjectResponses(fakeUriRetriever, publication, emptyList());
        fakeContextResponses(publication, fakeUriRetriever, resourceService, publicationContextRedirects);
        if (publication instanceof ImportCandidate) {
            createFakeCustomerApiResponse(fakeUriRetriever);
        } else {
            resourceService.updateResource(Resource.fromPublication(publication),
                                           UserInstance.fromPublication(publication));
        }
    }

    public static void setupFakeForType(TicketEntry ticket, FakeUriRetriever fakeUriRetriever) {
        var responsibilityArea = ticket.getReceivingOrganizationDetails().subOrganizationId();
        fakeUriRetriever.registerResponse(responsibilityArea, SC_OK, APPLICATION_JSON_LD,
                                          createCristinOrganizationResponse(responsibilityArea));
        fakeUriRetriever.registerResponse(ticket.getCustomerId(), SC_OK, APPLICATION_JSON_LD,
                                          createCristinOrganizationResponse(ticket.getCustomerId()));
        setUpPersonResponse(fakeUriRetriever, ticket.getOwner());
        setUpPersonResponse(fakeUriRetriever, ticket.getAssignee());
        setUpPersonResponse(fakeUriRetriever, ticket.getFinalizedBy());
        setUpPersonResponse(fakeUriRetriever, ticket.getViewedBy());
    }

    /**
     * This allows an override of the default value for the response.
     *
     * @param fakeUriRetriever The faked URI retrieval object.
     * @param statusCode       The desired response status code.
     * @param publication      The source of the URI for which we mock the response.
     * @param response         The desired response in JSON.
     */
    public static void setUpNviResponse(FakeUriRetriever fakeUriRetriever, int statusCode, Publication publication,
                                        String response) {
        var id = PublicationResponse.fromPublication(publication).getId();
        fakeUriRetriever.registerResponse(createNviCandidateUri(id.toString()), statusCode, MediaType.JSON_UTF_8,
                                          response);
    }

    public static URI constructCristinOrgUri(String identifier) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild("cristin")
                   .addChild("organization")
                   .addChild(identifier)
                   .getUri();
    }

    private static void createFakeCustomerApiResponse(FakeUriRetriever fakeUriRetriever) {
        fakeUriRetriever.registerResponse(toFetchCustomerByCristinIdUri(HARD_CODED_TOP_LEVEL_ORG_URI), SC_OK,
                                          MediaType.JSON_UTF_8,
                                          createCustomerApiResponse());
    }

    private static URI toFetchCustomerByCristinIdUri(URI uri) {
        var getCustomerEndpoint = UriWrapper.fromHost(API_HOST).addChild("customer").addChild("cristinId").getUri();
        return URI.create(
            getCustomerEndpoint + "/" + URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8));
    }

    private static void fakeContextResponses(Publication publication,
                                             FakeUriRetriever fakeUriRetriever, ResourceService resourceService,
                                             boolean publicationContextRedirects) {

        extractPublicationContext(publication)
            .ifPresent(publicationContext -> selectResponsesToFake(fakeUriRetriever,
                                                                   resourceService, publicationContext,
                                                                   publicationContextRedirects));
    }

    private static void selectResponsesToFake(FakeUriRetriever fakeUriRetriever,
                                              ResourceService resourceService, PublicationContext publicationContext,
                                              boolean publicationContextRedirects) {
        switch (publicationContext) {
            case Anthology anthologyContext -> setupFakeResponsesForAnthology(fakeUriRetriever, resourceService,
                                                                              anthologyContext);
            case Book book when book.getPublisher() instanceof Publisher publisher ->
                setupFakeResponsesForBookTypes(fakeUriRetriever, book, publisher);
            case Degree degree when degree.getPublisher() instanceof Publisher publisher ->
                setupFakeResponsesForBookTypes(fakeUriRetriever, degree, publisher);
            case GeographicalContent geographicalContent when
                geographicalContent.getPublisher() instanceof Publisher publisher -> {
                var uri = publisher.getId();
                fakeUriRetriever.registerResponse(uri, SC_OK, APPLICATION_JSON_LD, createPublisher(uri));
            }
            case Journal journal -> {
                var id = journal.getId();
                if (publicationContextRedirects) {
                    fakeUriRetriever.registerResponse(id, SC_MOVED_PERMANENTLY, APPLICATION_JSON_LD, null);
                } else {
                    fakeUriRetriever.registerResponse(id, SC_OK, APPLICATION_JSON_LD, createJournal(id));
                }
            }
            case Report report when report.getPublisher() instanceof Publisher publisher ->
                setupFakeResponsesForBookTypes(fakeUriRetriever, report, publisher);
            case ResearchData researchData when researchData.getPublisher() instanceof Publisher publisher -> {
                var uri = publisher.getId();
                fakeUriRetriever.registerResponse(uri, SC_OK, APPLICATION_JSON_LD, createPublisher(uri));
            }
            case Artistic ignored -> { /* No faking expected */ }
            case Event ignored -> { /* No faking expected */ }
            case ExhibitionContent ignored -> { /* No faking expected */ }
            case MediaContribution ignored -> { /* No faking expected */ }
            case Report ignored -> { /* No faking expected */ }
            case UnconfirmedJournal ignored -> { /* No faking expected */ }
            default -> throw new IllegalArgumentException("Unhandled publication context: " + publicationContext);
        }
    }

    private static void setupFakeResponsesForBookTypes(FakeUriRetriever fakeUriRetriever, Book book,
                                                       Publisher publisher) {
        URI id = publisher.getId();
        fakeUriRetriever.registerResponse(id, SC_OK, APPLICATION_JSON_LD, createPublisher(id));
        fakeSeriesResponse(fakeUriRetriever, book);
    }

    private static void setupFakeResponsesForAnthology(FakeUriRetriever fakeUriRetriever,
                                                       ResourceService resourceService, Anthology anthologyContext) {
        var parentPublication = randomPublication(BookAnthology.class);
        var persistedParent =
            attempt(() -> resourceService.createPublication(UserInstance.fromPublication(parentPublication),
                                                            parentPublication)).orElseThrow();
        anthologyContext.setId(getPublicationId(persistedParent));
        var parentResponse = PublicationResponse.fromPublication(parentPublication);
        fakePendingNviResponse(fakeUriRetriever, parentPublication);
        fakeFundingResponses(fakeUriRetriever, parentPublication);
        var book = (Book) parentResponse.getEntityDescription().getReference().getPublicationContext();
        fakePublisherResponse(fakeUriRetriever, book);
        fakeSeriesResponse(fakeUriRetriever, book);
    }

    private static Optional<PublicationContext> extractPublicationContext(Publication publication) {
        return nonNull(publication.getEntityDescription())
               && nonNull(publication.getEntityDescription().getReference())
                   ? Optional.of(publication.getEntityDescription().getReference().getPublicationContext())
                   : Optional.empty();
    }

    private static void fakeContributorResponses(Publication publication, FakeUriRetriever fakeUriRetriever) {
        extractAffiliations(publication).forEach(i -> createFakeOrganizationStructure(fakeUriRetriever, i));
    }

    private static List<URI> extractAffiliations(Publication publication) {
        return nonNull(publication.getEntityDescription()) && nonNull(
            publication.getEntityDescription().getContributors())
                   ? publication.getEntityDescription().getContributors().stream()
                         .map(Contributor::getAffiliations)
                         .flatMap(Collection::stream)
                         .map(Organization.class::cast)
                         .map(Organization::getId)
                         .filter(Objects::nonNull)
                         .toList()
                   : List.of();
    }

    private static void createFakeOrganizationStructure(FakeUriRetriever fakeUriRetriever, URI uri) {
        if (HARD_CODED_TOP_LEVEL_ORG_URI.equals(uri)) {
            fakeUriRetriever.registerResponse(uri, SC_OK, MediaType.JSON_UTF_8,
                                              createCristinOrganizationResponseForTopLevelOrg(uri));
        } else {
            fakeUriRetriever.registerResponse(uri, SC_OK, MediaType.JSON_UTF_8, createCristinOrganizationResponse(uri));
        }
    }

    private static void setUpPersonResponse(FakeUriRetriever fakeUriRetriever, Object person) {
        if (nonNull(person)) {
            var assigneeUri = createOwnerUri(person.toString());
            fakeUriRetriever.registerResponse(assigneeUri, SC_OK, APPLICATION_JSON_LD,
                                              createPersonResponse(person.toString(), null));
        }
    }

    private static String createPersonResponse(String username, URI affiliation) {
        return """
            {
              "@context" : "https://example.org/person-context.json",
              "id" : "https://api.dev.nva.aws.unit.no/cristin/person/%s",
              "type" : "Person",
              "identifiers" : [ {
                "type" : "CristinIdentifier",
                "value" : "%s"
              } ],
              "names" : [ {
                "type" : "FirstName",
                "value" : "someFirstName"
              }, {
                "type" : "LastName",
                "value" : "someLastName"
              }, {
                "type" : "PreferredFirstName",
                "value" : "somePreferredFirstName"
              }, {
                "type" : "PreferredLastName",
                "value" : "somePreferredLastName"
              } ],
              "affiliations" : [ {
                "type" : "Affiliation",
                "organization" : "%s",
                "active" : false,
                "role" : {
                  "type" : "Role",
                  "labels" : {
                    "en" : "Guest",
                    "nb" : "Gjest"
                  }
                }
              } ],
              "verified" : true,
              "keywords" : [ ],
              "background" : { },
              "place" : { },
              "collaboration" : { },
              "countries" : [ ],
              "awards" : [ ]
            }
            """.formatted(username, username, affiliation);
    }

    private static String extractCristinId(String owner) {
        return owner.split("@")[0];
    }

    private static URI createOwnerUri(String owner) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild("cristin")
                   .addChild("person")
                   .addChild(extractCristinId(owner))
                   .getUri();
    }

    private static void fakeOwnerResponse(FakeUriRetriever fakeUriRetriever, URI ownerAffiliation) {
        fakeUriRetriever.registerResponse(ownerAffiliation, SC_OK, APPLICATION_JSON_LD,
                                          createCristinOrganizationResponse(ownerAffiliation));
    }

    private static void fakeFundingResponses(FakeUriRetriever fakeUriRetriever, Publication publication) {
        publication.getFundings().forEach(funding -> fakeFundingResponse(fakeUriRetriever, funding));
    }

    public static void fakeProjectResponses(FakeUriRetriever fakeUriRetriever, Publication publication,
                                             List<Funding> fundings) {
        publication.getProjects().forEach(project -> fakeProjectResponse(fakeUriRetriever, project, fundings));
    }

    private static void fakeFundingResponse(FakeUriRetriever fakeUriRetriever, Funding funding) {
        var source = funding.getSource();
        if (funding instanceof ConfirmedFunding confirmedFunding) {
            var id = confirmedFunding.getId();
            fakeUriRetriever.registerResponse(id,
                                              SC_OK,
                                              APPLICATION_JSON_LD,
                                              confirmedFundingResponse(id, source));
        }

        fakeUriRetriever.registerResponse(source,
                                          SC_OK,
                                          APPLICATION_JSON_LD,
                                          fundingSourceResponse(source));
    }

    private static String fundingSourceResponse(URI source) {
        return """
            {
              "type" : "FundingSource",
              "id" : "%s",
              "identifier" : "NFR",
              "labels" : {
                "en" : "Research Council of Norway (RCN)",
                "nb" : "Norges forskningsråd"
              },
              "name" : {
                "en" : "Research Council of Norway (RCN)",
                "nb" : "Norges forskningsråd"
              },
              "@context" : {
                "@vocab" : "https://nva.sikt.no/ontology/publication#",
                "id" : "@id",
                "type" : "@type",
                "labels" : {
                  "@id" : "label",
                  "@container" : "@language"
                }
              }
            }
            """.formatted(source);
    }

    private static String confirmedFundingResponse(URI id, URI source) {
        return """
            {
              "type" : "ConfirmedFunding",
              "source" : "%s",
              "id" : "%s",
              "identifier" : "NFR00011-12-3211"
              }
            """.formatted(source, id);
    }

    private static void fakeProjectResponse(FakeUriRetriever fakeUriRetriever, ResearchProject project,
                                            List<Funding> fundings) {
        fakeUriRetriever.registerResponse(project.getId(), SC_OK, APPLICATION_JSON_LD,
                                          projectResponse(project.getId(), getFundingsArray(fundings)));
    }

    private static String getFundingsArray(List<Funding> fundings) {
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(fundings)).orElseThrow();
    }


    private static String projectResponse(URI id, String fundingsArray) {
        return """
            {
               "id": "%s",
               "type": "Project",
               "identifiers": [{ "type": "CristinIdentifier", "value": "2596568" }],
               "title": "FME HyValue - Norwegian centre for hydrogen value chain research",
               "language": "http://lexvo.org/id/iso639-3/eng",
               "alternativeTitles": [],
               "startDate": "2022-07-01T00:00:00Z",
               "endDate": "2030-06-30T00:00:00Z",
               "funding": %s,
               "coordinatingInstitution": {
                 "type": "Organization",
                 "id": "https://api.nva.unit.no/cristin/organization/2057.1.0.0",
                 "labels": {
                   "en": "NORCE Energy & Technology",
                   "nb": "NORCE Energi og teknologi"
                 }
               },
               "contributors": [
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/26636",
                     "firstName": "Fionn",
                     "lastName": "Iversen"
                   },
                   "roles": [
                     {
                       "type": "ProjectManager",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/2057.1.0.0",
                         "labels": {
                           "en": "NORCE Energy & Technology",
                           "nb": "NORCE Energi og teknologi"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/51376",
                     "firstName": "Ellen Ingeborg",
                     "lastName": "Hætta"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/2057.1.0.0",
                         "labels": {
                           "en": "NORCE Energy & Technology",
                           "nb": "NORCE Energi og teknologi"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/643791",
                     "firstName": "Nazanin",
                     "lastName": "Jahani"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/2057.1.0.0",
                         "labels": {
                           "en": "NORCE Energy & Technology",
                           "nb": "NORCE Energi og teknologi"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/332037",
                     "firstName": "Sindre Aske",
                     "lastName": "Høyland"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/2057.2.0.0",
                         "labels": {
                           "en": "NORCE Health & Social Sciences",
                           "nb": "NORCE Helse og samfunn"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/7146",
                     "firstName": "Tor Håkon Jackson",
                     "lastName": "Inderberg"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/7430.0.0.0",
                         "labels": {
                           "en": "Fridtjof Nansen Institute",
                           "nb": "Fridtjof Nansens institutt"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/12187",
                     "firstName": "Irja H",
                     "lastName": "Vormedal"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/7430.0.0.0",
                         "labels": {
                           "en": "Fridtjof Nansen Institute",
                           "nb": "Fridtjof Nansens institutt"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/317795",
                     "firstName": "Kenneth Løvold",
                     "lastName": "Rødseth"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/7482.0.0.0",
                         "labels": {
                           "en": "Institute of Transport Economics",
                           "nb": "Transportøkonomisk institutt"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/481383",
                     "firstName": "Linda Ager-Wick",
                     "lastName": "Ellingsen"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/7482.0.0.0",
                         "labels": {
                           "en": "Institute of Transport Economics",
                           "nb": "Transportøkonomisk institutt"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/39122",
                     "firstName": "Jardar",
                     "lastName": "Andersen"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/7482.0.0.0",
                         "labels": {
                           "en": "Institute of Transport Economics",
                           "nb": "Transportøkonomisk institutt"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/866997",
                     "firstName": "Håkon",
                     "lastName": "Eidsvåg"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/2057.1.0.0",
                         "labels": {
                           "en": "NORCE Energy & Technology",
                           "nb": "NORCE Energi og teknologi"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/395695",
                     "firstName": "Frode Andre",
                     "lastName": "Skjeret"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/191.0.0.0",
                         "labels": {
                           "en": "Norwegian School of Economics",
                           "nb": "Norges Handelshøyskole",
                           "nn": "Noregs Handelshøgskole"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/52946",
                     "firstName": "Leif Kristoffer",
                     "lastName": "Sandal"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/191.0.0.0",
                         "labels": {
                           "en": "Norwegian School of Economics",
                           "nb": "Norges Handelshøyskole",
                           "nn": "Noregs Handelshøgskole"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "firstName": "Armando José Garcia",
                     "lastName": "Pires"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/191.0.0.0",
                         "labels": {
                           "en": "Norwegian School of Economics",
                           "nb": "Norges Handelshøyskole",
                           "nn": "Noregs Handelshøgskole"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/326298",
                     "firstName": "Gunnar",
                     "lastName": "Eskeland"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/191.0.0.0",
                         "labels": {
                           "en": "Norwegian School of Economics",
                           "nb": "Norges Handelshøyskole",
                           "nn": "Noregs Handelshøgskole"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/48699",
                     "firstName": "Ingrid",
                     "lastName": "Sundvor"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/7482.0.0.0",
                         "labels": {
                           "en": "Institute of Transport Economics",
                           "nb": "Transportøkonomisk institutt"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/443968",
                     "firstName": "Rebecca Jayne",
                     "lastName": "Thorne"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/7482.0.0.0",
                         "labels": {
                           "en": "Institute of Transport Economics",
                           "nb": "Transportøkonomisk institutt"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/3081",
                     "firstName": "Torbjørg",
                     "lastName": "Jevnaker"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/7430.0.0.0",
                         "labels": {
                           "en": "Fridtjof Nansen Institute",
                           "nb": "Fridtjof Nansens institutt"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/400608",
                     "firstName": "Jon Birger",
                     "lastName": "Skjærseth"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/7430.0.0.0",
                         "labels": {
                           "en": "Fridtjof Nansen Institute",
                           "nb": "Fridtjof Nansens institutt"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/7156",
                     "firstName": "Per Ove",
                     "lastName": "Eikeland"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/7430.0.0.0",
                         "labels": {
                           "en": "Fridtjof Nansen Institute",
                           "nb": "Fridtjof Nansens institutt"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/562470",
                     "firstName": "Svein Gunnar",
                     "lastName": "Sjøtun"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/203.0.0.0",
                         "labels": {
                           "en": "Western Norway University of Applied Sciences",
                           "nb": "Høgskulen på Vestlandet"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/480354",
                     "firstName": "Rune",
                     "lastName": "Njøs"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/203.0.0.0",
                         "labels": {
                           "en": "Western Norway University of Applied Sciences",
                           "nb": "Høgskulen på Vestlandet"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/33349",
                     "firstName": "Jonathan Økland",
                     "lastName": "Torstensen"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/203.0.0.0",
                         "labels": {
                           "en": "Western Norway University of Applied Sciences",
                           "nb": "Høgskulen på Vestlandet"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/51832",
                     "firstName": "Dhayalan",
                     "lastName": "Velauthapillai"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/203.0.0.0",
                         "labels": {
                           "en": "Western Norway University of Applied Sciences",
                           "nb": "Høgskulen på Vestlandet"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/50047",
                     "firstName": "Velaug Myrseth",
                     "lastName": "Oltedal"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/203.0.0.0",
                         "labels": {
                           "en": "Western Norway University of Applied Sciences",
                           "nb": "Høgskulen på Vestlandet"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/1187692",
                     "firstName": "Lars Martel Antoine",
                     "lastName": "Coenen"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/203.0.0.0",
                         "labels": {
                           "en": "Western Norway University of Applied Sciences",
                           "nb": "Høgskulen på Vestlandet"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/625381",
                     "firstName": "Murugesan",
                     "lastName": "Rasukkannu"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/203.0.0.0",
                         "labels": {
                           "en": "Western Norway University of Applied Sciences",
                           "nb": "Høgskulen på Vestlandet"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/46348",
                     "firstName": "Gidske Leknæs",
                     "lastName": "Andersen"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/184.15.41.0",
                         "labels": {
                           "en": "Department of Geography",
                           "nb": "Institutt for geografi"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/9316",
                     "firstName": "Pascal Daniel Croumbie",
                     "lastName": "Dietzel"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/184.12.31.0",
                         "labels": {
                           "en": "Department of Chemistry",
                           "nb": "Kjemisk institutt"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/52941",
                     "firstName": "Kristine",
                     "lastName": "Spildo"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/184.12.31.0",
                         "labels": {
                           "en": "Department of Chemistry",
                           "nb": "Kjemisk institutt"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/27072",
                     "firstName": "Vidar Remi",
                     "lastName": "Jensen"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/184.12.31.0",
                         "labels": {
                           "en": "Department of Chemistry",
                           "nb": "Kjemisk institutt"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/50532",
                     "firstName": "Berte-Elen Reinertsen",
                     "lastName": "Konow"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/184.16.0.0",
                         "labels": { "en": "Faculty of Law", "nb": "Det juridiske fakultet" }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/49865",
                     "firstName": "Sigrid Eskeland",
                     "lastName": "Schütz"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/184.16.0.0",
                         "labels": { "en": "Faculty of Law", "nb": "Det juridiske fakultet" }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/584602",
                     "firstName": "Ignacio",
                     "lastName": "Herrera Anchustegui"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/184.16.0.0",
                         "labels": { "en": "Faculty of Law", "nb": "Det juridiske fakultet" }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/602536",
                     "firstName": "Helene",
                     "lastName": "Hisken"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/184.12.24.0",
                         "labels": {
                           "en": "Department of Physics and Technology",
                           "nb": "Institutt for fysikk og teknologi"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/48517",
                     "firstName": "Trygve",
                     "lastName": "Skjold"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/184.12.24.0",
                         "labels": {
                           "en": "Department of Physics and Technology",
                           "nb": "Institutt for fysikk og teknologi"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/333810",
                     "firstName": "Jon Tømmerås",
                     "lastName": "Selvik"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/2057.1.0.0",
                         "labels": {
                           "en": "NORCE Energy & Technology",
                           "nb": "NORCE Energi og teknologi"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/332187",
                     "firstName": "Ove",
                     "lastName": "Njå"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/217.8.3.0",
                         "labels": {
                           "en": "Department of Safety, Economics and Planning",
                           "nb": "Institutt for sikkerhet, økonomi og planlegging"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/499092",
                     "firstName": "Antonie",
                     "lastName": "Oosterkamp"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/2057.1.0.0",
                         "labels": {
                           "en": "NORCE Energy & Technology",
                           "nb": "NORCE Energi og teknologi"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/441140",
                     "firstName": "Kari",
                     "lastName": "Kjestveit"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/2057.2.0.0",
                         "labels": {
                           "en": "NORCE Health & Social Sciences",
                           "nb": "NORCE Helse og samfunn"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/52283",
                     "firstName": "Kjetil",
                     "lastName": "Folgerø"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/2057.1.0.0",
                         "labels": {
                           "en": "NORCE Energy & Technology",
                           "nb": "NORCE Energi og teknologi"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/1150245",
                     "firstName": "Nicole",
                     "lastName": "Dopffel"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/2057.1.0.0",
                         "labels": {
                           "en": "NORCE Energy & Technology",
                           "nb": "NORCE Energi og teknologi"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/26132",
                     "firstName": "Geir",
                     "lastName": "Nævdal"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/2057.1.0.0",
                         "labels": {
                           "en": "NORCE Energy & Technology",
                           "nb": "NORCE Energi og teknologi"
                         }
                       }
                     }
                   ]
                 },
                 {
                   "identity": {
                     "type": "Person",
                     "id": "https://api.nva.unit.no/cristin/person/59498",
                     "firstName": "Jonas",
                     "lastName": "Solbakken"
                   },
                   "roles": [
                     {
                       "type": "ProjectParticipant",
                       "affiliation": {
                         "type": "Organization",
                         "id": "https://api.nva.unit.no/cristin/organization/2057.1.0.0",
                         "labels": {
                           "en": "NORCE Energy & Technology",
                           "nb": "NORCE Energi og teknologi"
                         }
                       }
                     }
                   ]
                 }
               ],
               "status": "ACTIVE",
               "academicSummary": {
                 "en": "<p>The proposed FME-centre HyValue aims to facilitate the safe and sustainable development of value chains for hydrogen and hydrogenbased energy carriers in industry and society. It will be run in close collaboration between research- and user partners. International cooperation and researcher training will be included in the centre. A deep transition to hydrogen-based energy carriers can only be achieved through a holistic approach including technical solutions, economic incentives, regulatory framework, societal and environmental impact and risk. HyValue takes a broad technical-economic-societal-environmental perspective, addressing hydrogen and ammonia value chains, business models, overarching policy and system integration issues. The centre will:</p>\\r\\n\\r\\n<p>1. explore methods for hydrogen and ammonia production, targeting novel technologies for significant increase in energy efficiency and corresponding cost reduction.</p>\\r\\n\\r\\n<p>2. exploit and develop solutions for transport, storage and filling/ bunkering of hydrogen-based fuels.</p>\\r\\n\\r\\n<p>3. analyse maritime value chains and study regulations and economic incentives to avoid barriers and promote business models for hydrogen and ammonia.</p>\\r\\n\\r\\n<p>4. develop a novel framework for assessing the strength of knowledge in risk assessments for hydrogen and ammonia systems.</p>\\r\\n\\r\\n<p>5. frame the technology development and business models in a societal embeddedness context and document the total emission of greenhouse gases for hydrogen and ammonia value chains</p>\\r\\n\\r\\n<p>The holistic approach adopted by the HyValue consortium will secure that the research supports the overall ambition to accelerate the transition to a net-zero economy, bringing about new technical solutions as well as empowering Norwegian industry with methods and tools that can enable the development of profitable products and services. HyValue will increase the national and international competitiveness on hydrogen-based solutions for implementation in society, with maritime application as the main driver.</p>\\r\\n"
               },
               "popularScientificSummary": {
                 "en": "<p>To achieve national and global carbon emission targets, particularly from the transport and industry sectors, there is an urgent need for alternatives to fossil fuels. Electrification and battery technology is not sufficient for this purpose, due to charging times, space required, and limits in power generation. Hydrogen can fill this technology gap, but energy requirements, societal preparedness, and commercial viability are all current obstructions. The HyValue research centre for environmental-friendly energy shall contribute to solving these challenges.</p>\\r\\n\\r\\n<p>Hydrogen-based energy carriers generate zero carbon emissions when used to generate power. Nonetheless, if emissions are to be avoided, production of these energy carriers also needs to be emission-free. This means that clean and renewable energy, such as hydro-, wind-, or solar power must be used as energy sources. Alternatively, the CO2 generated in the hydrogen production process must be captured and stored.</p>\\r\\n\\r\\n<p>The HyValue research centre assembles a broad cross-disciplinary consortium of national and international research partners with cutting-edge expertise in hydrogen related technical, economic, legal and societal fields of research. The centre research spans from studies of new, energy-efficient methods for production of hydrogen and ammonia, to how the hydrogen sector can be matured as a technical system in society. To achieve the latter, HyValue will also provide new knowledge to assess and improve risk assessments for hydrogen transport systems and value chains. Equally important is research on economic and regulatory barriers that must be overcome to establish a resilient hydrogen energy sector. The relevance of the research activities will be secured through the centre user partners, which represent leading national and international industrial firms as well as key public partners.</p>\\r\\n"
               },
               "published": true,
               "publishable": true,
               "created": { "date": "2023-05-22T09:37:59Z" },
               "lastModified": { "date": "2024-12-03T09:23:08Z" },
               "contactInfo": {
                 "type": "ContactInfo",
                 "contactPerson": "Fionn Iversen",
                 "organization": "NORCE Norwegian Research Centre",
                 "email": "fiiv@norceresearch.no",
                 "phone": "+4741669423"
               },
               "fundingAmount": {
                 "type": "FundingAmount",
                 "currency": "NOK",
                 "value": 350000.0
               },
               "method": {
                 "en": "<p>Research questions will be addressed by multidisciplinary approaches bridging and combining natural and social sciences. HyValue will develop new catalysts and materials beyond the state of art for the hydrogen production processes. In doing so, HyValue will benefit from the team’s competitive advantage in the development and use of predictive computational methods, including the sole de novo method experimentally validated in design of inorganic molecules, to reduce the time and cost of materials and catalyst discovery. Whenever possible, promising predictions will be followed up by high-throughput synthesis and testing, further speeding up discovery. The new catalysts and materials will feed into the process-development tasks and help unleash breakthroughs in novel H2 and NH3 production methods.</p>\\r\\n\\r\\n<p>Laboratory experimentswillserve as basis for processsimulationand numericalmodels combined with equipment testing in close cooperation with vendorsand end-users. Socio-technological status willbe combined withlegal doctrinal methods,cross-industrial comparative analysisandregulation theorytoaddress regulatory barriers fortechnology implementation.</p>\\r\\n\\r\\n<p>One will rely on the use of tools from several engineering disciplines, together with tools from basic sciences as well as economy. Initial studies will be done as laboratory or computer experiments, depending on the setting. Data will be gathered from real life as far as possible, and input from industrial user partners will be an important part in the work. Towards qualification for real implementation, the facilities of relevant Norwegian catapults will be exploited.</p>\\r\\n\\r\\n<p>Literature reviews, surveys and interviews, stakeholder workshops and expert panels, laboratory-scale experiments, large-scale experimental campaigns (commissioned), blind-prediction benchmark studies, risk assessment benchmark studies for hypothetical (or actual) systems, as well as intervention and case studies will be applied.</p>\\r\\n\\r\\n<p>We will use a sociotechnical and political-economic approach that is combined with institutional theory. While the Technology Readiness Level (TRL) methodology often comes short when encountering societal obstacles, the Societal Embeddedness Level (SEL) is a valuable methodology which specifically addresses the societal conditions to be in assessed before deploying a technological innovation. Triangulation of societal science methods is essential, including transdisciplinary living labs. These are arenas for experimentation and co-creation of new insight between stakeholders and for experimentation on transitions toward sustainability.</p>\\r\\n\\r\\n<p>HyValue will combine data gathering, including case studies, economic modelling, and political legal theory. Input data from all HyValue work packages will be integrated to provide new legal, financial, political sciences and economic knowledge directed to facilitate new hydrogen value chains. We resort to modern law and institutional economics: that green innovation and business could be driven by a carbon price only, that complementary investments in value chains shape business models, regulation and finance, and that knowledge and development is geo- and agglomeration dependent.</p>\\r\\n\\r\\n<p>The legal dogmatic method will be used to identify and interpret legal sources at different levels and legal issues will be discussed through law in context perspectives . Finally, we apply a double functional comparative method, in connection to other countries as well as comparing solutions found in the governance of other energy sources.</p>\\r\\n\\r\\n<p>Lastly, we will use GIS to tie our work together: complementary investments, hubs, deliveries in value chains lending emphasis to agglomeration, knowledge and risk, all of which relate to geography.</p>\\r\\n"
               },
               "equipment": {
                 "en": "<p>Partner institutions are equipped with state-of-the-art laboratories and experimental infrastructure:</p>\\r\\n\\r\\n<p>HTE&#64;UiB Robotic high-throughput experimentation</p>\\r\\n\\r\\n<p>Risavika Test Centre Demo-scale renewable energy production processing</p>\\r\\n\\r\\n<p>Monash U. labs Leading infrastructure for electromaterials/chemistry</p>\\r\\n\\r\\n<p>NMP (UiB) The Norwegian NMR platform, incl. 850 MHz instrument</p>\\r\\n\\r\\n<p>UiB/HVL/UiS labs Completely equipped for synthesis/characterization</p>\\r\\n\\r\\n<p>SERL (NORCE) Lab-scale models of subsurface hydrogen flow</p>\\r\\n\\r\\n<p>HySALA (UiB) Lab-scale investigation of ignition and combust. phenomena</p>\\r\\n\\r\\n<p>HyValue will also benefit from close collaboration with other national research infrastructure.</p>\\r\\n"
               },
               "projectCategories": [
                 {
                   "type": "APPLIEDRESEARCH",
                   "label": { "en": "Applied Research", "nb": "Anvendt forskning" }
                 }
               ],
               "keywords": [
                 {
                   "type": "3025",
                   "label": {
                     "en": "Electrolytic hydrogen production",
                     "nb": "Elektrolytisk hydrogenproduksjon"
                   }
                 },
                 {
                   "type": "4644",
                   "label": {
                     "en": "Sociology of science and technology",
                     "nb": "Teknologisosiologi"
                   }
                 },
                 { "type": "4837", "label": { "en": "Supply chain", "nb": "Verdikjede" } },
                 {
                   "type": "4987",
                   "label": { "en": "Hydrogen", "nb": "Hydrogen", "nn": "Hydrogen" }
                 },
                 {
                   "type": "11826",
                   "label": { "en": "Fuel cells", "nb": "Brenscelceller" }
                 },
                 {
                   "type": "18372",
                   "label": {
                     "en": "Hydrogen storage",
                     "nb": "Hydrogenlagring",
                     "nn": "Hydrogenlagring"
                   }
                 },
                 { "type": "30435", "label": { "en": "Fuel", "nb": "Drivstoff" } },
                 {
                   "type": "41605",
                   "label": {
                     "en": "Sustainable business models",
                     "nb": "Bærekraftige forretningsmodeller"
                   }
                 },
                 {
                   "type": "44747",
                   "label": { "en": "Consumer trust", "nb": "Forbrukertillit" }
                 },
                 {
                   "type": "53380",
                   "label": { "en": "Hydrogen safety", "nb": "Hydrogensikkerhet" }
                 }
               ],
               "externalSources": [],
               "relatedProjects": [
                 "https://api.nva.unit.no/cristin/project/2615922",
                 "https://api.nva.unit.no/cristin/project/2728252",
                 "https://api.nva.unit.no/cristin/project/2558347",
                 "https://api.nva.unit.no/cristin/project/2547317"
               ],
               "institutionsResponsibleForResearch": [],
               "approvals": [],
               "creator": {
                 "identity": {
                   "type": "Person",
                   "id": "https://api.nva.unit.no/cristin/person/26636",
                   "firstName": "Fionn",
                   "lastName": "Iversen"
                 },
                 "roles": [
                   {
                     "type": "ProjectCreator",
                     "affiliation": {
                       "type": "Organization",
                       "id": "https://api.nva.unit.no/cristin/organization/2057.0.0.0"
                     }
                   }
                 ]
               },
               "webPage": "hyvalue.no",
               "@context": "https://bibsysdev.github.io/src/project-context.json"
             }
            """.formatted(id, fundingsArray);
    }

    private static void fakePublisherResponse(FakeUriRetriever fakeUriRetriever, Book book) {
        if (nonNull(book.getPublisher()) && book.getPublisher() instanceof Publisher publisher) {
            var id = publisher.getId();
            fakeUriRetriever.registerResponse(id, SC_OK, APPLICATION_JSON_LD, createPublisher(id));
        }
    }

    private static void fakeSeriesResponse(FakeUriRetriever fakeUriRetriever, Book book) {
        if (book.getSeries() instanceof Series series) {
            var seriesId = series.getId();
            fakeUriRetriever.registerResponse(seriesId, SC_OK, APPLICATION_JSON_LD, createSeries(seriesId));
        }
    }

    private static void fakePendingNviResponse(FakeUriRetriever fakeUriRetriever, Publication publication) {
        setUpNviResponse(fakeUriRetriever, SC_OK, publication, getPendingNviResponseString());
    }

    public static String createCristinOrganizationResponseForTopLevelOrg(URI uri) {
        return """
            {
                           "@context" : "https://bibsysdev.github.io/src/organization-context.json",
                           "type" : "Organization",
                           "id" : "%s",
                           "labels" : {
                             "en" : "Norwegian Centre for Mathematics Education",
                             "nb" : "Nasjonalt senter for matematikk i opplæringen"
                           },
                           "acronym" : "SU-ILU-NSM",
                           "country" : "NO",
                           "hasPart" : [ {
                             "type" : "Organization",
                             "id" : "%s",
                             "labels" : {
                               "en" : "Department of Teacher Education",
                               "nb" : "Institutt for lærerutdanning"
                             },
                             "acronym" : "SU-ILU",
                             "country" : "NO",
                             "hasPart" : [ {
                               "type" : "Organization",
                               "id" : "%s",
                               "labels" : {
                                 "en" : "Faculty of Social and Educational Sciences",
                                 "nb" : "Fakultet for samfunns- og utdanningsvitenskap"
                               },
                               "acronym" : "SU",
                               "country" : "NO",
                               "partOf" : [ ],
                               "hasPart" : [ ]
                             } ],
                             "partOf" : [ ]
                           } ],
                           "hasPart" : [ ]
                         }
            """.formatted(uri, HARD_CODED_LEVEL_2_ORG_URI, HARD_CODED_LEVEL_3_ORG_URI);
    }

    private static String createCustomerApiResponse() {
        return """
            {
                "not": "relevant"
            }
            """;
    }

    private static String createCristinOrganizationResponse(URI uri) {
        return """
            {
                             "@context" : "https://bibsysdev.github.io/src/organization-context.json",
                             "type" : "Organization",
                             "id" : "%s",
                             "labels" : {
                               "en" : "Department of Teacher Education",
                               "nb" : "Institutt for lærerutdanning"
                             },
                             "acronym" : "SU-ILU",
                             "country" : "NO",
                             "partOf" : [ {
                               "type" : "Organization",
                               "id" : "%s",
                               "labels" : {
                                 "en" : "Faculty of Social and Educational Sciences",
                                 "nb" : "Fakultet for samfunns- og utdanningsvitenskap"
                               },
                               "acronym" : "SU",
                               "country" : "NO",
                               "partOf" : [ {
                                 "type" : "Organization",
                                 "id" : "%s",
                                 "labels" : {
                                   "en" : "Norwegian University of Science and Technology",
                                   "nb" : "Norges teknisk-naturvitenskapelige universitet",
                                   "nn" : "Noregs teknisk-naturvitskaplege universitet"
                                 },
                                 "acronym" : "NTNU",
                                 "country" : "NO",
                                 "partOf" : [ ],
                                 "hasPart" : [ ]
                               } ],
                               "hasPart" : [ ]
                             } ],
                             "hasPart" : [ ]
                           } ],
                           "hasPart" : [ ]
                         }
            """.formatted(uri, HARD_CODED_LEVEL_2_ORG_URI, HARD_CODED_TOP_LEVEL_ORG_URI);
    }

    private static String createJournal(URI id) {
        return """
            {
              "id" : "%s",
              "name" : "Test (Madrid)",
              "onlineIssn" : "1863-8260",
              "printIssn" : "1133-0686",
              "scientificValue" : "LevelOne",
              "sameAs" : "https://example.org/KanalTidsskriftInfo?pid=D4781C26-15BD-4CD2-BC2D-03C19B112134",
              "type" : "Journal",
              "@context" : "https://bibsysdev.github.io/src/publication-channel/channel-context.json"
            }
            """.formatted(id);
    }

    private static String createPublisher(URI uri) {
        return """
            {
              "id" : "%s",
              "identifier" : "91CE8D38-5C23-4A97-A429-EDECBD8586B5",
              "name" : "Universitetet i Sørøst-Norge/Universitetet i Søraust-Noreg",
              "scientificValue" : "Unassigned",
              "sameAs" : "%s",
              "year" : "2020",
              "type" : "Publisher",
              "@context" : "https://bibsysdev.github.io/src/publication-channel/channel-context.json"
            }
            """.formatted(uri, CHANNEL_SAME_AS);
    }

    public static String createSeries(URI seriesId) {
        return """
            {
              "id" : "%s",
              "identifier" : "127CA877-01F1-4F0D-B97A-BE9BD81B0A9C",
              "name" : "Skriftserien fra Universitetet i Sørøst-Norge",
              "printIssn" : "2535-5325",
              "scientificValue" : "Unassigned",
              "sameAs" : "%s",
              "year" : "2020",
              "type" : "Series",
              "@context" : "https://bibsysdev.github.io/src/publication-channel/channel-context.json"
            }
            """.formatted(seriesId, CHANNEL_SAME_AS);
    }

    private static String getPendingNviResponseString() {
        return PENDING_NVI_RESPONSE;
    }

    private static URI createNviCandidateUri(String id) {
        var urlEncodedPublicationId = URLEncoder.encode(id, StandardCharsets.UTF_8);
        var uri = UriWrapper.fromHost(API_HOST)
                      .addChild("scientific-index")
                      .addChild("publication")
                      .getUri();
        return URI.create(String.format("%s/%s/%s", uri, urlEncodedPublicationId, "report-status"));
    }

    private static URI getPublicationId(Publication publication) {
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                   .addChild("publication")
                   .addChild(publication.getIdentifier().toString()).getUri();
    }
}
