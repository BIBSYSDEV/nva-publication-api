package no.unit.nva.doi;

import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.model.Customer;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.journal.JournalReview;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.transformer.dto.CreatorDto;
import no.unit.nva.transformer.dto.DataCiteMetadataDto;
import no.unit.nva.transformer.dto.IdentifierDto;
import no.unit.nva.transformer.dto.PublisherDto;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@WireMockTest(httpsEnabled = true)
public class DataCiteMetadataDtoMapperTest {

    public static final String MAIN_TITLE = "MainTitle";
    public static final String SOME_ISSUE = "1";
    public static final String SOME_ARTICLE_NUMBER = "1";
    public static final String SOME_VOLUME = "1";
    public static final String SOME_YEAR = "1991";
    public static final String SOME_MONTH = "12";
    public static final String SOME_DAY = "52";
    public static final int NOT_THE_CREATOR = 2;
    public Organization somePublisher;
    public static final int THE_CREATOR = 1;
    public static final String THE_CREATOR_NAME = "TheCreatorName";
    public static final String NOT_THE_CREATOR_NAME = "notTheCreatorName";
    public static final String PUBLISHER_NAME = "publisher name";
    private UriRetriever uriRetriever;

    @BeforeEach
    public void init(WireMockRuntimeInfo wireMockRuntimeInfo) {
        this.somePublisher = createPublisher(wireMockRuntimeInfo);
        this.uriRetriever = new UriRetriever(WiremockHttpClient.create());
    }

    private static Organization createPublisher(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return new Builder()
                   .withId(URI.create(wireMockRuntimeInfo.getHttpsBaseUrl() + "/customer/" + randomUUID()))
                   .build();
    }

    @Test
    public void fromPublicationReturnsDataCiteMetadataDto() {
        Publication publication = samplePublication();
        createCustomerMock(publication.getPublisher());
        DataCiteMetadataDto dataCiteMetadataDto = DataCiteMetadataDtoMapper.fromPublication(publication, uriRetriever);

        assertThat(dataCiteMetadataDto, doesNotHaveEmptyValues());
    }

    @Test
    public void fromPublicationReturnsDataCiteMetadataDtoWithNullFieldsWhenPublicationIsEmpty() {
        Publication publication = Mockito.mock(Publication.class);
        DataCiteMetadataDto dataCiteMetadataDto = DataCiteMetadataDtoMapper.fromPublication(publication, uriRetriever);

        assertThat(dataCiteMetadataDto, notNullValue());
    }

    @Test
    public void shouldReturnDataCiteMetadataDtoFromPublication() {
        var identifier = SortableIdentifier.next();
        var publisher = somePublisher;
        var contributor = createExpectedContributor();
        var entityDescription = new EntityDescription.Builder().withContributors(List.of(contributor)).build();
        Publication publication = createPublicationWithValues(identifier, publisher, entityDescription);
        DataCiteMetadataDto expected = createDataciteMetadateWithValues(contributor, publication);
        createCustomerMock(publisher);
        DataCiteMetadataDto actual = DataCiteMetadataDtoMapper.fromPublication(publication, uriRetriever);
        assertThat(actual.getPublisher().getValue(), is(equalTo(expected.getPublisher().getValue())));
    }

    private static DataCiteMetadataDto createDataciteMetadateWithValues(Contributor contributor, Publication publication) {
        return new DataCiteMetadataDto.Builder()
                   .withIdentifier(new IdentifierDto(publication.getIdentifier().toString()))
                   .withCreator(List.of(new CreatorDto.Builder().withCreatorName(
                       contributor.getIdentity().getName()).build()))
                   .withPublisher(new PublisherDto.Builder().withValue(PUBLISHER_NAME).build())
                   .build();
    }

    private static Publication createPublicationWithValues(SortableIdentifier identifier, Organization publisher,
                                              EntityDescription entityDescription) {
        return new Publication.Builder()
                                      .withIdentifier(identifier)
                                      .withPublisher(publisher)
                                      .withEntityDescription(entityDescription)
                                      .build();
    }

    private Publication samplePublication() {
        return new Publication.Builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withPublisher(somePublisher)
                   .withEntityDescription(createEntityDescriptionWithExpectedAndUnexpectedContributors())
                   .build();
    }

    private EntityDescription createEntityDescriptionWithExpectedAndUnexpectedContributors() {
        return new EntityDescription.Builder()
                   .withReference(createReference())
                   .withMainTitle(MAIN_TITLE)
                   .withDate(createPublicationDate())
                   .withContributors(List.of(createUnexpectedContributor(), createExpectedContributor()))
                   .build();
    }

    private Contributor createExpectedContributor() {
        return new Contributor.Builder()
                   .withSequence(THE_CREATOR)
                   .withAffiliations(List.of(somePublisher))
                   .withIdentity(new Identity.Builder()
                                     .withName(THE_CREATOR_NAME)
                                     .build())
                   .build();
    }

    private Contributor createUnexpectedContributor() {
        return new Contributor.Builder()
                   .withSequence(NOT_THE_CREATOR)
                   .withAffiliations(List.of(somePublisher))
                   .withIdentity(new Identity.Builder()
                                     .withName(NOT_THE_CREATOR_NAME)
                                     .build())
                   .build();
    }

    private PublicationDate createPublicationDate() {
        return new PublicationDate.Builder()
                   .withYear(SOME_YEAR)
                   .withMonth(SOME_MONTH)
                   .withDay(SOME_DAY)
                   .build();
    }

    private Reference createReference() {
        return new Reference.Builder()
                   .withPublicationInstance(createJournalReview()).build();
    }

    private JournalReview createJournalReview() {
        return new JournalReview.Builder()
                   .withIssue(SOME_ISSUE)
                   .withArticleNumber(SOME_ARTICLE_NUMBER)
                   .withVolume(SOME_VOLUME)
                   .build();
    }

    private void createCustomerMock(Organization organization) {
        var customer = new Customer(organization.getId(), PUBLISHER_NAME);
        var response = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(customer)).orElseThrow();
        var id = UriWrapper.fromUri(organization.getId()).getLastPathElement();
        stubFor(WireMock.get(urlPathEqualTo("/customer/" + id))
                    .willReturn(WireMock.ok().withBody(response).withStatus(HttpURLConnection.HTTP_OK)));
    }
}
