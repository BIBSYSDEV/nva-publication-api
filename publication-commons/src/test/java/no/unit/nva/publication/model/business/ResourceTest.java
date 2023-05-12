package no.unit.nva.publication.model.business;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ResourceTest {

    public static final User SOME_OWNER = new User("some@owner.no");
    public static final URI SOME_HOST = URI.create("https://example.org/");
    public static final String DOI_REQUEST_FIELD = "doiRequest";
    public static final String IMPORT_STATUS = "importStatus";
    private final Javers javers = JaversBuilder.javers().build();
    private final SortableIdentifier sampleIdentifier = SortableIdentifier.next();

    @ParameterizedTest(name = "builder contains all fields: {0}")
    @MethodSource("publicationInstanceProvider")
    void builderContainsAllFields(Class<?> publicationInstanceType) {
        Resource resource = sampleResource(publicationInstanceType);
        assertThat(resource, doesNotHaveEmptyValuesIgnoringFields(Set.of(IMPORT_STATUS)));
    }

    @ParameterizedTest(name = "copy contains all fields: {0}")
    @MethodSource("publicationInstanceProvider")
    void copyContainsAllFields(Class<?> publicationInstanceType) {
        Resource resource = sampleResource(publicationInstanceType);
        Resource copy = resource.copy().build();
        JsonNode resourceJson = dynamoDbObjectMapper.convertValue(resource, JsonNode.class);
        JsonNode copyJson = dynamoDbObjectMapper.convertValue(copy, JsonNode.class);
        assertThat(resource, doesNotHaveEmptyValuesIgnoringFields(Set.of(IMPORT_STATUS)));
        assertThat(copy, is(equalTo(resource)));
        assertThat(resourceJson, is(equalTo(copyJson)));
    }

    @ParameterizedTest
    @MethodSource("publicationInstanceProvider")
    void copyShouldCreateDeepCopyWithoutOverridingValuesOfOriginalResourceWhenModifyingCopy(
        Class<?> publicationInstanceType) {
        Resource resource = sampleResource(publicationInstanceType);
        Resource copy = resource.copy().build();
        copy.setLink(randomUri());
        assertThat(resource, not(is(equalTo(copy))));
        Diff diff = javers.compare(resource, copy);
        assertThat(diff.prettyPrint(), diff.getChanges().size(), is(1));
    }

    @ParameterizedTest(name = "to DTO returns DTO without loss of information: {0}")
    @MethodSource("publicationInstanceProvider")
    void toDtoReturnsDtoWithoutLossOfInformation(Class<?> publicationInstanceType) {
        Resource resource = sampleResource(publicationInstanceType);
        assertThat(resource, doesNotHaveEmptyValuesIgnoringFields(Set.of(IMPORT_STATUS)));
        Publication publication = resource.toPublication();
        Resource fromPublication = Resource.fromPublication(publication);
        Diff diff = javers.compare(resource, fromPublication);
        assertThat(diff.prettyPrint(), diff.getChanges().size(), is(0));
    }

    @ParameterizedTest(name = "from dto to dao and back without loss for type {0}")
    @MethodSource("publicationInstanceProvider")
    void fromDtoToDaoToDtoReturnsDtoWithoutLossOfInformation(Class<?> publicationInstanceType) {
        var expected = PublicationGenerator.randomPublication(publicationInstanceType);
        assertThat(expected, doesNotHaveEmptyValuesIgnoringFields(Set.of(DOI_REQUEST_FIELD)));

        var transformed = Resource.fromPublication(expected).toPublication();

        var diff = javers.compare(expected, transformed);

        //TODO: re-insert when "owner" field has been removed in favor for "resourceOwner"
        //assertThat(diff.prettyPrint(), diff.getChanges().size(), is(0));

        assertThat(diff.prettyPrint(), transformed, is(equalTo(expected)));
    }

    @Test
    void emptyResourceReturnsResourceWithTheMinimumNecessaryFieldsNotNull() {

        Resource emptyResource = Resource.emptyResource(SOME_OWNER, SOME_HOST, sampleIdentifier);
        assertThat(emptyResource.getIdentifier(), is(equalTo(sampleIdentifier)));
        assertThat(emptyResource.getPublisher().getId(), is(equalTo(SOME_HOST)));
        assertThat(emptyResource.getOwner(), is(equalTo(SOME_OWNER)));
    }

    @Test
    void queryObjectReturnsResourceWithIdentifier() {
        Resource resource = Resource.resourceQueryObject(sampleIdentifier);
        assertThat(resource.getIdentifier(), is(equalTo(sampleIdentifier)));
    }

    @Test
    void queryObjectReturnsResourceWithIdentifierAndPublisher() {
        UserInstance userInstance = UserInstance.create(SOME_OWNER, SOME_HOST);
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