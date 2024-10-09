package no.unit.nva.publication.uriretriever;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public class FakeUriResponse {

    private static final ObjectMapper OBJECT_MAPPER = JsonUtils.dtoObjectMapper;
    private static final String API_HOST = new Environment().readEnv("API_HOST");
    private static final String PENDING_NVI_RESPONSE = """
        {
          "type": "NviCandidateResponse",
          "status": "Pending",
          "period": {
           "type": "Period",
           "id": "https://example.org/period/1233",
           "year": "2024"
          }
        }""";
    private static final URI CHANNEL_SAME_AS =
        URI.create("https://kanalregister.hkdir.no/publiseringskanaler/KanalForlagInfo"
                   + "?pid=91CE8D38-5C23-4A97-A429-EDECBD8586B5");

    private FakeUriResponse() {
        // NO-OP
    }

    public static void setupFakeForType(Publication publication,
                                        FakeUriRetriever fakeUriRetriever) throws JsonProcessingException {

        publication.getEntityDescription().getContributors().stream()
            .map(Contributor::getAffiliations)
            .flatMap(Collection::stream)
            .map(Organization.class::cast)
            .map(Organization::getId)
            .filter(Objects::nonNull)
            .forEach(i -> fakeUriRetriever.registerResponse(i, 200, MediaType.JSON_UTF_8,
                                                            createCristinOrganizationResponse(i)));

        fakePendingNviResponse(fakeUriRetriever, publication);
        var publicationContext = publication.getEntityDescription().getReference().getPublicationContext();

        if (publicationContext instanceof Anthology anthologyContext) {
            var contextUri = anthologyContext.getId();
            var parent = randomPublication(BookAnthology.class).copy()
                             .withIdentifier(SortableIdentifier.fromUri(contextUri))
                             .build();
            var parentResponse = PublicationResponse.fromPublication(parent);
            var anthology = OBJECT_MAPPER.writeValueAsString(parentResponse);
            var book = ((Book) parentResponse.getEntityDescription().getReference().getPublicationContext());
            fakeUriRetriever.registerResponse(contextUri, 200, APPLICATION_JSON_LD, anthology);
            fakePendingNviResponse(fakeUriRetriever, parent);
            fakePublisherResponse(fakeUriRetriever, book);
            fakeSeriesResponse(fakeUriRetriever, book);
        } else if (publicationContext instanceof Journal journal) {
            URI id = journal.getId();
            fakeUriRetriever.registerResponse(id, 200, APPLICATION_JSON_LD, createJournal(id));
        } else if (publicationContext instanceof Publisher publisher) {
            URI id = publisher.getId();
            fakeUriRetriever.registerResponse(id, 200, APPLICATION_JSON_LD, createPublisher(id));
        }
    }

    /**
     * This allows an override of the default value for the response.
     * @param fakeUriRetriever The faked URI retrieval object.
     * @param statusCode The desired response status code.
     * @param publication The source of the URI for which we mock the response.
     * @param response The desired response in JSON.
     */
    public static void setUpNviResponse(FakeUriRetriever fakeUriRetriever, int statusCode, Publication publication,
                                        String response) {
        var id = PublicationResponse.fromPublication(publication).getId();
        fakeUriRetriever.registerResponse(createNviCandidateUri(id.toString()), statusCode, MediaType.JSON_UTF_8,
                                          response);
    }

    private static void fakePublisherResponse(FakeUriRetriever fakeUriRetriever, Book book) {
        var publisher = ((Publisher) book.getPublisher()).getId();
        fakeUriRetriever.registerResponse(publisher, 200, APPLICATION_JSON_LD, createPublisher(publisher));
    }

    private static void fakeSeriesResponse(FakeUriRetriever fakeUriRetriever, Book book) {
        var seriesId = ((Series) book.getSeries()).getId();
        fakeUriRetriever.registerResponse(seriesId, 200, APPLICATION_JSON_LD, createSeries(seriesId));
    }

    private static void fakePendingNviResponse(FakeUriRetriever fakeUriRetriever, Publication publication) {
        setUpNviResponse(fakeUriRetriever, 200, publication, getPendingNviResponseString());
    }

    private static String createCristinOrganizationResponse(URI uri) {
        return """
            {
              "id": "%s",
              "type": "Organization",
              "labels": {
                "en": "Some organization",
                "nb": "En organisasjon"
              }
            }
            """.formatted(uri);
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

    private static String createSeries(URI seriesId) {
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
        var publicationId = URLEncoder.encode(id, StandardCharsets.UTF_8);
        var uri = UriWrapper.fromHost(API_HOST)
                      .addChild("scientific-index")
                      .addChild("candidate")
                      .addChild("publication")
                      .getUri();
        return URI.create(String.format("%s/%s", uri, publicationId));
    }
}
