package no.unit.nva.publication.utils;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.paths.UriWrapper;

public class CsvGeneratorTestClient {

    private final URI baseUri;

    public CsvGeneratorTestClient(URI baseUri) {
        this.baseUri = baseUri;
    }

    public void exportToCsv() throws IOException, InterruptedException {
        fetchPublications();
    }

    private void fetchPublications() throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder()
                             .connectTimeout(Duration.ofSeconds(3))
                             .build();

        int from = 0;
        int results = 20;

        int count;
        var allDocuments = new ArrayList<JsonNode>();
        do {
            System.out.printf("From %d, %d%n", from, results);
            var documents = fetchPage(httpClient, from, results);
            allDocuments.addAll(documents);
            count = documents.size();
            from += results;
        } while (count >= results && from <= 5000);

        try (var publicationDataSetGenerator = new PublicationDataSetGenerator()) {
            allDocuments.forEach(publicationDataSetGenerator::addEntry);
            publicationDataSetGenerator.exportToFile();
        }
    }

    private List<JsonNode> fetchPage(HttpClient httpClient, int from, int results)
        throws IOException, InterruptedException {
        var pageUri = UriWrapper.fromUri(baseUri)
                          .addQueryParameter("from", Integer.toString(from))
                          .addQueryParameter("results", Integer.toString(results))
//                          .addQueryParameter("orderBy", "modifiedDate")
//                          .addQueryParameter("sortOrder", "desc")
                          .getUri();
        var httpRequest = HttpRequest.newBuilder(pageUri)
                              .GET()
                              .header("Accept", "application/json")
                              .build();

        var response = httpClient.send(httpRequest, BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            var body = response.body();
            var rootNode = JsonUtils.dtoObjectMapper.readTree(body);
            var hitsNode = rootNode.get("hits");
            var iterator = hitsNode.elements();
            var documents = new ArrayList<JsonNode>();
            while (iterator.hasNext()) {
                var document = iterator.next();
                System.out.printf("Found %s\n", document.at("/id").asText());
                documents.add(document);
            }

            return documents;
        } else {
            throw new RuntimeException(String.format("Got status code %d calling %s!", response.statusCode(),
                                                     response.uri().toString()));
        }
    }

    public static void main(String... args) throws IOException, InterruptedException {
        //var client = new CsvGeneratorTestClient(URI.create("https://api.dev.nva.aws.unit.no/search/resources"));
        var client = new CsvGeneratorTestClient(URI.create("https://api.nva.unit.no/search/resources"));
        client.exportToCsv();
    }
}
