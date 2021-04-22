package no.unit.nva.cristin.mapper;

import static nva.commons.core.JsonUtils.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import nva.commons.core.attempt.Try;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CristinMapperTest {

    public static final String SAMPLE_INPUT_01 = "input01.gz";
    private static final Integer NUMBER_OF_LINES_IN_RESOURCES_FILE = 100;

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

    private Stream<CristinObject> cristinObjects() throws IOException {
        return attempt(this::readJsonArray)
                   .orElse(fail -> readSeriesOfJsonObjects());
    }

    private BufferedReader newReader() throws IOException {
        InputStream inputStream = new GZIPInputStream(IoUtils.inputStreamFromResources(SAMPLE_INPUT_01));
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    private Stream<CristinObject> readJsonArray() {
        try (BufferedReader reader = newReader()) {
            String jsonString = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            return parseCristinObjectsArray(jsonString).stream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<CristinObject> parseCristinObjectsArray(String jsonString) {
        JavaType cristinObjectsList = cristinObjectsListJavaType();
        return attempt(() -> objectMapperWithEmpty.<List<CristinObject>>readValue(jsonString, cristinObjectsList))
                   .orElseThrow();
    }

    private CollectionType cristinObjectsListJavaType() {
        return objectMapperWithEmpty.getTypeFactory().constructCollectionType(List.class,
                                                                              CristinObject.class);
    }

    private Stream<CristinObject> readSeriesOfJsonObjects() throws IOException {
        return newReader().lines()
                   .map(attempt(line -> objectMapperWithEmpty.readValue(line, CristinObject.class)))
                   .map(Try::orElseThrow);
    }
}