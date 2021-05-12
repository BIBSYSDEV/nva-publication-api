package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinObject.IDENTIFIER_ORIGIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.cristin.AbstractCristinImportTest;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import nva.commons.core.JsonSerializable;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CristinMapperTest extends AbstractCristinImportTest {

    public static final String NAME_DELIMITER = ", ";
    private CristinDataGenerator cristinDataGenerator;

    @BeforeEach
    public void init() {
        super.init();
        cristinDataGenerator = new CristinDataGenerator();
        testingData = cristinDataGenerator.randomDataAsString();
    }

    @Test
    public void mapReturnsResourceWithCristinIdStoredInAdditionalIdentifiers() {
        Set<Integer> expectedIds = cristinObjects().map(CristinObject::getId).collect(Collectors.toSet());

        Set<Integer> actualIds = cristinObjects()
                                     .map(CristinObject::toPublication)
                                     .map(Publication::getAdditionalIdentifiers)
                                     .flatMap(Collection::stream)
                                     .map(AdditionalIdentifier::getValue)
                                     .map(Integer::parseInt)
                                     .collect(Collectors.toSet());

        assertThat(expectedIds.size(), is(equalTo(NUMBER_OF_LINES_IN_RESOURCES_FILE)));
        assertThat(actualIds, is(equalTo(expectedIds)));
    }

    @Test
    public void mapReturnsResourceWithMainTitleBeingTheTitleAnnotatedAsOriginalTitle() {

        List<CristinObject> cristinObjects = cristinObjects().collect(Collectors.toList());
        List<String> expectedTitles = cristinObjects.stream()
                                          .map(CristinObject::getCristinTitles)
                                          .map(this::mainTitle)
                                          .map(CristinTitle::getTitle)
                                          .collect(Collectors.toList());

        List<String> actualTitles = cristinObjects.stream()
                                        .map(CristinObject::toPublication)
                                        .map(Publication::getEntityDescription)
                                        .map(EntityDescription::getMainTitle)
                                        .collect(Collectors.toList());
        assertThat(expectedTitles, is(not(empty())));
        assertThat(actualTitles, containsInAnyOrder(expectedTitles.toArray(String[]::new)));
        assertThat(actualTitles.size(), is(equalTo(cristinObjects.size())));
    }

    @Test
    public void mapReturnsResourcePublicationStatusDraft() {

        List<Publication> publications = cristinObjects()
                                             .map(CristinObject::toPublication)
                                             .collect(Collectors.toList());
        for (Publication publication : publications) {
            assertThat(publication.getStatus(), is(equalTo(PublicationStatus.DRAFT)));
        }
    }

    @Test
    @DisplayName("map returns resource with date equal to \"arstall\"")
    public void mapReturnsResourceWithDateEqualToArstall() {
        List<String> expectedPublicationYear = cristinObjects()
                                                   .map(CristinObject::getPublicationYear)
                                                   .collect(Collectors.toList());

        List<String> actualPublicationDates = cristinObjects().map(CristinObject::toPublication)
                                                  .map(Publication::getEntityDescription)
                                                  .map(EntityDescription::getDate)
                                                  .map(PublicationDate::getYear)
                                                  .collect(Collectors.toList());
        assertThat(expectedPublicationYear, is(not(empty())));
        assertThat(actualPublicationDates, containsInAnyOrder(expectedPublicationYear.toArray(String[]::new)));
    }

    @Test
    @DisplayName("map returns resource with createdDate equal to \"dato_opprettet\"")
    public void mapReturnsResourceWithCreatedDateEqualToDatoOpprettet() {
        ZoneOffset currentZoneOffset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
        List<Instant> expectedCreatedDates = cristinObjects()
                                                 .map(CristinObject::getEntryCreationDate)
                                                 .map(LocalDate::atStartOfDay)
                                                 .map(time -> time.toInstant(currentZoneOffset))
                                                 .collect(Collectors.toList());

        List<Instant> actualCreatedDates = cristinObjects().map(CristinObject::toPublication)
                                               .map(Publication::getCreatedDate)
                                               .collect(Collectors.toList());

        assertThat(actualCreatedDates, containsInAnyOrder(expectedCreatedDates.toArray(Instant[]::new)));
    }

    @Test
    public void mapReturnsBookAnthologyWhenInputHasMainTypeBookAndSecondaryTypeAnthology() {
        testingData = Stream.of(cristinDataGenerator.randomBookAnthology())
                          .map(JsonSerializable::toJsonString)
                          .collect(SingletonCollector.collect());

        Publication actualPublication = cristinObjects()
                                            .map(CristinObject::toPublication)
                                            .collect(SingletonCollector.collect());

        PublicationInstance<?> actualPublicationInstance = actualPublication
                                                               .getEntityDescription()
                                                               .getReference()
                                                               .getPublicationInstance();
        PublicationContext actualPublicationContext = actualPublication
                                                          .getEntityDescription()
                                                          .getReference()
                                                          .getPublicationContext();

        assertThat(actualPublicationInstance, is(instanceOf(BookAnthology.class)));
        assertThat(actualPublicationContext, is(instanceOf(Book.class)));
    }

    @Test
    public void mapReturnsResourceWhereNvaContributorsNamesAreConcatenationsOfCristinFirstAndFamilyNames() {

        List<String> expectedContributorNames = cristinObjects()
                                                    .map(CristinObject::getContributors)
                                                    .flatMap(Collection::stream)
                                                    .map(this::formatNameAccordingToNvaPattern)
                                                    .collect(Collectors.toList());

        List<String> actualContributorNames = cristinObjects()
                                                  .map(CristinObject::toPublication)
                                                  .map(Publication::getEntityDescription)
                                                  .map(EntityDescription::getContributors)
                                                  .flatMap(Collection::stream)
                                                  .map(Contributor::getIdentity)
                                                  .map(Identity::getName)
                                                  .collect(Collectors.toList());

        assertThat(actualContributorNames, containsInAnyOrder(expectedContributorNames.toArray(String[]::new)));
    }

    @Test
    public void mapReturnsResourceWhereNvaContributorSequenceIsEqualToCristinContributorSequence() {

        Set<ContributionReference> expectedContributions = cristinObjects()
                                                               .map(this::extractContributions)
                                                               .flatMap(Collection::stream)
                                                               .collect(Collectors.toSet());

        Set<ContributionReference> actualContributions = cristinObjects()
                                                             .map(CristinObject::toPublication)
                                                             .map(this::extractContributions)
                                                             .flatMap(Collection::stream)
                                                             .collect(Collectors.toSet());

        assertThat(expectedContributions, is(equalTo(actualContributions)));
    }

    private List<ContributionReference> extractContributions(Publication publication) {

        AdditionalIdentifier cristinIdentifier = publication.getAdditionalIdentifiers().stream()
                                                     .filter(this::isCristinIdentifier)
                                                     .collect(SingletonCollector.collect());
        Integer cristinIdentifierValue = Integer.parseInt(cristinIdentifier.getValue());

        return publication.getEntityDescription()
                   .getContributors().stream()
                   .map(contributor -> extractContributionReference(cristinIdentifierValue, contributor))
                   .collect(Collectors.toList());
    }

    private List<ContributionReference> extractContributions(CristinObject cristinObject) {
        final Integer cristinResourceIdentifier = cristinObject.getId();
        return cristinObject.getContributors().stream()
                   .map(c -> new ContributionReference(cristinResourceIdentifier, c.getIdentifier(),
                                                       c.getContributorOrder()))
                   .collect(Collectors.toList());
    }

    private boolean isCristinIdentifier(AdditionalIdentifier identifier) {
        return identifier.getSource().equals(IDENTIFIER_ORIGIN);
    }

    private ContributionReference extractContributionReference(Integer cristinIdentifierValue,
                                                               Contributor contributor) {
        return new ContributionReference(cristinIdentifierValue, extractPersonId(contributor),
                                         contributor.getSequence());
    }

    private Integer extractPersonId(Contributor contributor) {
        String personIdentifier = Path.of(contributor.getIdentity().getId().getPath()).getFileName().toString();
        return Integer.parseInt(personIdentifier);
    }

    private String formatNameAccordingToNvaPattern(CristinContributor cristinContributor) {
        return cristinContributor.getFamilyName() + NAME_DELIMITER + cristinContributor.getGivenName();
    }

    private CristinTitle mainTitle(List<CristinTitle> titles) {
        return titles.stream().filter(CristinTitle::isMainTitle).collect(SingletonCollector.collect());
    }
}