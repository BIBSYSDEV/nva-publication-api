package no.unit.nva.publication.events.handlers.batch;

import static java.util.UUID.randomUUID;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.RelationType;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Book.BookBuilder;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.publication.model.ResourceWithId;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.SearchService;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdatePublicationsInBatchesHandlerTest extends ResourcesLocalTest {

    private static final Context CONTEXT = mock(Context.class);
    private ManuallyUpdatePublicationsHandler handler;
    private ByteArrayOutputStream output;
    private ResourceService resourceService;
    private UriRetriever uriRetriever;

    @BeforeEach
    public void setUp() {
        super.init();
        output = new ByteArrayOutputStream();
        resourceService = getResourceServiceBuilder().build();
        uriRetriever = mock(UriRetriever.class);
        handler = new ManuallyUpdatePublicationsHandler(SearchService.create(uriRetriever, resourceService),
                                                        resourceService);
    }

    @Test
    void shouldUpdatePublicationPublisherIdWhenUpdateTypeIsPublisherAndPublisherIdIsProvidedInRequest()
        throws IOException {
        var publisherIdentifier = randomUUID().toString();
        var newPublisherIdentifier = randomUUID().toString();
        var publisherId = createPublisherIdWithIdentifier(publisherIdentifier);
        var publicationsToUpdate = createMultiplePublicationsWithPublisher(publisherId);
        var event = createEvent(ManualUpdateType.PUBLISHER, publisherIdentifier, newPublisherIdentifier);

        mockSearchApiResponseWithPublications(publicationsToUpdate);

        handler.handleRequest(event, output, CONTEXT);

        publicationsToUpdate.forEach(publication -> {
            var updatedPublication = getPublicationByIdentifier(publication);
            var updatedPublisher = getPublisher(updatedPublication);
            var expectedPublisher = publisherId.toString().replace(publisherIdentifier, newPublisherIdentifier);

            assertEquals(URI.create(expectedPublisher), updatedPublisher);
        });
    }

    @Test
    void shouldNotUpdatePublisherWhenPublisherUpdateAndPublicationAndRequestHaveDifferentPublishers()
        throws IOException {
        var publisherIdToKeep = createPublisherIdWithIdentifier(randomUUID().toString());
        var publicationsToUpdate = createMultiplePublicationsWithPublisher(publisherIdToKeep);
        var event = createEvent(ManualUpdateType.PUBLISHER, randomUUID().toString(), randomUUID().toString());

        mockSearchApiResponseWithPublications(publicationsToUpdate);

        handler.handleRequest(event, output, CONTEXT);

        publicationsToUpdate.forEach(publication -> {
            var updatedPublication = getPublicationByIdentifier(publication);
            var updatedPublisherId = getPublisher(updatedPublication);

            assertEquals(publisherIdToKeep, updatedPublisherId);
        });
    }

    @Test
    void shouldUpdateFileLicenseUriWhenUpdateTypeIsLicenseAndLicenseUriIsProvidedInRequestAndMatchesFileLicense()
        throws IOException {
        var license = randomUri();
        var newLicense = randomUri();
        var publicationsToUpdate = createMultiplePublicationsWithLicense(license);
        var event = createEvent(ManualUpdateType.LICENSE, license.toString(), newLicense.toString());

        mockSearchApiResponseWithPublications(publicationsToUpdate);

        handler.handleRequest(event, output, CONTEXT);

        publicationsToUpdate.forEach(publication -> {
            var updatedPublication = getPublicationByIdentifier(publication);

            assertEquals(publication.getAssociatedArtifacts().size(),
                         updatedPublication.getAssociatedArtifacts().size());

            var updatedFiles = getFiles(updatedPublication);

            updatedFiles.forEach(file -> assertEquals(newLicense, file.getLicense()));
        });
    }

    @Test
    void shouldNotUpdateFileLicenseUriWhenUpdateTypeIsLicenseAndLicenseUriIsNotEqualProvidedInRequestLicense()
        throws IOException {
        var license = randomUri();
        var publicationsToUpdate = createMultiplePublicationsWithLicense(license);
        var event = createEvent(ManualUpdateType.LICENSE, randomString(), randomString());

        mockSearchApiResponseWithPublications(publicationsToUpdate);

        handler.handleRequest(event, output, CONTEXT);

        publicationsToUpdate.forEach(publication -> {
            var updatedPublication = getPublicationByIdentifier(publication);

            assertEquals(publication.getAssociatedArtifacts().size(),
                         updatedPublication.getAssociatedArtifacts().size());

            var updatedFiles = getFiles(updatedPublication);

            updatedFiles.forEach(file -> assertEquals(license, file.getLicense()));
        });
    }

    private static List<File> getFiles(Publication updatedPublication) {
        return updatedPublication.getAssociatedArtifacts().stream().filter(File.class::isInstance)
                   .map(File.class::cast).toList();
    }

    private static InputStream createEvent(ManualUpdateType type, String oldValue, String newValue) {
        return IoUtils.stringToStream(new ManuallyUpdatePublicationsRequest(type, oldValue, newValue,
                                                                            Map.of("publisher",
                                                                                   oldValue)).toJsonString());
    }

    private static URI createPublisherIdWithIdentifier(String publisherIdentifier) {
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                   .addChild(publisherIdentifier)
                   .addChild(randomString())
                   .getUri();
    }

    private static URI getPublisher(Publication updatedPublication) {
        var book = (Book) updatedPublication.getEntityDescription().getReference().getPublicationContext();
        var publisher = (Publisher) book.getPublisher();
        return publisher.getId();
    }

    private static URI createPublicationId(String identifier) {
        return UriWrapper.fromUri(randomUri()).addChild(identifier).getUri();
    }

    private static List<ResourceWithId> convertToResourcesWithId(List<Publication> publicationList) {
        return publicationList.stream()
                   .map(Publication::getIdentifier)
                   .map(SortableIdentifier::toString)
                   .map(UpdatePublicationsInBatchesHandlerTest::createPublicationId)
                   .map(ResourceWithId::new)
                   .toList();
    }

    private Publication getPublicationByIdentifier(Publication publication) {
        return attempt(() -> resourceService.getPublicationByIdentifier(publication.getIdentifier())).orElseThrow();
    }

    private List<Publication> createMultiplePublicationsWithLicense(URI license) {
        return IntStream.range(0, 10).boxed().map(i -> createPublicationWithLicense(license)).toList();
    }

    private Publication createPublicationWithLicense(URI license) {
        var publication = randomPublication();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(randomFileWithLicense(license)));
        return attempt(() -> resourceService.createPublication(UserInstance.fromPublication(publication),
                                                               publication)).orElseThrow();
    }

    private List<AssociatedArtifact> randomFileWithLicense(URI license) {
        return List.of(File.builder().withLicense(license).withIdentifier(randomUUID()).buildOpenFile(),
                       File.builder().withLicense(license).withIdentifier(randomUUID()).buildInternalFile(),
                       File.builder().withLicense(license).withIdentifier(randomUUID()).buildPendingInternalFile(),
                       new AssociatedLink(randomUri(), randomString(), randomString(), RelationType.SAME_AS));
    }

    private List<Publication> createMultiplePublicationsWithPublisher(URI publisherId) {
        return IntStream.range(0, 10).boxed().map(i -> createPublicationWithPublisher(publisherId)).toList();
    }

    private void mockSearchApiResponseWithPublications(List<Publication> publicationList) {
        var resourcesWithId = convertToResourcesWithId(publicationList);
        var responseBody = new SearchResourceApiResponse(publicationList.size(), resourcesWithId);
        var response = httpResponse(200, responseBody.toJsonString());
        when(uriRetriever.fetchResponse(any(), any())).thenReturn(Optional.of(response));
    }

    private HttpResponse<String> httpResponse(int statusCode, String responseBody) {
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(responseBody);
        return response;
    }

    private Publication createPublicationWithPublisher(URI publisherId) {
        var publication = randomPublication();
        publication.getEntityDescription()
            .getReference()
            .setPublicationContext(new BookBuilder().withPublisher(new Publisher(publisherId)).build());
        return attempt(() -> resourceService.createPublication(UserInstance.fromPublication(publication),
                                                               publication)).orElseThrow();
    }
}