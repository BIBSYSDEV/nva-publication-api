package no.unit.nva.publication.storage.model;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.publication.storage.model.StorageModelConfig.dynamoDbObjectMapper;
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
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;

import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.storage.model.exceptions.IllegalDoiRequestUpdate;
import org.junit.jupiter.api.Test;

public class DoiRequestTest {
    
    public static final String TYPE_FIELD = "type";
    private static final Instant NOW = Instant.now();
    public static final Clock CLOCK = fixedClock();
    private final DoiRequest sampleDoiRequest = sampleDoiRequestFromResource();
    
    @Test
    public void doiRequestHasTypeDoiRequest() {

        JsonNode json = dynamoDbObjectMapper.convertValue(sampleDoiRequest, JsonNode.class);
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

        DoiRequest doiRequest =
            DoiRequest.newDoiRequestForResource(Resource.fromPublication(PublicationGenerator.randomPublication()));

        assertThat(doiRequest, doesNotHaveEmptyValues());
        Publication generatedPublication = doiRequest.toPublication();

        DoiRequest regeneratedDoiRequest = DoiRequest.fromDto(generatedPublication, doiRequest.getIdentifier());
        // when transformed to Publication we do not have control over the rowVersion anymore because
        // nva-datamodel-java is an external library.
        assertThat(regeneratedDoiRequest, doesNotHaveEmptyValuesIgnoringFields(Set.of("rowVersion")));
        assertThat(regeneratedDoiRequest, is(equalTo(doiRequest)));
    }

    @Test
    public void fromPublicationIsEquivalentToFromDto() {
        Publication publication = sampleDoiRequest.toPublication();
        SortableIdentifier identifier = sampleDoiRequest.getIdentifier();

        DoiRequest fromPublication = DoiRequest.fromPublication(publication, identifier);
        DoiRequest fromDto = DoiRequest.fromDto(publication, identifier);

        assertThat(fromPublication, is(equalTo(fromDto)));
        assertThat(fromDto, is(equalTo(sampleDoiRequest)));
    }

    @Test
    public void updateReturnsNewAndUpdatedDoiRequest() {
        Resource resource = Resource.fromPublication(PublicationGenerator.publicationWithIdentifier());
        DoiRequest doiRequest = DoiRequest.newDoiRequestForResource(resource);

        String newTitle = "newTitle";
        Resource updatedResource = updateResource(resource, newTitle);

        DoiRequest updatedDoiRequest = doiRequest.update(updatedResource);
        assertThat(updatedDoiRequest.getResourceTitle(), is(equalTo(newTitle)));
    }
    
    @Test
    public void updateThrowsExceptionWhenResourceIdentifierIsDiffenret() {
        Resource resource = Resource.fromPublication(PublicationGenerator.publicationWithIdentifier());
        DoiRequest doiRequest = DoiRequest.newDoiRequestForResource(resource);
        
        Resource updatedResource = Resource.fromPublication(PublicationGenerator.publicationWithIdentifier());
        
        assertThrows(IllegalDoiRequestUpdate.class, () -> doiRequest.update(updatedResource));
    }
    
    private static Clock fixedClock() {
        return Clock.fixed(NOW, ZoneId.systemDefault());
    }
    
    private Resource updateResource(Resource resource, String newTitle) {
        EntityDescription updatedEntityDescription = resource.getEntityDescription();
        updatedEntityDescription.setMainTitle(newTitle);
        return resource.copy().withEntityDescription(updatedEntityDescription).build();
    }
    

    
    private DoiRequest doiRequestWithoutResourceReference() {
        Resource resource = Resource.fromPublication(PublicationGenerator.publicationWithoutIdentifier());
        return DoiRequest.newDoiRequestForResource(resource);
    }
    
    private DoiRequest sampleDoiRequestFromResource() {
        Resource resource = Resource.fromPublication(PublicationGenerator.publicationWithIdentifier());
        return DoiRequest.newDoiRequestForResource(resource, CLOCK.instant());
    }
}