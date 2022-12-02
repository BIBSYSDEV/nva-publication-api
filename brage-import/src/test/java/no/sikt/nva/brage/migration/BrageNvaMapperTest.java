package no.sikt.nva.brage.migration;

import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.BASE_URL;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.util.Collections;
import no.sikt.nva.brage.migration.mapper.BrageNvaMapper;
import no.sikt.nva.brage.migration.mapper.ChannelType;
import no.sikt.nva.brage.migration.record.Publication;
import no.sikt.nva.brage.migration.record.PublicationContext;
import no.sikt.nva.brage.migration.record.PublicationDate;
import no.sikt.nva.brage.migration.record.PublicationDateNva;
import no.sikt.nva.brage.migration.record.Publisher;
import no.sikt.nva.brage.migration.record.Record;
import no.sikt.nva.brage.migration.record.Series;
import no.sikt.nva.brage.migration.record.Type;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import nva.commons.core.paths.UriWrapper;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class BrageNvaMapperTest {

    public static final String EXPECTED_EXTRACTED_SERIES_NUMBER = "42";
    public static final String REPORT = "Report";
    public static final String PART_OF_SERIES_VALUE_V1 = "SOMESERIES;42";
    public static final String PART_OF_SERIES_VALUE_V2 = "SOMESERIES;42:2022";
    public static final String PART_OF_SERIES_VALUE_V3 = "SOMESERIES;2022:42";
    public static final String PART_OF_SERIES_VALUE_V4 = "SOMESERIES;2022/42";
    public static final String PART_OF_SERIES_VALUE_V5 = "SOMESERIES;42/2022";
    public static final String ISBN = "9788241017575";
    public static final String SERIES = randomInteger().toString();
    public static final String PUBLISHER = randomInteger().toString();
    public static final String YEAR = randomInteger().toString();

    @Test
    public void shouldMapPublicationContextCorrectly()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var recordToMap = constructRecordToMapToPublicationContext(
            PART_OF_SERIES_VALUE_V1);
        var report = generateReport();
        var expectedPublicationContext = constructPublicationWithPublicationContextForReport(report)
                                             .getEntityDescription()
                                             .getReference()
                                             .getPublicationContext();
        var actualPublicationContext = BrageNvaMapper.toNvaPublication(recordToMap)
                                           .getEntityDescription()
                                           .getReference()
                                           .getPublicationContext();
        assertThat(expectedPublicationContext, is(equalTo(actualPublicationContext)));
    }

    @ParameterizedTest
    @ValueSource(strings = {PART_OF_SERIES_VALUE_V1, PART_OF_SERIES_VALUE_V2,
        PART_OF_SERIES_VALUE_V3, PART_OF_SERIES_VALUE_V4, PART_OF_SERIES_VALUE_V5})
    void shouldConvertPartOfSeriesValueCorrectly(String partOfSeries)
        throws InvalidIsbnException, InvalidIssnException, InvalidUnconfirmedSeriesException {
        var recordToMap = constructRecordToMapToPublicationContext(partOfSeries);
        var report = generateReport();
        var expectedPublicationContext = constructPublicationWithPublicationContextForReport(report)
                                             .getEntityDescription()
                                             .getReference()
                                             .getPublicationContext();
        var actualPublicationContext = BrageNvaMapper.toNvaPublication(recordToMap)
                                           .getEntityDescription()
                                           .getReference()
                                           .getPublicationContext();

        assertThat(expectedPublicationContext, is(equalTo(actualPublicationContext)));
    }

    private Report generateReport()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Report.Builder()
                   .withPublisher(generatePublisher(BrageNvaMapperTest.PUBLISHER, BrageNvaMapperTest.YEAR))
                   .withSeriesNumber(EXPECTED_EXTRACTED_SERIES_NUMBER)
                   .withIsbnList(Collections.singletonList(BrageNvaMapperTest.ISBN))
                   .withSeries(generateSeries(BrageNvaMapperTest.SERIES, BrageNvaMapperTest.YEAR))
                   .build();
    }

    private no.unit.nva.model.Publication constructPublicationWithPublicationContextForReport(Book publicationContext) {
        var reference = new Reference.Builder().withPublishingContext(publicationContext).build();
        var entity = new EntityDescription.Builder().withReference(reference).build();
        return new no.unit.nva.model.Publication.Builder().withEntityDescription(entity).build();
    }

    private no.sikt.nva.brage.migration.record.EntityDescription generateEntityDescriptionForReport() {
        var entityDescription = new no.sikt.nva.brage.migration.record.EntityDescription();
        var publicationDate = new PublicationDate(BrageNvaMapperTest.YEAR, new PublicationDateNva.Builder().withYear(
            BrageNvaMapperTest.YEAR).build());
        entityDescription.setPublicationDate(publicationDate);
        return entityDescription;
    }

    private no.unit.nva.model.contexttypes.Series generateSeries(String seriesIdentifier, String publicationYear) {
        return new no.unit.nva.model.contexttypes.Series(
            UriWrapper.fromUri(BASE_URL)
                .addChild(ChannelType.SERIES.getType())
                .addChild(seriesIdentifier)
                .addChild(publicationYear).getUri());
    }

    private no.unit.nva.model.contexttypes.Publisher generatePublisher(String publisherIdentifier,
                                                                       String publicationYear) {
        return new no.unit.nva.model.contexttypes.Publisher(
            UriWrapper.fromUri(BASE_URL)
                .addChild(ChannelType.PUBLISHER.getType())
                .addChild(publisherIdentifier)
                .addChild(publicationYear).getUri());
    }

    private Record constructRecordToMapToPublicationContext(String partOfSeries) {
        var record = new Record();
        record.setPublication(generatePublicationForReport(
            partOfSeries));
        record.setEntityDescription(generateEntityDescriptionForReport());
        record.setType(new Type(Collections.singletonList(REPORT), REPORT));
        return record;
    }

    private Publication generatePublicationForReport(String partOfSeries) {
        var publication = new Publication();
        publication.setIsbn(BrageNvaMapperTest.ISBN);
        publication.setIssn(randomIssn());
        publication.setPartOfSeries(partOfSeries);
        publication.setJournal(randomString());
        var publicationContext = new PublicationContext();
        publicationContext.setBragePublisher(randomString());
        publicationContext.setSeries(new Series(BrageNvaMapperTest.SERIES));
        publicationContext.setPublisher(new Publisher(BrageNvaMapperTest.PUBLISHER));
        publication.setPublicationContext(publicationContext);
        return publication;
    }
}
