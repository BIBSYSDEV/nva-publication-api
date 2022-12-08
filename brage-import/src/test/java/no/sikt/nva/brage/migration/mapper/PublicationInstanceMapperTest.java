//package no.sikt.nva.brage.migration.mapper;
//
//import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.equalTo;
//import static org.hamcrest.Matchers.is;
//import java.util.Collections;
//import no.sikt.nva.brage.migration.NvaType;
//import no.sikt.nva.brage.migration.record.EntityDescription;
//import no.sikt.nva.brage.migration.record.Pages;
//import no.sikt.nva.brage.migration.record.PublicationDateNva;
//import no.sikt.nva.brage.migration.record.Range;
//import no.sikt.nva.brage.migration.record.Record;
//import no.sikt.nva.brage.migration.record.Type;
//import no.unit.nva.model.PublicationDate;
//import no.unit.nva.model.instancetypes.PublicationInstance;
//import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
//import no.unit.nva.model.instancetypes.journal.JournalArticle;
//import no.unit.nva.model.pages.MonographPages;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.ValueSource;
//
//public final class PublicationInstanceMapperTest {
//
//    public static final String ISSUE = "50";
//    public static final String VOLUME = "5";
//    public static final String ARTICLE_NUMBER = "3";
//    public static final String NUMBER_OF_PAGES = "5";
//    public static final String END = "10";
//    public static final String BEGIN = "5";
//    public static final String BRAGE_PAGES = "46 s.";
//    public static final String YEAR = String.valueOf(randomInteger());
//    public static final String MONTH = String.valueOf(randomInteger());
//    public static final String DAY = String.valueOf(randomInteger());
//
//    @Test
//    void shouldMapPublicationInstanceForJournalArticle() {
//        var recordToMap = constructRecord(NvaType.JOURNAL_ARTICLE.getValue());
//        var expectedPublicationInstance = constructPublicationInstanceForJournalArticle();
//        var actualPublicationInstance = PublicationInstanceMapper.buildPublicationInstance(recordToMap);
//        assertThat(actualPublicationInstance, is(equalTo(expectedPublicationInstance)));
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = {"DegreeBachelor", "DegreeMaster", "Doctoral thesis"})
//    void shouldMapPublicationInstanceForBachelor(String type) {
//        var recordToMap = constructRecord(type);
//        var expectedPublicationInstance = constructPublicationInstanceForBachelor();
//        var actualPublicationInstance = PublicationInstanceMapper.buildPublicationInstance(recordToMap);
//        assertThat(actualPublicationInstance, is(equalTo(expectedPublicationInstance)));
//    }
//
//    private static PublicationDate constructPublicationDate() {
//        return new PublicationDate.Builder().withYear(YEAR).withMonth(MONTH).withDay(DAY).build();
//    }
//
//    private static MonographPages constructMonographPages() {
//        return new MonographPages.Builder().withPages(NUMBER_OF_PAGES).build();
//    }
//
//    private static PublicationDateNva constructPublicationDateNvaForRecord() {
//        return new PublicationDateNva.Builder()
//                   .withYear(YEAR)
//                   .withMonth(MONTH)
//                   .withDay(DAY)
//                   .build();
//    }
//
//    private PublicationInstance constructPublicationInstanceForJournalArticle() {
//        return new JournalArticle.Builder()
//                   .withPages(new no.unit.nva.model.pages.Range(BEGIN, END))
//                   .withIssue(ISSUE)
//                   .withVolume(VOLUME)
//                   .withPeerReviewed(false)
//                   .build();
//    }
//
//    private PublicationInstance constructPublicationInstanceForBachelor() {
//        return new DegreeBachelor.Builder()
//                   .withPages(constructMonographPages())
//                   .withSubmittedDate(constructPublicationDate())
//                   .build();
//    }
//
//    private Record constructRecord(String type) {
//        var record = new Record();
//        record.setType(new Type(Collections.singletonList(type), type));
//        record.setEntityDescription(constructEntityDescription());
//        return record;
//    }
//
//    private EntityDescription constructEntityDescription() {
//        var entityDescription = new EntityDescription();
//        entityDescription.setPublicationInstance(constructPublicationInstanceForRecord());
//        entityDescription.setPublicationDate(constructPublicationDateForRecord());
//        return entityDescription;
//    }
//
//    private no.sikt.nva.brage.migration.record.PublicationDate constructPublicationDateForRecord() {
//        return new no.sikt.nva.brage.migration.record.PublicationDate(YEAR, constructPublicationDateNvaForRecord());
//    }
//
//    private no.sikt.nva.brage.migration.record.PublicationInstance constructPublicationInstanceForRecord() {
//        var publicationInstance = new no.sikt.nva.brage.migration.record.PublicationInstance();
//        publicationInstance.setIssue(ISSUE);
//        publicationInstance.setVolume(VOLUME);
//        publicationInstance.setArticleNumber(ARTICLE_NUMBER);
//        publicationInstance.setPageNumber(new Pages(BRAGE_PAGES, new Range(BEGIN, END), NUMBER_OF_PAGES));
//        return publicationInstance;
//    }
//}
