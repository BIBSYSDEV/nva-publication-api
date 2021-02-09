package no.unit.nva.publication.storage.model;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringClasses;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationDate.Builder;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.publication.PublicationGenerator;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

public class DoiRequestTest {

    public static final String TYPE_FIELD = "type";
    public static final SortableIdentifier DOI_REQUEST_IDENTIFIER = SortableIdentifier.next();
    public static final SortableIdentifier RESOURCE_IDENTIFIER = SortableIdentifier.next();
    public static final String RESOURCE_TITLE = "resourceTitle";
    public static final String SOME_OWNER = "someOwner";
    public static final URI SOME_CUSTOMER = URI.create("https://some-customer.com");
    public static final DoiRequestStatus SOME_DOI_REQUEST_STATUS = DoiRequestStatus.REJECTED;
    public static final PublicationStatus SOME_PUBLICATION_STATUS = PublicationStatus.DRAFT;
    public static final Instant DOI_REQUEST_CREATION_TIME = Instant.parse("1000-01-01T10:15:30.00Z");
    public static final Instant DOI_REQUEST_UPDATE_TIME = Instant.parse("2000-01-01T10:15:30.00Z");
    private static final Instant NOW = Instant.now();
    public static final Clock CLOCK = fixedClock();
    public static final String SOME_PUBLICATION_YEAR = "ca. 1600";
    public static final URI SOME_DOI = URI.create("https://doi.org/example");
    private final DoiRequest sampleDoiRequest = sampleDoiRequest();

    @Test
    public void doiRequestHasTypeDoiRequest() {

        JsonNode json = JsonUtils.objectMapper.convertValue(sampleDoiRequest, JsonNode.class);
        assertThat(json.get(TYPE_FIELD), is(not(nullValue())));
        assertThat(json.get(TYPE_FIELD).textValue(), is(equalTo(DoiRequest.TYPE)));
    }

    @Test
    public void doiRequestHasReferenceToResource() {
        assertThat(sampleDoiRequest.getResourceIdentifier(), is(notNullValue()));
    }

    @Test
    public void doiRequestCannotBeCreatedWithoutReferenceToResource() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            this::doiRequestWithoutResourceReference);
        assertThat(exception.getMessage(), is(equalTo(DoiRequest.MISSING_RESOURCE_REFERENCE_ERROR)));
    }

    @Test
    public void doiRequestContainsResourcesMainTitle() {
        Publication publication = PublicationGenerator.publicationWithIdentifier();
        Resource resource = Resource.fromPublication(publication);
        DoiRequest doiRequest = DoiRequest.newDoiRequestForResource(resource, fixedClock().instant());
        assertThat(doiRequest.getResourceTitle(), is(equalTo(publication.getEntityDescription().getMainTitle())));
    }

    @Test
    public void toPublicationReturnsPublicationInstanceWithoutLossOfInformation() {
        PublicationDate publicationDate = new Builder()
            .withYear(SOME_PUBLICATION_YEAR)
            .withMonth("5")
            .withDay("22")
            .build();
        Contributor sampleContributor = attempt(PublicationGenerator::sampleContributor).orElseThrow();
        DoiRequest doiRequest = DoiRequest.builder()
            .withIdentifier(DOI_REQUEST_IDENTIFIER)
            .withResourceIdentifier(RESOURCE_IDENTIFIER)
            .withResourceTitle(RESOURCE_TITLE)
            .withOwner(SOME_OWNER)
            .withCustomerId(SOME_CUSTOMER)
            .withStatus(SOME_DOI_REQUEST_STATUS)
            .withResourceStatus(SOME_PUBLICATION_STATUS)
            .withCreatedDate(DOI_REQUEST_CREATION_TIME)
            .withModifiedDate(DOI_REQUEST_UPDATE_TIME)
            .withResourceModifiedDate(DOI_REQUEST_UPDATE_TIME)
            .withResourcePublicationDate(publicationDate)
            .withResourcePublicationYear(SOME_PUBLICATION_YEAR)
            .withDoi(SOME_DOI)
            .withResourcePublicationInstance(new ReportBasic.Builder().build())
            .withContributors(List.of(sampleContributor))
            .build();

        assertThat(doiRequest, doesNotHaveEmptyValuesIgnoringClasses(Set.of(PublicationInstance.class)));
        Publication generatedPublication = doiRequest.toPublication();

        DoiRequest regeneratedDoiRequest = DoiRequest.fromDto(generatedPublication, doiRequest.getIdentifier());
        assertThat(regeneratedDoiRequest, doesNotHaveEmptyValuesIgnoringClasses(Set.of(PublicationInstance.class)));
        assertThat(regeneratedDoiRequest, is(equalTo(doiRequest)));
    }

    private static Clock fixedClock() {
        return Clock.fixed(NOW, ZoneId.systemDefault());
    }

    private DoiRequest doiRequestWithoutResourceReference() {
        Resource resource = Resource.fromPublication(PublicationGenerator.publicationWithoutIdentifier());
        return DoiRequest.newDoiRequestForResource(resource);
    }

    private DoiRequest sampleDoiRequest() {
        Resource resource = Resource.fromPublication(PublicationGenerator.publicationWithIdentifier());
        return DoiRequest.newDoiRequestForResource(resource, CLOCK.instant());
    }
}