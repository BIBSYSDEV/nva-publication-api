package no.unit.nva.publication.model.business;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.storage.model.exceptions.IllegalDoiRequestUpdate;
import org.junit.jupiter.api.Test;

class DoiRequestTest {
    
    public static final String TYPE_FIELD = "type";
    private static final Instant NOW = Instant.now();
    public static final Clock CLOCK = fixedClock();
    private final DoiRequest sampleDoiRequest = sampleDoiRequestFromResource();
    
    @Test
    void doiRequestHasTypeDoiRequest() {
        JsonNode json = dynamoDbObjectMapper.convertValue(sampleDoiRequest, JsonNode.class);
        assertThat(json.get(TYPE_FIELD), is(not(nullValue())));
        assertThat(json.get(TYPE_FIELD).textValue(), is(equalTo(DoiRequest.TYPE)));
    }
    
    @Test
    void doiRequestHasReferenceToResource() {
        assertThat(sampleDoiRequest.getResourceIdentifier(), is(notNullValue()));
    }
    
    @Test
    void doiRequestCannotBeCreatedWithoutReferenceToResource() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            this::doiRequestWithoutResourceReference);
        assertThat(exception.getMessage(), is(equalTo(DoiRequest.MISSING_RESOURCE_REFERENCE_ERROR)));
    }
    
    @Test
    void doiRequestContainsResourcesMainTitle() {
        Publication publication = PublicationGenerator.publicationWithIdentifier();
        Resource resource = Resource.fromPublication(publication);
        DoiRequest doiRequest = DoiRequest.newDoiRequestForResource(resource, fixedClock().instant());
        assertThat(doiRequest.getResourceTitle(), is(equalTo(publication.getEntityDescription().getMainTitle())));
    }
    
    @Test
    void toPublicationPreservesPublicationRelatedFields() {
    
        var originalDoiRequest = DoiRequest.fromPublication(PublicationGenerator.randomPublication());
    
        var generatedPublication = originalDoiRequest.toPublication();
    
        var regeneratedDoiRequest = DoiRequest.fromPublication(generatedPublication);
        regeneratedDoiRequest.setIdentifier(originalDoiRequest.getIdentifier());
        regeneratedDoiRequest.setCreatedDate(originalDoiRequest.getCreatedDate());
        regeneratedDoiRequest.setModifiedDate(originalDoiRequest.getModifiedDate());
    
        assertThat(originalDoiRequest, doesNotHaveEmptyValues());
        assertThat(regeneratedDoiRequest, doesNotHaveEmptyValues());
        assertThat(regeneratedDoiRequest, is(equalTo(originalDoiRequest)));
    }
    
    @Test
    void updateReturnsNewAndUpdatedDoiRequest() {
        Resource resource = Resource.fromPublication(PublicationGenerator.publicationWithIdentifier());
        DoiRequest doiRequest = DoiRequest.newDoiRequestForResource(resource);
        
        String newTitle = "newTitle";
        Resource updatedResource = updateResource(resource, newTitle);
        
        DoiRequest updatedDoiRequest = doiRequest.update(updatedResource);
        assertThat(updatedDoiRequest.getResourceTitle(), is(equalTo(newTitle)));
    }
    
    @Test
    void updateThrowsExceptionWhenResourceIdentifierIsDifferent() {
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