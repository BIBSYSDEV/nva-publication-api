package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedOrganization;
import no.unit.nva.expansion.model.ExpandedPerson;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.NonClaimedPublicationChannel;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.Arguments.ArgumentSet;
import org.junit.jupiter.params.provider.MethodSource;

public class ExpansionManagerTest {

    private static final Resource mockedResource = mock(Resource.class);
    private static final FileEntry mockedFileEntry = mock(FileEntry.class);
    private static final Message mockedMessage = mock(Message.class);
    private static final DoiRequest mockedDoiRequest = mock(DoiRequest.class);
    static {
        when(mockedResource.getStatus()).thenReturn(PublicationStatus.PUBLISHED);
        when(mockedDoiRequest.getResourceStatus()).thenReturn(PublicationStatus.PUBLISHED);
    }
    private static final List<Entity> ENTITY_INSTANCES = List.of(
        mockedResource,
        mockedFileEntry,
        mockedMessage,
        mockedDoiRequest
    );

    private static Resource randomResource() {
        var publication = randomPublication();
        publication.setStatus(PublicationStatus.PUBLISHED);
        return Resource.fromPublication(publication);
    }

    private static final Map<Class<? extends Entity>, Optional<String>> ENTITY_EXPECTATIONS = Map.of(
        Resource.class, Optional.of("Resource"),
        FileEntry.class, Optional.of("Resource"),
        DoiRequest.class, Optional.of("DoiRequest"),
        FilesApprovalThesis.class, Optional.of("FilesApprovalThesis"),
        GeneralSupportRequest.class, Optional.of("GeneralSupportRequest"),
        PublishingRequestCase.class, Optional.of("PublishingRequestCase"),
        UnpublishRequest.class, Optional.of("UnpublishRequest"),
        Message.class, Optional.of("TicketEntry"),
        ClaimedPublicationChannel.class, Optional.empty(),
        NonClaimedPublicationChannel.class, Optional.empty()
    );

    private static Stream<ArgumentSet> entityExpanderImplementationClasses() {
        var entityExpanders = allEntityExpanders();
        return Stream.of(
            entityExpanders.stream()
                .map(entityExpander ->
                         Arguments.argumentSet(entityExpander.getSimpleName(), entityExpander))
                .toArray(ArgumentSet[]::new)
        );
    }

    private static Stream<ArgumentSet> scannedEntities() {
        return Stream.of(
            allEntities().stream()
                .map(entity -> Arguments.argumentSet(entity.getSimpleName(), entity))
                .toArray(ArgumentSet[]::new)
        );
    }

    private static List<Class<? extends EntityExpander>> allEntityExpanders() {
        var entityExpanders = new ArrayList<Class<? extends EntityExpander>>();

        try (ScanResult scanResult = new ClassGraph()
                                         .enableClassInfo()
                                         .acceptPackages("no.unit.nva.publication.events.handlers.expandresources")
                                         .scan()) {

            for (var classInfo : scanResult.getClassesImplementing(EntityExpander.class.getName())) {
                if (!classInfo.isAbstract() && !classInfo.isInterface()) {
                    var implementingClass = classInfo.loadClass(EntityExpander.class);
                    entityExpanders.add(implementingClass);
                }
            }
        }

        return entityExpanders;
    }

    private static List<Class<? extends Entity>> allEntities() {
        var entities = new ArrayList<Class<? extends Entity>>();

        try (ScanResult scanResult = new ClassGraph()
                                         .enableClassInfo()
                                         .acceptPackages("no.unit.nva.publication.model.business")
                                         .scan()) {

            for (var classInfo : scanResult.getClassesImplementing(Entity.class.getName())) {
                if (!classInfo.isAbstract() && !classInfo.isInterface()) {
                    var implementingClass = classInfo.loadClass(Entity.class);
                    entities.add(implementingClass);
                }
            }
        }

        return entities;
    }

    static Stream<ArgumentSet> entityExpanderInstances() {
        return Stream.of(
            allEntityExpanders().stream()
                .map(clazz -> attempt(() -> clazz.getDeclaredConstructor().newInstance()))
                .map(entityExpander -> Arguments.argumentSet(entityExpander.getClass().getSimpleName(), entityExpander))
                .toArray(ArgumentSet[]::new)
        );
    }

    static Stream<ArgumentSet> failingExpansionDataProvider() {
        return allEntityExpanders().stream()
                   .map(clazz -> attempt(() -> clazz.getDeclaredConstructor().newInstance()).orElseThrow())
                   .flatMap(entityExpander -> Stream.of(
                       Arguments.argumentSet(entityExpander.getClass().getSimpleName(), entityExpander,
                                             new JsonProcessingException("test") {
                                             }),
                       Arguments.argumentSet(entityExpander.getClass().getSimpleName(), entityExpander,
                                             new NotFoundException("test"))
                   ));
    }

    @ParameterizedTest(name = "entity {0} has expectations")
    @MethodSource("scannedEntities")
    void allEntitiesHaveExpectations(Class<? extends Entity> entityClass) {
        assertThat(ENTITY_EXPECTATIONS, hasKey(entityClass));
    }

    @ParameterizedTest(name = "entity expander {0} should throw EntityExpansionException when "
                              + "ResourceExpansionService throws {1}")
    @MethodSource("failingExpansionDataProvider")
    void entityExpanderCanHandleEntity(EntityExpander entityExpander, Throwable throwable) {
        var entity = entityExpandableBy(entityExpander);
        assertThrows(EntityExpansionException.class,
                     () -> entityExpander.expand(failingResourceExpansionService(throwable)
                         , entity, entity));
    }

    private Entity entityExpandableBy(EntityExpander entityExpander) {
        return ENTITY_INSTANCES.stream()
                   .filter(entity -> entityExpander.canExpand(entity.getClass()))
                   .findFirst()
                   .orElseThrow();
    }

    @Test
    void shouldThrowExceptionWhenEntityExpanderNotFound() {
        var publication = randomPublication();
        var resourceExpansionService = mock(ResourceExpansionService.class);
        var manager = new ExpansionManager(resourceExpansionService);
        var entity = Resource.fromPublication(publication);

        assertThrows(NoEntityExpanderException.class, () -> manager.expand(entity, entity));
    }

    private static ResourceExpansionService failingResourceExpansionService(Throwable throwable) {
        return new ResourceExpansionService() {

            @Override
            public Optional<ExpandedDataEntry> expandEntry(Entity dataEntry, boolean replaceContext)
                throws JsonProcessingException, NotFoundException {
                switch (throwable) {
                    case JsonProcessingException jsonProcessingException -> throw jsonProcessingException;
                    case NotFoundException notFoundException -> throw notFoundException;
                    case RuntimeException runtimeException -> throw runtimeException;
                    case null, default -> throw new RuntimeException("Unexpected exception", throwable);
                }
            }

            @Override
            public ExpandedOrganization getOrganization(Entity dataEntry) throws NotFoundException {
                if (throwable instanceof NotFoundException) {
                    throw (NotFoundException) throwable;
                } else if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                } else {
                    throw new RuntimeException("Unexpected exception", throwable);
                }
            }

            @Override
            public ExpandedPerson expandPerson(User username) {
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                } else {
                    throw new RuntimeException("Unexpected exception", throwable);
                }
            }

            @Override
            public ExpandedMessage expandMessage(Message messages) {
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                } else {
                    throw new RuntimeException("Unexpected exception", throwable);
                }
            }
        };
    }
}
