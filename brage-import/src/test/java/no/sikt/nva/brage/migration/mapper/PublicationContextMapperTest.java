package no.sikt.nva.brage.migration.mapper;

import static no.sikt.nva.brage.migration.mapper.PublicationContextMapper.BASE_URL;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn10;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.net.URI;
import java.util.Collections;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.record.EntityDescription;
import no.sikt.nva.brage.migration.record.Publication;
import no.sikt.nva.brage.migration.record.PublicationDate;
import no.sikt.nva.brage.migration.record.PublicationDateNva;
import no.sikt.nva.brage.migration.record.Record;
import no.sikt.nva.brage.migration.record.Type;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import org.junit.jupiter.api.Test;

public class PublicationContextMapperTest {

    public static final String PUBLISHER_STRING = String.valueOf(randomInteger());
    public static final String PUBLICATION_YEAR = String.valueOf(randomInteger());
    public static final URI PUBLISHER_URI = constructPublisherUriId();
    public static final String SERIES_STRING = String.valueOf(randomInteger());
    public static final URI SERIES_URI = constructSeriesUriId();
    public static final String ISBN = randomIsbn10();
    public static final String SERIES_NUMBER = randomString();
    public static final String CORRESPONDING_PART_OF_SERIES_NUMBER = randomString() + ";" + SERIES_NUMBER;
    public static final String PUBLISHER_BRAGE = randomString();

    @Test
    void shouldMapPublicationContextForReport()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var recordToExtractFrom = constructRecord(NvaType.REPORT.getValue());
        var expectedPublicationContext = constructPublicationContext();
        var actualPublicationContext = PublicationContextMapper.buildPublicationContext(recordToExtractFrom);
        assertThat(actualPublicationContext, is(equalTo(expectedPublicationContext)));
    }

    @Test
    void shouldMapPublicationContextForBook()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var recordToExtractFrom = constructRecord(NvaType.BOOK.getValue());
        var expectedPublicationContext = constructPublicationContext();
        var actualPublicationContext = PublicationContextMapper.buildPublicationContext(recordToExtractFrom);
        assertThat(actualPublicationContext, is(equalTo(expectedPublicationContext)));
    }

    @Test
    void shouldReturnNullForPublicationContextWhenUnknownType() throws InvalidIssnException, InvalidIsbnException,
                                                                       InvalidUnconfirmedSeriesException {
        var recordToExtractFrom = constructRecord(null);
        var actualPublicationContext = PublicationContextMapper.buildPublicationContext(recordToExtractFrom);
        assertThat(actualPublicationContext, is(nullValue()));
    }

    private static URI constructPublisherUriId() {
        return URI.create(
            BASE_URL + "/" + ChannelType.PUBLISHER.getType() + "/" + PUBLISHER_STRING + "/" + PUBLICATION_YEAR);
    }

    private static URI constructSeriesUriId() {
        return URI.create(BASE_URL + "/" + ChannelType.SERIES.getType() + "/" + SERIES_STRING + "/" + PUBLICATION_YEAR);
    }

    private Record constructRecord(String type) {
        var record = new Record();
        record.setEntityDescription(constructEntityDescription());
        record.setType(new Type(Collections.singletonList(type), type));
        record.setPublication(createPublication(
        ));
        return record;
    }

    private EntityDescription constructEntityDescription() {
        var entityDescription = new EntityDescription();
        entityDescription.setPublicationDate(
            new PublicationDate(PublicationContextMapperTest.PUBLICATION_YEAR,
                                new PublicationDateNva.Builder().withYear(
                                    PublicationContextMapperTest.PUBLICATION_YEAR).build()));
        return entityDescription;
    }

    private Publication createPublication() {
        var publication = new Publication();
        publication.setIsbn(PublicationContextMapperTest.ISBN);
        publication.setPublicationContext(createPublicationContext(
        ));
        publication.setPartOfSeries(PublicationContextMapperTest.CORRESPONDING_PART_OF_SERIES_NUMBER);
        return publication;
    }

    private no.sikt.nva.brage.migration.record.PublicationContext createPublicationContext() {
        var publicationContext = new no.sikt.nva.brage.migration.record.PublicationContext();
        publicationContext.setPublisher(new no.sikt.nva.brage.migration.record.Publisher(
            PublicationContextMapperTest.PUBLISHER_STRING));
        publicationContext.setSeries(new no.sikt.nva.brage.migration.record.Series(
            PublicationContextMapperTest.SERIES_STRING));
        publicationContext.setBragePublisher(PublicationContextMapperTest.PUBLISHER_BRAGE);
        return publicationContext;
    }

    private PublicationContext constructPublicationContext()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {

        return new Report.Builder()
                   .withPublisher(new Publisher(PublicationContextMapperTest.PUBLISHER_URI))
                   .withSeries(new Series(PublicationContextMapperTest.SERIES_URI))
                   .withIsbnList(Collections.singletonList(PublicationContextMapperTest.ISBN))
                   .withSeriesNumber(PublicationContextMapperTest.SERIES_NUMBER)
                   .build();
    }
}
