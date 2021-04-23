package no.unit.nva.cristin.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import java.io.IOException;
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
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CristinMapperTest extends AbstractCristinImportTest {

    @Test
    public void mapReturnsResourceWithCristinIdStoredInAdditionalIdentifiers() throws IOException {
        Set<String> expectedIds = cristinObjects().map(CristinObject::getId).collect(Collectors.toSet());

        Set<String> actualIds = extractedPublications()
                                    .map(Publication::getAdditionalIdentifiers)
                                    .flatMap(Collection::stream)
                                    .map(AdditionalIdentifier::getValue)
                                    .collect(Collectors.toSet());

        assertThat(expectedIds.size(), is(equalTo(NUMBER_OF_LINES_IN_RESOURCES_FILE)));
        assertThat(actualIds, is(equalTo(expectedIds)));
    }

    @Test
    @DisplayName("map returns resource with main title the max length title in \"titteltekst\"")
    public void mapReturnsResourceWithMainTitleTheMaxLengthTitleInTitteltekst() throws IOException {

        List<CristinObject> cristingObjects = cristinObjects().collect(Collectors.toList());
        List<String> expectedTitles = cristingObjects.stream()
                                          .map(CristinObject::getCristinTitles)
                                          .map(this::maxLengthTitle)
                                          .map(CristinTitle::getTitle)
                                          .collect(Collectors.toList());

        List<String> actualTitles = extractedPublications()
                                        .map(Publication::getEntityDescription)
                                        .map(EntityDescription::getMainTitle)
                                        .collect(Collectors.toList());
        assertThat(expectedTitles, is(not(empty())));
        assertThat(actualTitles, containsInAnyOrder(expectedTitles.toArray(String[]::new)));
        assertThat(actualTitles.size(), is(equalTo(cristingObjects.size())));
    }

    @Test
    @DisplayName("map returns resource with main title equal to \"arstall\"")
    public void mapReturnsResourceWithDateEqualToArsTall() throws IOException {
        List<String> expectedPublicationYear = cristinObjects()
                                                   .map(CristinObject::getPublicationYear)
                                                   .collect(Collectors.toList());

        List<String> actualPublicationDates = extractedPublications()
                                                  .map(Publication::getEntityDescription)
                                                  .map(EntityDescription::getDate)
                                                  .map(PublicationDate::getYear)
                                                  .collect(Collectors.toList());
        assertThat(expectedPublicationYear, is(not(empty())));
        assertThat(actualPublicationDates, containsInAnyOrder(expectedPublicationYear.toArray(String[]::new)));
    }

    @Test
    @DisplayName("map returns resource with createdDate equal to \"dato_opprettet\"")
    public void mapReturnsResourceWithCreatedDateEqualToDatoOpprettet() throws IOException {
        ZoneOffset currentZoneOffset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
        List<Instant> expectedCreatedDates = cristinObjects()
                                                 .map(CristinObject::getEntryCreationDate)
                                                 .map(LocalDate::atStartOfDay)
                                                 .map(time -> time.toInstant(currentZoneOffset))
                                                 .collect(Collectors.toList());

        List<Instant> actualCreatedDates = extractedPublications()
                                               .map(Publication::getCreatedDate)
                                               .collect(Collectors.toList());

        assertThat(actualCreatedDates, containsInAnyOrder(expectedCreatedDates.toArray(Instant[]::new)));
    }

    private CristinTitle maxLengthTitle(List<CristinTitle> titles) {
        CristinTitle maxTitle = null;
        int maxSize = -1;
        for (CristinTitle title : titles) {
            if (maxSize < title.getTitle().length()) {
                maxSize = title.getTitle().length();
                maxTitle = title;
            }
        }
        return maxTitle;
    }

    private Stream<Publication> extractedPublications() throws IOException {
        return cristinObjects()
                   .map(CristinMapper::new)
                   .map(CristinMapper::generatePublication);
    }
}