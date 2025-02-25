package no.unit.nva.publication.uriretriever;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;
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
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
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
                                        ResourceService resourceService) {

        fakeContributorResponses(publication, fakeUriRetriever);
        fakeOwnerResponse(fakeUriRetriever, publication.getResourceOwner().getOwnerAffiliation());
        fakePendingNviResponse(fakeUriRetriever, publication);
        fakeFundingResponses(fakeUriRetriever, publication);
        fakeContextResponses(publication, fakeUriRetriever, resourceService);
        if (publication instanceof ImportCandidate) {
            createFakeCustomerApiResponse(fakeUriRetriever);
        } else {
            resourceService.updateResource(Resource.fromPublication(publication), UserInstance.fromPublication(publication));
        }
    }

    public static void setupFakeForType(TicketEntry ticket, FakeUriRetriever fakeUriRetriever) {
        var responsibilityArea = ticket.getResponsibilityArea();
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
                                             FakeUriRetriever fakeUriRetriever, ResourceService resourceService) {

        extractPublicationContext(publication)
            .ifPresent(publicationContext -> selectResponsesToFake(fakeUriRetriever,
                                                                   resourceService, publicationContext));
    }

    private static void selectResponsesToFake(FakeUriRetriever fakeUriRetriever,
                                              ResourceService resourceService, PublicationContext publicationContext) {
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
                URI id = journal.getId();
                fakeUriRetriever.registerResponse(id, SC_OK, APPLICATION_JSON_LD, createJournal(id));
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

    private static void fakeFundingResponse(FakeUriRetriever fakeUriRetriever, Funding funding) {
        var source = funding.getSource();
        if (funding instanceof ConfirmedFunding confirmedFunding) {
            var id = confirmedFunding.getId();
            fakeUriRetriever.registerResponse(id,
                                              SC_OK,
                                              APPLICATION_JSON_LD,
                                              confirmedFundingResponse(id, source));
        } else {

            fakeUriRetriever.registerResponse(source,
                                              SC_OK,
                                              APPLICATION_JSON_LD,
                                              unconfirmedFundingResponse(source));
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

    private static String unconfirmedFundingResponse(URI source) {
        return """
            {
              "type" : "UnconfirmedFunding",
              "source" : "%s",
              "identifier" : "NFR00011-12-3211"
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

    private static void fakePublisherResponse(FakeUriRetriever fakeUriRetriever, Book book) {
        if (nonNull(book.getPublisher()) && book.getPublisher() instanceof Publisher publisher) {
            var id = publisher.getId();
            fakeUriRetriever.registerResponse(id, SC_OK, APPLICATION_JSON_LD, createPublisher(id));
        }
    }

    private static void fakeSeriesResponse(FakeUriRetriever fakeUriRetriever, Book book) {
        if (book.getSeries() != null && book.getSeries() instanceof Series series) {
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
