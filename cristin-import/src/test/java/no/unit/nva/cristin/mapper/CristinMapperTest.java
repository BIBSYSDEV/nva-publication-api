package no.unit.nva.cristin.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.cristin.AbstractCristinImportTest;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CristinMapperTest extends AbstractCristinImportTest {

    @BeforeEach
    public void init() {
        super.init();
        content = new CristinDataGenerator().randomDataAsString();
    }

    @Test
    public void mapReturnsResourceWithCristinIdStoredInAdditionalIdentifiers() {
        Set<String> expectedIds = cristinObjects().map(CristinObject::getId).collect(Collectors.toSet());

        Set<String> actualIds = cristinObjects()
                                    .map(CristinObject::toPublication)
                                    .map(Publication::getAdditionalIdentifiers)
                                    .flatMap(Collection::stream)
                                    .map(AdditionalIdentifier::getValue)
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

    private CristinTitle mainTitle(List<CristinTitle> titles) {
            return titles.stream().filter(CristinTitle::isMainTitle).collect(SingletonCollector.collect());
    }
}