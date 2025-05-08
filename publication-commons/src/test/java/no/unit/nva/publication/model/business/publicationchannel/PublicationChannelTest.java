package no.unit.nva.publication.model.business.publicationchannel;

import static java.util.UUID.randomUUID;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.model.business.publicationchannel.PublicationChannelUtil.toChannelClaimUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.clients.ChannelClaimDto;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim.ChannelConstraint;
import no.unit.nva.clients.ChannelClaimDto.CustomerSummaryDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.PublicationChannelDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PublicationChannelTest extends ResourcesLocalTest {

    private final int NUMBER_OF_RESOURCES = 2000;
    private final int ONE = 1;
    private ResourceService resourceService;
    private IdentityServiceClient identityService;

    public static Stream<Arguments> publicationChannelProvider() {
        return Stream.of(Arguments.of(randomNonClaimedPublicationChannel(), randomClaimedPublicationChannel()));
    }

    @BeforeEach
    public void init() {
        super.init();
        identityService = mock(IdentityServiceClient.class);
        resourceService = getResourceServiceBuilder().withIdentityService(identityService).build();
    }

    @ParameterizedTest
    @MethodSource("publicationChannelProvider")
    void shouldDoRoundTripWithoutLossOfInformation(PublicationChannel publicationChannel)
        throws JsonProcessingException {
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(publicationChannel);
        var roundTripped = JsonUtils.dtoObjectMapper.readValue(json, PublicationChannel.class);

        assertEquals(publicationChannel, roundTripped);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Publisher", "SerialPublication"})
    void shouldConvertStringToChannelType(String value) {
        var channelType = ChannelType.fromValue(value);

        assertEquals(value, channelType.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"OwnerOnly", "Everyone"})
    void shouldConvertStringToChannelPolicy(String value) {
        var channelPolicy = ChannelPolicy.fromValue(value);

        assertEquals(value, channelPolicy.getValue());
    }

    @ParameterizedTest
    @MethodSource("publicationChannelProvider")
    void shouldConvertPublicationChannelToDaoWithExpectedPrimaryPartitionAndSortKey(
        PublicationChannel publicationChannel) {
        var dao = PublicationChannelDao.fromPublicationChannel(publicationChannel);

        assertEquals("%s:%s".formatted("PublicationChannel", publicationChannel.getIdentifier()),
                     dao.getPrimaryKeyPartitionKey());
        assertEquals("%s:%s".formatted("Resource", publicationChannel.getResourceIdentifier()),
                     dao.getPrimaryKeySortKey());
    }

    @ParameterizedTest
    @MethodSource("publicationChannelProvider")
    void shouldConvertPublicationChannelToDaoWithExpectedByTypeAndIdentifierGSIPartitionAndSortKey(
        PublicationChannel publicationChannel) {
        var dao = PublicationChannelDao.fromPublicationChannel(publicationChannel);

        assertEquals("%s:%s".formatted("Resource", publicationChannel.getResourceIdentifier()),
                     dao.getByTypeAndIdentifierPartitionKey());
        assertEquals("%s:%s".formatted("PublicationChannel", publicationChannel.getIdentifier()),
                     dao.getByTypeAndIdentifierSortKey());
    }

    @Test
    void shouldPersistClaimedPublicationChannelWhenPublisherIsPresentAndChannelClaimExistsInIdentityService()
        throws BadRequestException, NotFoundException {
        var channelIdentifier = randomUUID();
        var publisherId = randomPublisherId(channelIdentifier);
        var publication = randomPublication(DegreeBachelor.class);
        publication.getEntityDescription().getReference().setPublicationContext(degreeWithPublisherId(publisherId));

        var claim = channelClaimDtoForPublisher(channelIdentifier);
        when(identityService.getChannelClaim(toChannelClaimId(channelIdentifier))).thenReturn(claim);
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(resourceService, UserInstance.fromPublication(publication));

        var fetchedResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());

        var actualPublicationChannel = fetchedResource.getPublicationChannels().getFirst();
        var expectedClaim = constructExpectedPublicationChannel(channelIdentifier, persistedPublication.getIdentifier(),
                                                                claim, actualPublicationChannel.getCreatedDate(),
                                                                actualPublicationChannel.getModifiedDate());

        assertEquals(expectedClaim, actualPublicationChannel);
    }

    @Test
    void shouldPersistNonClaimedPublicationChannelWhenPublisherIsPresentButChannelClaimDoesNotExistsInIdentityService()
        throws BadRequestException, NotFoundException {
        var channelIdentifier = randomUUID();
        var publisherId = randomPublisherId(channelIdentifier);
        var publication = randomPublication(DegreeBachelor.class);
        publication.getEntityDescription().getReference().setPublicationContext(degreeWithPublisherId(publisherId));

        when(identityService.getChannelClaim(toChannelClaimId(channelIdentifier)))
            .thenThrow(new NotFoundException(randomString()));
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(resourceService, UserInstance.fromPublication(publication));

        var fetchedResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());

        var actualPublicationChannel = fetchedResource.getPublicationChannels().getFirst();
        var expectedClaim = constructExpectedNonClaimedChannel(channelIdentifier, fetchedResource, actualPublicationChannel);

        assertEquals(expectedClaim, actualPublicationChannel);
    }

    @Test
    void shouldThrowExceptionWhenIdentityServiceRespondsWithUnhandledError() throws NotFoundException {
        var channelIdentifier = randomUUID();
        var publisherId = randomPublisherId(channelIdentifier);
        var publication = randomPublication(DegreeBachelor.class);
        publication.getEntityDescription().getReference().setPublicationContext(degreeWithPublisherId(publisherId));

        when(identityService.getChannelClaim(toChannelClaimId(channelIdentifier))).thenThrow(new RuntimeException(randomString()));

        assertThrows(IllegalStateException.class, () -> Resource.fromPublication(publication)
                                       .persistNew(resourceService, UserInstance.fromPublication(publication)));
    }

    @Test
    void shouldUpdatePublicationChannelWhenPublisherIsUpdated() throws BadRequestException, NotFoundException {
        var publisherId = randomPublisherId(randomUUID());
        var persistedPublication = persistDegreeWithPublisher(new Publisher(publisherId));

        var existingResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());
        var existingPublicationChannel = existingResource.getPublicationChannels().getFirst();
        assertEquals(getChannelClaimIdentifier(publisherId), existingPublicationChannel.getIdentifier().toString());

        var newChannelIdentifier = randomUUID();
        var newPublisherId = randomPublisherId(newChannelIdentifier);
        persistedPublication.getEntityDescription().getReference().setPublicationContext(degreeWithPublisherId(newPublisherId));

        when(identityService.getChannelClaim(toChannelClaimUri(newChannelIdentifier))).thenThrow(new NotFoundException(randomString()));
        resourceService.updatePublication(persistedPublication);

        var updatedResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());
        var updatedPublicationChannels = updatedResource.getPublicationChannels();

        assertEquals(ONE, updatedPublicationChannels.size());
        assertEquals(getChannelClaimIdentifier(newPublisherId),
                     updatedPublicationChannels.getFirst().getIdentifier().toString());
    }

    @Test
    void shouldRemovePublicationChannelWhenPublisherIsRemoved() throws BadRequestException, NotFoundException {
        var publisherId = randomPublisherId(randomUUID());
        var persistedPublication = persistDegreeWithPublisher(new Publisher(publisherId));

        var existingResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());
        assertFalse(existingResource.getPublicationChannels().isEmpty());

        persistedPublication.getEntityDescription().getReference().setPublicationContext(degreeWithPublisher(null));
        resourceService.updatePublication(persistedPublication);

        var updatedResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());
        assertTrue(updatedResource.getPublicationChannels().isEmpty());
    }

    @Test
    void shouldAddPublicationChannelWhenNoPublisherIsAdded() throws BadRequestException, NotFoundException {
        var persistedPublication = persistDegreeWithPublisher(null);

        var existingResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());
        assertTrue(existingResource.getPublicationChannels().isEmpty());

        var channelIdentifier = randomUUID();
        var publisherId = randomPublisherId(channelIdentifier);
        persistedPublication.getEntityDescription().getReference().setPublicationContext(degreeWithPublisherId(publisherId));

        var channelClaimDto = channelClaimDtoForPublisher(channelIdentifier);
        when(identityService.getChannelClaim(toChannelClaimUri(channelIdentifier))).thenReturn(channelClaimDto);
        resourceService.updatePublication(persistedPublication);

        var updatedResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());
        var updatedPublicationChannels = updatedResource.getPublicationChannels();

        assertEquals(ONE, updatedPublicationChannels.size());
        assertEquals(getChannelClaimIdentifier(publisherId),
                     updatedPublicationChannels.getFirst().getIdentifier().toString());
    }

    @Test
    void shouldReturnListingResultWhenListingPublicationChannels() throws NotFoundException {
        var channelIdentifier = randomUUID();
        var publisherId = randomPublisherId(channelIdentifier);
        var claim = channelClaimDtoForPublisher(channelIdentifier);

        persistResourcesPublicationChannels(NUMBER_OF_RESOURCES, publisherId, claim, channelIdentifier);

        var publicationChannelIdentifier = new SortableIdentifier(channelIdentifier.toString());
        var firstListingResult = resourceService.fetchAllPublicationChannelsByIdentifier(publicationChannelIdentifier, null);
        var secondListingResult =
            resourceService.fetchAllPublicationChannelsByIdentifier(publicationChannelIdentifier,
                                                                    firstListingResult.getStartMarker());
        var totalChannelsRetrieved = new ArrayList<PublicationChannel>();
        totalChannelsRetrieved.addAll(firstListingResult.getDatabaseEntries());
        totalChannelsRetrieved.addAll(secondListingResult.getDatabaseEntries());

        assertEquals(NUMBER_OF_RESOURCES, totalChannelsRetrieved.size());
    }

    private void persistResourcesPublicationChannels(int numberOfResources, URI publisherId, ChannelClaimDto claim,
                                                     UUID channelIdentifier) throws NotFoundException {
        when(identityService.getChannelClaim(toChannelClaimId(channelIdentifier))).thenReturn(claim);
        IntStream.range(0, numberOfResources)
            .forEach(i -> {
                var publication = randomPublication(DegreeBachelor.class);
                publication.getEntityDescription().getReference().setPublicationContext(degreeWithPublisherId(publisherId));
                var userInstance = UserInstance.fromPublication(publication);
                attempt(() -> resourceService.createPublication(userInstance, publication));
            });
    }

    private NonClaimedPublicationChannel constructExpectedNonClaimedChannel(UUID channelIdentifier, Resource fetchedResource,
                                                                            PublicationChannel actualPublicationChannel) {
        return new NonClaimedPublicationChannel(toChannelClaimId(channelIdentifier),
                                                ChannelType.PUBLISHER,
                                                new SortableIdentifier(channelIdentifier.toString()),
                                                fetchedResource.getIdentifier(),
                                                actualPublicationChannel.getCreatedDate(),
                                                actualPublicationChannel.getModifiedDate());
    }

    private static URI randomPublisherId(UUID channelIdentifier) {
        return UriWrapper.fromUri(randomUri())
                   .addChild("publication-channel-v2")
                   .addChild("publisher")
                   .addChild(channelIdentifier.toString())
                   .addChild(randomInteger().toString())
                   .getUri();
    }

    private static Degree degreeWithPublisherId(URI publisherId) {
        return attempt(() -> degreeWithPublisher(new Publisher(publisherId))).orElseThrow();
    }

    private static Degree degreeWithPublisher(Publisher publisher) {
        return attempt(() ->  new Degree(null, null, null, publisher, List.of(), null))
                   .orElseThrow();
    }

    private static ClaimedPublicationChannel randomClaimedPublicationChannel() {
        return new ClaimedPublicationChannel(randomUri(),
                                             randomUri(),
                                             randomUri(),
                                             new Constraint(ChannelPolicy.EVERYONE,
                                                            ChannelPolicy.OWNER_ONLY,
                                                            List.of(randomString())),
                                             ChannelType.PUBLISHER,
                                             SortableIdentifier.next(),
                                             SortableIdentifier.next(),
                                             Instant.now(),
                                             Instant.now());
    }

    private static NonClaimedPublicationChannel randomNonClaimedPublicationChannel() {
        return new NonClaimedPublicationChannel(randomUri(), ChannelType.PUBLISHER, SortableIdentifier.next(),
                                                SortableIdentifier.next(), Instant.now(), Instant.now());
    }

    private PublicationChannel constructExpectedPublicationChannel(UUID channelIdentifier,
                                                                   SortableIdentifier resourceIdentifier,
                                                                   ChannelClaimDto claim,
                                                                   Instant createdDate,
                                                                   Instant modifiedDate) {
        return new ClaimedPublicationChannel(toChannelClaimId(channelIdentifier),
                                             claim.claimedBy().id(),
                                             claim.claimedBy().organizationId(),
                                             new Constraint(ChannelPolicy.fromValue(claim.channelClaim().constraint().publishingPolicy()),
                                                            ChannelPolicy.fromValue(claim.channelClaim().constraint().editingPolicy()),
                                                            claim.channelClaim().constraint().scope()),
                                             ChannelType.PUBLISHER,
                                             new SortableIdentifier(channelIdentifier.toString()),
                                             resourceIdentifier,
                                             createdDate,
                                             modifiedDate);
    }

    private ChannelClaimDto channelClaimDtoForPublisher(UUID channelIdentifier) {
        return new ChannelClaimDto(toChannelClaimId(channelIdentifier),
                                   new CustomerSummaryDto(randomUri(), randomUri()),
                                   new ChannelClaim(randomPublisherId(channelIdentifier),
                                                    new ChannelConstraint("OwnerOnly", "Everyone", List.of(randomString()))));
    }

    private URI toChannelClaimId(UUID channelClaimIdentifier) {
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                   .addChild("customer")
                   .addChild("channel-claim")
                   .addChild(channelClaimIdentifier.toString())
                   .getUri();
    }

    private static String getChannelClaimIdentifier(URI publisher) {
        return UriWrapper.fromUri(publisher)
                   .replacePathElementByIndexFromEnd(0, StringUtils.EMPTY_STRING)
                   .getLastPathElement();
    }

    private Publication persistDegreeWithPublisher(Publisher publisher) throws NotFoundException, BadRequestException {
        var publication = randomPublication(DegreeBachelor.class);
        publication.getEntityDescription().getReference().setPublicationContext(degreeWithPublisher(publisher));

        if (publisher != null) {
            when(identityService.getChannelClaim(toChannelClaimUri(publisher.getIdentifier())))
                .thenThrow(new NotFoundException(randomString()));
        }

        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
}