package no.unit.nva.publication.storage.model;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.publication.storage.model.StorageModelConfig.dynamoDbObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Identity;
import no.unit.nva.model.NameType;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ResourceTest {

    public static final String SOME_OWNER = "some@owner.no";
    public static final URI SOME_HOST = URI.create("https://example.org/");
    public static final String DOI_REQUEST_FIELD = "doiRequest";
    private final Javers javers = JaversBuilder.javers().build();
    private final SortableIdentifier sampleIdentifier = SortableIdentifier.next();

    @ParameterizedTest
    @MethodSource("publicationInstanceProvider")
    public void builderContainsAllFields(Class<?> publicationInstanceType) {
        Resource resource = sampleResource(publicationInstanceType);
        assertThat(resource, doesNotHaveEmptyValues());
    }


    @ParameterizedTest
    @MethodSource("publicationInstanceProvider")
    public void copyContainsAllFields(Class<?> publicationInstanceType) {
        Resource resource = sampleResource(publicationInstanceType);
        Resource copy = resource.copy().build();
        JsonNode resourceJson = dynamoDbObjectMapper.convertValue(resource, JsonNode.class);
        JsonNode copyJson = dynamoDbObjectMapper.convertValue(copy, JsonNode.class);
        assertThat(resource, doesNotHaveEmptyValues());
        assertThat(copy, is(equalTo(resource)));
        assertThat(resourceJson, is(equalTo(copyJson)));
    }

    @ParameterizedTest
    @MethodSource("publicationInstanceProvider")
    public void toDtoReturnsDtoWithoutLossOfInformation(Class<?> publicationInstanceType) {
        Resource resource = sampleResource(publicationInstanceType);
        assertThat(resource, doesNotHaveEmptyValues());
        Publication publication = resource.toPublication();
        Resource fromPublication = Resource.fromPublication(publication);
        //inject row version because conversion to publication changes the row version
        fromPublication.setRowVersion(resource.getRowVersion());
        Diff diff = javers.compare(resource, fromPublication);
        assertThat(diff.prettyPrint(), diff.getChanges().size(), is(0));
    }

    @ParameterizedTest
    @MethodSource("publicationInstanceProvider")
    public void fromDtoToDaoToDtoReturnsDtoWithoutLossOfInformation(Class<?> publicationInstanceType) {
        Publication expected = PublicationGenerator.randomPublication(publicationInstanceType);
        assertThat(expected, doesNotHaveEmptyValuesIgnoringFields(Set.of(DOI_REQUEST_FIELD)));

        Publication transformed = Resource.fromPublication(expected).toPublication();
        //doiRequest does not get saved with the resource
        transformed.setDoiRequest(expected.getDoiRequest());

        Diff diff = javers.compare(expected, transformed);

        //TODO: re-insert when "owner" field has been removed in favor for "resourceOwner"
        //assertThat(diff.prettyPrint(), diff.getChanges().size(), is(0));

        assertThat(diff.prettyPrint(),transformed, is(equalTo(expected)));
    }

    @Test
    public void emptyResourceReturnsResourceWithTheMinimumNecessaryFieldsNotNull() {

        Resource emptyResource = Resource.emptyResource(SOME_OWNER, SOME_HOST, sampleIdentifier);
        assertThat(emptyResource.getIdentifier(), is(equalTo(sampleIdentifier)));
        assertThat(emptyResource.getPublisher().getId(), is(equalTo(SOME_HOST)));
        assertThat(emptyResource.getOwner(), is(equalTo(SOME_OWNER)));
    }

    @Test
    public void queryObjectReturnsResourceWithIdentifier() {
        Resource resource = Resource.resourceQueryObject(sampleIdentifier);
        assertThat(resource.getIdentifier(), is(equalTo(sampleIdentifier)));
    }

    @Test
    public void queryObjectReturnsResourceWithIdentifierAndPublisher() {
        UserInstance userInstance = new UserInstance(SOME_OWNER, SOME_HOST);
        Resource resource = Resource.resourceQueryObject(userInstance, sampleIdentifier);
        assertThat(resource.getIdentifier(), is(equalTo(sampleIdentifier)));
        assertThat(resource.getPublisher().getId(), is(equalTo(SOME_HOST)));
        assertThat(resource.getOwner(), is(equalTo(SOME_OWNER)));
    }

    private static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    private Resource sampleResource(Class<?> publicationInstanceType) {
        return Resource.fromPublication(PublicationGenerator.randomPublication(publicationInstanceType));
    }
}