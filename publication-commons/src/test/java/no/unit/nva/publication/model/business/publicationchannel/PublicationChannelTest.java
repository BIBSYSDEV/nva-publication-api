package no.unit.nva.publication.model.business.publicationchannel;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.clients.ChannelClaimDto;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim.ChannelConstraint;
import no.unit.nva.clients.ChannelClaimDto.CustomerSummaryDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.PublicationChannelDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.BadGatewayException;
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
        throws InvalidUnconfirmedSeriesException, BadRequestException, NotFoundException {
        var publisherId = randomPublisherId();
        var publication = randomPublication(DegreeBachelor.class);
        publication.getEntityDescription().getReference().setPublicationContext(degreeWithPublisher(publisherId));

        var claim = channelClaimDtoForPublisher();
        when(identityService.getChannelClaim(toChannelClaimUri(publisherId))).thenReturn(claim);
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(resourceService, UserInstance.fromPublication(publication));

        var fetchedResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());

        var actualPublicationChannel = fetchedResource.getPublicationChannels().getFirst();
        var expectedClaim = constructExpectedPublicationChannel(publisherId, persistedPublication.getIdentifier(),
                                                                claim, actualPublicationChannel.getCreatedDate(),
                                                                actualPublicationChannel.getModifiedDate());

        assertEquals(expectedClaim, actualPublicationChannel);
    }

    @Test
    void shouldPersistNonClaimedPublicationChannelWhenPublisherIsPresentButChannelClaimDoesNotExistsInIdentityService()
        throws InvalidUnconfirmedSeriesException, BadRequestException, NotFoundException {
        var publisherId = randomPublisherId();
        var publication = randomPublication(DegreeBachelor.class);
        publication.getEntityDescription().getReference().setPublicationContext(degreeWithPublisher(publisherId));

        when(identityService.getChannelClaim(toChannelClaimUri(publisherId))).thenThrow(new NotFoundException(randomString()));
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(resourceService, UserInstance.fromPublication(publication));

        var fetchedResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());

        var actualPublicationChannel = fetchedResource.getPublicationChannels().getFirst();
        var expectedClaim = constructExpectedNonClaimedChannel(publisherId, fetchedResource, actualPublicationChannel);

        assertEquals(expectedClaim, actualPublicationChannel);
    }

    @Test
    void shouldThrowExceptionWhenIdentityServiceRespondsWithUnhandledError()
        throws InvalidUnconfirmedSeriesException, NotFoundException {
        var publisherId = randomPublisherId();
        var publication = randomPublication(DegreeBachelor.class);
        publication.getEntityDescription().getReference().setPublicationContext(degreeWithPublisher(publisherId));

        when(identityService.getChannelClaim(toChannelClaimUri(publisherId))).thenThrow(new RuntimeException(randomString()));

        assertThrows(IllegalStateException.class, () -> Resource.fromPublication(publication)
                                       .persistNew(resourceService, UserInstance.fromPublication(publication)));
    }

    private NonClaimedPublicationChannel constructExpectedNonClaimedChannel(URI publisherId, Resource fetchedResource,
                                                          PublicationChannel actualPublicationChannel) {
        return new NonClaimedPublicationChannel(toChannelClaimUri(publisherId), ChannelType.PUBLISHER,
                                                new SortableIdentifier(getChannelClaimIdentifier(publisherId)),
                                                fetchedResource.getIdentifier(),
                                                actualPublicationChannel.getCreatedDate(),
                                                actualPublicationChannel.getModifiedDate());
    }

    private static URI randomPublisherId() {
        return UriWrapper.fromUri(randomUri())
                   .addChild("publication-channel-v2")
                   .addChild("publisher")
                   .addChild(UUID.randomUUID().toString())
                   .addChild(randomInteger().toString())
                   .getUri();
    }

    private static Degree degreeWithPublisher(URI publisher) throws InvalidUnconfirmedSeriesException {
        return new Degree(null, null, null, new Publisher(publisher), List.of(), null);
    }

    private static ClaimedPublicationChannel randomClaimedPublicationChannel() {
        return new ClaimedPublicationChannel(randomUri(), randomUri(), randomUri(),
                                             new Constraint(ChannelPolicy.EVERYONE, ChannelPolicy.OWNER_ONLY,
                                                            List.of(randomString())), ChannelType.PUBLISHER,
                                             SortableIdentifier.next(), SortableIdentifier.next(), Instant.now(),
                                             Instant.now());
    }

    private static NonClaimedPublicationChannel randomNonClaimedPublicationChannel() {
        return new NonClaimedPublicationChannel(randomUri(), ChannelType.PUBLISHER, SortableIdentifier.next(),
                                                SortableIdentifier.next(), Instant.now(), Instant.now());
    }

    private PublicationChannel constructExpectedPublicationChannel(URI publisherId, SortableIdentifier resourceIdentifier,
                                                                   ChannelClaimDto claim, Instant createdDate,
                                                                   Instant modifiedDate) {
        return new ClaimedPublicationChannel(toChannelClaimUri(publisherId), claim.claimedBy().id(), claim.claimedBy()
                                                                                                         .organizationId(),
                                             new Constraint(ChannelPolicy.fromValue(claim.channelClaim().constraint().publishingPolicy()),
                                                            ChannelPolicy.fromValue(claim.channelClaim().constraint().editingPolicy()),
                                                            claim.channelClaim().constraint().scope()),
                                             ChannelType.PUBLISHER,
                                             new SortableIdentifier(getChannelClaimIdentifier(publisherId)),
                                             resourceIdentifier, createdDate, modifiedDate);
    }

    private ChannelClaimDto channelClaimDtoForPublisher() {
        return new ChannelClaimDto(new CustomerSummaryDto(randomUri(), randomUri()), new ChannelClaim(randomPublisherId(),
                                                                                                      new ChannelConstraint(
                                                                                                          "OwnerOnly",
                                                                                                          "Everyone",
                                                                                                          List.of(
                                                                                                              randomString()))));
    }

    private URI toChannelClaimUri(URI publisher) {
        var channelClaimIdentifier = getChannelClaimIdentifier(publisher);
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                   .addChild("customer")
                   .addChild("channel-claim")
                   .addChild(channelClaimIdentifier)
                   .getUri();
    }

    private static String getChannelClaimIdentifier(URI publisher) {
        return UriWrapper.fromUri(publisher)
                   .replacePathElementByIndexFromEnd(0, StringUtils.EMPTY_STRING)
                   .getLastPathElement();
    }
}