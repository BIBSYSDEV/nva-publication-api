package no.unit.nva.model.testing;

import static no.unit.nva.model.testing.RandomUtils.randomPublicationDate;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.UnconfirmedOrganization;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.instancetypes.Map;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.artistic.architecture.Architecture;
import no.unit.nva.model.instancetypes.artistic.architecture.ArchitectureOutput;
import no.unit.nva.model.instancetypes.artistic.architecture.ArchitectureSubtype;
import no.unit.nva.model.instancetypes.artistic.architecture.ArchitectureSubtypeEnum;
import no.unit.nva.model.instancetypes.artistic.architecture.realization.Award;
import no.unit.nva.model.instancetypes.artistic.architecture.realization.Competition;
import no.unit.nva.model.instancetypes.artistic.architecture.realization.Exhibition;
import no.unit.nva.model.instancetypes.artistic.architecture.realization.MentionInPublication;
import no.unit.nva.model.instancetypes.artistic.design.ArtisticDesign;
import no.unit.nva.model.instancetypes.artistic.design.ArtisticDesignSubtype;
import no.unit.nva.model.instancetypes.artistic.design.ArtisticDesignSubtypeEnum;
import no.unit.nva.model.instancetypes.artistic.design.realization.Venue;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.film.MovingPictureSubtype;
import no.unit.nva.model.instancetypes.artistic.film.MovingPictureSubtypeEnum;
import no.unit.nva.model.instancetypes.artistic.film.realization.Broadcast;
import no.unit.nva.model.instancetypes.artistic.film.realization.CinematicRelease;
import no.unit.nva.model.instancetypes.artistic.film.realization.MovingPictureOutput;
import no.unit.nva.model.instancetypes.artistic.film.realization.OtherRelease;
import no.unit.nva.model.instancetypes.artistic.literaryarts.LiteraryArts;
import no.unit.nva.model.instancetypes.artistic.literaryarts.LiteraryArtsSubtype;
import no.unit.nva.model.instancetypes.artistic.literaryarts.LiteraryArtsSubtypeEnum;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsAudioVisual;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsAudioVisualSubtype;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsAudioVisualSubtypeEnum;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsManifestation;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsMonograph;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsPerformance;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsPerformanceSubtype;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsPerformanceSubtypeEnum;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsWeb;
import no.unit.nva.model.instancetypes.artistic.music.AudioVisualPublication;
import no.unit.nva.model.instancetypes.artistic.music.Concert;
import no.unit.nva.model.instancetypes.artistic.music.Ismn;
import no.unit.nva.model.instancetypes.artistic.music.Isrc;
import no.unit.nva.model.instancetypes.artistic.music.MusicMediaSubtype;
import no.unit.nva.model.instancetypes.artistic.music.MusicMediaSubtypeOther;
import no.unit.nva.model.instancetypes.artistic.music.MusicMediaType;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformanceManifestation;
import no.unit.nva.model.instancetypes.artistic.music.MusicScore;
import no.unit.nva.model.instancetypes.artistic.music.MusicTrack;
import no.unit.nva.model.instancetypes.artistic.music.MusicalWork;
import no.unit.nva.model.instancetypes.artistic.music.MusicalWorkPerformance;
import no.unit.nva.model.instancetypes.artistic.music.OtherPerformance;
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArts;
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArtsSubtype;
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArtsSubtypeEnum;
import no.unit.nva.model.instancetypes.artistic.performingarts.realization.PerformingArtsOutput;
import no.unit.nva.model.instancetypes.artistic.performingarts.realization.PerformingArtsVenue;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArts;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArtsSubtype;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArtsSubtypeEnum;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArtsSubtypeOther;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.book.BookAbstracts;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.Encyclopedia;
import no.unit.nva.model.instancetypes.book.ExhibitionCatalog;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;
import no.unit.nva.model.instancetypes.book.PopularScienceMonograph;
import no.unit.nva.model.instancetypes.book.Textbook;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.chapter.ChapterConferenceAbstract;
import no.unit.nva.model.instancetypes.chapter.ChapterInReport;
import no.unit.nva.model.instancetypes.chapter.EncyclopediaChapter;
import no.unit.nva.model.instancetypes.chapter.ExhibitionCatalogChapter;
import no.unit.nva.model.instancetypes.chapter.Introduction;
import no.unit.nva.model.instancetypes.chapter.NonFictionChapter;
import no.unit.nva.model.instancetypes.chapter.PopularScienceChapter;
import no.unit.nva.model.instancetypes.chapter.TextbookChapter;
import no.unit.nva.model.instancetypes.degree.ConfirmedDocument;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;
import no.unit.nva.model.instancetypes.degree.UnconfirmedDocument;
import no.unit.nva.model.instancetypes.event.ConferenceLecture;
import no.unit.nva.model.instancetypes.event.ConferencePoster;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.event.OtherPresentation;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProduction;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProductionSubtype;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProductionSubtypeEnum;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProductionSubtypeOther;
import no.unit.nva.model.instancetypes.exhibition.manifestations.ExhibitionBasic;
import no.unit.nva.model.instancetypes.exhibition.manifestations.ExhibitionCatalogReference;
import no.unit.nva.model.instancetypes.exhibition.manifestations.ExhibitionMentionInPublication;
import no.unit.nva.model.instancetypes.exhibition.manifestations.ExhibitionOtherPresentation;
import no.unit.nva.model.instancetypes.exhibition.manifestations.ExhibitionProductionManifestation;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.AcademicLiteratureReview;
import no.unit.nva.model.instancetypes.journal.CaseReport;
import no.unit.nva.model.instancetypes.journal.ConferenceAbstract;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.instancetypes.journal.JournalCorrigendum;
import no.unit.nva.model.instancetypes.journal.JournalInterview;
import no.unit.nva.model.instancetypes.journal.JournalIssue;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.JournalLetter;
import no.unit.nva.model.instancetypes.journal.JournalReview;
import no.unit.nva.model.instancetypes.journal.PopularScienceArticle;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;
import no.unit.nva.model.instancetypes.journal.StudyProtocol;
import no.unit.nva.model.instancetypes.media.MediaBlogPost;
import no.unit.nva.model.instancetypes.media.MediaFeatureArticle;
import no.unit.nva.model.instancetypes.media.MediaInterview;
import no.unit.nva.model.instancetypes.media.MediaParticipationInRadioOrTv;
import no.unit.nva.model.instancetypes.media.MediaPodcast;
import no.unit.nva.model.instancetypes.media.MediaReaderOpinion;
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportBookOfAbstract;
import no.unit.nva.model.instancetypes.report.ReportPolicy;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;
import no.unit.nva.model.instancetypes.researchdata.CompliesWithUris;
import no.unit.nva.model.instancetypes.researchdata.DataManagementPlan;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import no.unit.nva.model.instancetypes.researchdata.GeographicalDescription;
import no.unit.nva.model.instancetypes.researchdata.ReferencedByUris;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import no.unit.nva.model.time.Instant;
import no.unit.nva.model.time.Period;
import no.unit.nva.model.time.Time;
import no.unit.nva.model.time.duration.NullDuration;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.GodClass" })
public final class PublicationInstanceBuilder {

    private static final String VALID_ISMN_10 = "M-2306-7118-7";
    private static final String VALID_ISMN_13 = "979-0-9016791-7-7";

    private PublicationInstanceBuilder() {

    }

    public static PublicationInstance<? extends Pages> randomPublicationInstance() {
        Class<?> randomType = randomPublicationInstanceType();
        return randomPublicationInstance(randomType);
    }

    // TODO: remove JournalArticle following migration

    @SuppressWarnings({"PMD.NcssCount", "PMD.ExcessiveMethodLength" })
    public static PublicationInstance<? extends Pages> randomPublicationInstance(Class<?> randomType) {
        var typeName = randomType.getSimpleName();

        switch (typeName) {
            case "LiteraryArts":
                return generateLiteraryArts();
            case "PerformingArts":
                return generatePerformingArts();
            case "MovingPicture":
                return generateRandomMotionPicture();
            case "Architecture":
                return generateRandomArchitecture();
            case "ArtisticDesign":
                return generateRandomArtisticDesign();
            case "VisualArts":
                return generateVisualArts();
            case "FeatureArticle":
                return generateFeatureArticle();
            case "JournalCorrigendum":
                return generateJournalCorrigendum();
            case "JournalArticle":
            case "AcademicArticle":
                return generateAcademicArticle();
            case "AcademicLiteratureReview":
                return generateAcademicLiteratureReview();
            case "CaseReport":
                return generateCaseReport();
            case "StudyProtocol":
                return generateStudyProtocol();
            case "ProfessionalArticle":
                return generateProfessionalArticle();
            case "PopularScienceArticle":
                return generatePopularScienceArticle();
            case "BookAnthology":
                return generateBookAnthology();
            case "ChapterArticle":
            case "AcademicChapter":
                return generateAcademicChapter();
            case "NonFictionChapter":
                return generateNonFictionChapter();
            case "PopularScienceChapter":
                return generatePopularScienceChapter();
            case "TextbookChapter":
                return generateTextbookChapter();
            case "EncyclopediaChapter":
                return generateEncyclopediaChapter();
            case "Introduction":
                return generateIntroduction();
            case "ExhibitionCatalogChapter":
                return generateExhibitionCatalogChapter();
            case "ChapterConferenceAbstract":
                return generateChapterConferenceAbstract();
            case "ChapterInReport":
                return generateChapterInReport();
            case "JournalInterview":
                return generateJournalInterview();
            case "JournalLetter":
                return generateJournalLetter();
            case "JournalLeader":
                return generateJournalLeader();
            case "JournalReview":
                return generateJournalReview();
            case "BookAbstracts":
                return generateBookAbstracts();
            case "BookMonograph":
            case "AcademicMonograph":
                return generateAcademicMonograph();
            case "NonFictionMonograph":
                return generateNonFictionMonograph();
            case "PopularScienceMonograph":
                return generatePopularScienceMonograph();
            case "Textbook":
                return generateTextbook();
            case "Encyclopedia":
                return generateEncyclopedia();
            case "ExhibitionCatalog":
                return generateExhibitionCatalog();
            case "DegreeBachelor":
                return generateDegreeBachelor();
            case "DegreeMaster":
                return generateDegreeMaster();
            case "DegreePhd":
                return generateDegreePhd();
            case "DegreeLicentiate":
                return generateDegreeLicentiate();
            case "ReportBasic":
                return generateReportBasic();
            case "ReportPolicy":
                return generateReportPolicy();
            case "ReportResearch":
                return generateReportResearch();
            case "ReportWorkingPaper":
                return generateReportWorkingPaper();
            case "ReportBookOfAbstract":
                return generateReportBookOfAbstract();
            case "ConferenceReport":
                return generateConferenceReport();
            case "OtherStudentWork":
                return generateOtherStudentWork();
            case "ConferenceLecture":
                return generateConferenceLecture();
            case "ConferencePoster":
                return generateConferencePoster();
            case "Lecture":
                return generateLecture();
            case "OtherPresentation":
                return generateOtherPresentation();
            case "JournalIssue":
                return generateJournalIssue();
            case "ConferenceAbstract":
                return generateConferenceAbstract();
            case "MediaFeatureArticle":
                return generateMediaFeatureArticle();
            case "MediaBlogPost":
                return generateMediaBlogPost();
            case "MediaInterview":
                return generateMediaInterview();
            case "MediaParticipationInRadioOrTv":
                return generateMediaParticipation();
            case "MediaPodcast":
                return generateMediaPodcast();
            case "MediaReaderOpinion":
                return generateMediaReaderOpinion();
            case "MusicPerformance":
                return generateMusicPerformance();
            case "DataManagementPlan":
                return generateDataManagementPlan();
            case "DataSet":
                return generateDataSet();
            case "Map":
                return generateMap();
            case "ExhibitionProduction":
                return generateExhibitionProduction();
            default:
                throw new UnsupportedOperationException("Publication instance not supported: " + typeName);
        }
    }

    public static Class<?> randomPublicationInstanceType() {
        List<Class<?>> publicationInstanceClasses = listPublicationInstanceTypes();
        return randomElement(publicationInstanceClasses);
    }

    public static Stream<Class<?>> journalArticleInstanceTypes() {
        return listPublicationInstanceTypes().stream()
                .map(PublicationInstanceBuilder::getPublicationContext)
                .filter(contextAndInstanceTuple -> contextAndInstanceTuple.getContext() instanceof Journal)
                .map(ContextAndInstanceTuple::getInstanceType);
    }

    public static List<Class<?>> listPublicationInstanceTypes() {
        JsonSubTypes[] annotations = PublicationInstance.class.getAnnotationsByType(JsonSubTypes.class);
        Type[] types = annotations[0].value();
        return Arrays.stream(types).map(Type::value).collect(Collectors.toList());
    }

    public static Time randomTime() {
        var randomInstant = (Time) randomNvaInstant();
        var randomPeriod = (Time) randomNvaPeriod();
        return randomElement(randomInstant, randomPeriod);
    }

    public static UnconfirmedPlace randomUnconfirmedPlace() {
        return new UnconfirmedPlace(randomString(), randomString());
    }

    private static ExhibitionProduction generateExhibitionProduction() {
        var subtype = generateRandomExhibitionProductionSubtype();
        return new ExhibitionProduction(subtype, generateRandomExhibitionThings());
    }

    private static ExhibitionProductionSubtype generateRandomExhibitionProductionSubtype() {
        var subtype = randomElement(ExhibitionProductionSubtypeEnum.values());
        return ExhibitionProductionSubtypeEnum.OTHER.equals(subtype)
                ? new ExhibitionProductionSubtypeOther(subtype, randomString())
                : new ExhibitionProductionSubtype(subtype);
    }

    private static List<ExhibitionProductionManifestation> generateRandomExhibitionThings() {
        return List.of(
                randomExhibitionCatalogReference(),
                randomExhibitionBasic(),
                randomExhibitionMentionInPublication(),
                randomExhibitionOtherPresentation()
        );
    }

    private static ExhibitionOtherPresentation randomExhibitionOtherPresentation() {
        return new ExhibitionOtherPresentation(randomString(), randomString(), randomUnconfirmedPlace(),
                randomUnconfirmedPublisher(), randomNvaInstant());
    }

    private static ExhibitionMentionInPublication randomExhibitionMentionInPublication() {
        return new ExhibitionMentionInPublication(randomString(), randomString(), randomString(), randomNvaInstant(),
                randomString());
    }

    private static ExhibitionBasic randomExhibitionBasic() {
        return new ExhibitionBasic(randomUnconfirmedOrganization(), randomUnconfirmedPlace(), randomNvaPeriod());
    }

    private static ExhibitionCatalogReference randomExhibitionCatalogReference() {
        return new ExhibitionCatalogReference(randomUri());
    }

    private static UnconfirmedOrganization randomUnconfirmedOrganization() {
        return new UnconfirmedOrganization(randomString());
    }

    private static DataSet generateDataSet() {
        var geographicalCoverage = new GeographicalDescription(randomString());
        var referencedUri = new ReferencedByUris(Set.of(randomUri()));
        var compliesWithUris = new CompliesWithUris(Set.of(randomUri()));
        return new DataSet(randomBoolean(),
                geographicalCoverage, referencedUri, Set.of(new UnconfirmedDocument(randomString())), compliesWithUris);
    }

    private static DataManagementPlan generateDataManagementPlan() {
        return new DataManagementPlan(Set.of(new UnconfirmedDocument(randomString())), randomMonographPages());
    }

    private static Map generateMap() {
        return new Map(randomString(), randomMonographPages());
    }

    private static MusicPerformance generateMusicPerformance() {
        return new MusicPerformance(List.of(randomAudioVisualPublication(),
                randomConcert(),
                randomMusicScore(),
                randomOtherPerformance()), NullDuration.create());
    }

    private static MusicPerformanceManifestation randomOtherPerformance() {
        //performanceType, place, extent
        return new OtherPerformance(randomString(), randomUnconfirmedPlace(), randomString(),
                List.of(randomWork()));
    }

    private static MusicalWork randomWork() {
        return new MusicalWork(randomString(), randomString());
    }

    private static MusicPerformanceManifestation randomMusicScore() {
        //ensemble movements extent, publisher,
        return new MusicScore(randomString(),
                randomString(),
                randomString(),
                randomUnconfirmedPublisher(),
                randomIsmn());
    }

    private static Isrc randomIsrc() {
        return attempt(() -> new Isrc("USRC17607839")).orElseThrow();
    }

    private static Ismn randomIsmn() {
        return attempt(() -> new Ismn(randomElement(VALID_ISMN_13, VALID_ISMN_10))).orElseThrow();
    }

    private static UnconfirmedPublisher randomUnconfirmedPublisher() {
        return new UnconfirmedPublisher(randomString());
    }

    private static Concert randomConcert() {
        return new Concert(randomUnconfirmedPlace(),
                randomTime(),
                randomString(),
                randomConcertProgramme(),
                randomString());
    }

    private static List<MusicalWorkPerformance> randomConcertProgramme() {
        return List.of(new MusicalWorkPerformance(randomString(), randomString(), randomBoolean()));
    }

    private static MusicPerformanceManifestation randomAudioVisualPublication() {
        return new AudioVisualPublication(randomMusicMediaSubType(),
                randomUnconfirmedPublisher(),
                randomString(),
                randomTrackList(),
                randomIsrc());
    }

    private static MusicMediaSubtype randomMusicMediaSubType() {
        var musicMediaType = randomElement(MusicMediaType.values());
        return MusicMediaType.OTHER.equals(musicMediaType)
                ? new MusicMediaSubtypeOther(musicMediaType, randomString())
                : new MusicMediaSubtype(musicMediaType);
    }

    private static List<MusicTrack> randomTrackList() {
        return List.of(new MusicTrack(randomString(), randomString(), randomString()));
    }

    private static MediaReaderOpinion generateMediaReaderOpinion() {
        return new MediaReaderOpinion(randomVolume(), randomIssue(), randomArticleNumber(), randomRange());
    }

    private static MediaPodcast generateMediaPodcast() {
        return new MediaPodcast();
    }

    private static MediaParticipationInRadioOrTv generateMediaParticipation() {
        return new MediaParticipationInRadioOrTv();
    }

    private static MediaInterview generateMediaInterview() {
        return new MediaInterview();
    }

    private static MediaBlogPost generateMediaBlogPost() {
        return new MediaBlogPost();
    }

    private static MediaFeatureArticle generateMediaFeatureArticle() {
        return new MediaFeatureArticle(randomVolume(), randomIssue(), randomArticleNumber(), randomRange());
    }

    private static JournalIssue generateJournalIssue() {
        return new JournalIssue.Builder()
                .withArticleNumber(randomArticleNumber())
                .withIssue(randomIssue())
                .withPages(randomRange())
                .withVolume(randomVolume())
                .build();
    }

    private static ConferenceAbstract generateConferenceAbstract() {
        return new ConferenceAbstract.Builder()
                .withArticleNumber(randomArticleNumber())
                .withIssue(randomIssue())
                .withPages(randomRange())
                .withVolume(randomVolume())
                .build();
    }

    private static ContextAndInstanceTuple getPublicationContext(Class<?> instanceType) {
        var context = PublicationContextBuilder.randomPublicationContext(instanceType);
        return new ContextAndInstanceTuple(context, instanceType);
    }

    private static OtherPresentation generateOtherPresentation() {
        return new OtherPresentation();
    }

    private static Lecture generateLecture() {
        return new Lecture();
    }

    private static ConferencePoster generateConferencePoster() {
        return new ConferencePoster();
    }

    private static ConferenceLecture generateConferenceLecture() {
        return new ConferenceLecture();
    }

    private static OtherStudentWork generateOtherStudentWork() {
        return new OtherStudentWork(randomMonographPages(), randomPublicationDate());
    }

    private static ReportWorkingPaper generateReportWorkingPaper() {
        return new ReportWorkingPaper(randomMonographPages());
    }

    private static ReportBookOfAbstract generateReportBookOfAbstract() {
        return new ReportBookOfAbstract(randomMonographPages());
    }

    private static ConferenceReport generateConferenceReport() {
        return new ConferenceReport(randomMonographPages());
    }

    private static ReportResearch generateReportResearch() {
        return new ReportResearch(randomMonographPages());
    }

    private static ReportPolicy generateReportPolicy() {
        return new ReportPolicy(randomMonographPages());
    }

    private static ReportBasic generateReportBasic() {
        return new ReportBasic(randomMonographPages());
    }

    private static DegreePhd generateDegreePhd() {
        return new DegreePhd(randomMonographPages(), randomPublicationDate(),
                             Set.of(new ConfirmedDocument(randomUri()),
                                    new UnconfirmedDocument(randomString())));
    }

    private static DegreeLicentiate generateDegreeLicentiate() {
        return new DegreeLicentiate(randomMonographPages(), randomPublicationDate());
    }

    private static DegreeMaster generateDegreeMaster() {
        return new DegreeMaster(randomMonographPages(), randomPublicationDate());
    }

    private static DegreeBachelor generateDegreeBachelor() {
        return new DegreeBachelor(randomMonographPages(), randomPublicationDate());
    }

    private static AcademicMonograph generateAcademicMonograph() {
        return new AcademicMonograph(randomMonographPages());
    }

    private static ExhibitionCatalog generateExhibitionCatalog() {
        return new ExhibitionCatalog(randomMonographPages());
    }

    private static Encyclopedia generateEncyclopedia() {
        return new Encyclopedia(randomMonographPages());
    }

    private static Textbook generateTextbook() {
        return new Textbook(randomMonographPages());
    }

    private static PopularScienceMonograph generatePopularScienceMonograph() {
        return new PopularScienceMonograph(randomMonographPages());
    }

    private static NonFictionMonograph generateNonFictionMonograph() {
        return new NonFictionMonograph(randomMonographPages());
    }

    private static BookAbstracts generateBookAbstracts() {
        return new BookAbstracts(randomMonographPages());
    }

    private static JournalReview generateJournalReview() {
        return new JournalReview.Builder()
                .withArticleNumber(randomArticleNumber())
                .withIssue(randomIssue())
                .withPages(randomRange())
                .withVolume(randomVolume())
                .build();
    }

    private static JournalLeader generateJournalLeader() {
        return new JournalLeader.Builder()
                .withArticleNumber(randomArticleNumber())
                .withIssue(randomIssue())
                .withPages(randomRange())
                .withVolume(randomVolume())
                .build();
    }

    private static JournalLetter generateJournalLetter() {
        return new JournalLetter.Builder()
                .withArticleNumber(randomArticleNumber())
                .withIssue(randomIssue())
                .withPages(randomRange())
                .withVolume(randomVolume())
                .build();
    }

    private static JournalInterview generateJournalInterview() {
        return new JournalInterview.Builder()
                .withArticleNumber(randomArticleNumber())
                .withIssue(randomIssue())
                .withPages(randomRange())
                .withVolume(randomVolume())
                .build();
    }

    private static AcademicChapter generateAcademicChapter() {
        return new AcademicChapter(randomRange());
    }

    private static ExhibitionCatalogChapter generateExhibitionCatalogChapter() {
        return new ExhibitionCatalogChapter(randomRange());
    }

    private static Introduction generateIntroduction() {
        return new Introduction(randomRange());
    }

    private static EncyclopediaChapter generateEncyclopediaChapter() {
        return new EncyclopediaChapter(randomRange());
    }

    private static TextbookChapter generateTextbookChapter() {
        return new TextbookChapter(randomRange());
    }

    private static PopularScienceChapter generatePopularScienceChapter() {
        return new PopularScienceChapter(randomRange());
    }

    private static NonFictionChapter generateNonFictionChapter() {
        return new NonFictionChapter(randomRange());
    }

    private static ChapterConferenceAbstract generateChapterConferenceAbstract() {
        return new ChapterConferenceAbstract(randomRange());
    }

    private static ChapterInReport generateChapterInReport() {
        return new ChapterInReport.Builder()
                .withPages(randomRange())
                .build();
    }

    private static BookAnthology generateBookAnthology() {
        return new BookAnthology(randomMonographPages());
    }

    public static MonographPages randomMonographPages() {
        return new MonographPages.Builder()
                .withPages(randomPagesString())
                .withIllustrated(randomBoolean())
                .withIntroduction(randomRange())
                .build();
    }

    private static Range randomRange() {
        return new Range.Builder().withBegin(randomPagesString()).withEnd(randomPagesString()).build();
    }

    private static String randomPagesString() {
        return randomString();
    }

    private static JournalCorrigendum generateJournalCorrigendum() {
        return new JournalCorrigendum.Builder()
                .withArticleNumber(randomArticleNumber())
                .withIssue(randomIssue())
                .withVolume(randomVolume())
                .withPages(randomRange())
                .withCorrigendumFor(randomUri())
                .build();
    }

    private static AcademicArticle generateAcademicArticle() {
        return new AcademicArticle(randomRange(), randomVolume(), randomIssue(), randomArticleNumber());
    }

    private static AcademicLiteratureReview generateAcademicLiteratureReview() {
        return new AcademicLiteratureReview(randomRange(), randomVolume(), randomIssue(),
                randomArticleNumber());
    }

    private static CaseReport generateCaseReport() {
        return new CaseReport(randomRange(), randomVolume(), randomIssue(),
                randomArticleNumber());
    }

    private static StudyProtocol generateStudyProtocol() {
        return new StudyProtocol(randomRange(), randomVolume(), randomIssue(),
                randomArticleNumber());
    }

    private static ProfessionalArticle generateProfessionalArticle() {
        return new ProfessionalArticle(randomRange(), randomVolume(), randomIssue(), randomArticleNumber());
    }

    private static PopularScienceArticle generatePopularScienceArticle() {
        return new PopularScienceArticle(randomRange(), randomVolume(), randomIssue(),
                randomArticleNumber());
    }

    private static String randomArticleNumber() {
        return randomString();
    }

    private static String randomIssue() {
        return randomString();
    }

    private static FeatureArticle generateFeatureArticle() {
        return new FeatureArticle.Builder()
                .withArticleNumber(randomArticleNumber())
                .withIssue(randomIssue())
                .withPages(randomRange())
                .withVolume(randomVolume())
                .build();
    }

    private static String randomVolume() {
        return randomString();
    }

    private static PublicationInstance<? extends Pages> generateLiteraryArts() {
        var subtype = randomElement(LiteraryArtsSubtypeEnum.values());
        return literaryArts(subtype);
    }

    private static PublicationInstance<? extends Pages> literaryArts(LiteraryArtsSubtypeEnum subtype) {
        var literaryArtsSubtype = literaryArtsSubtype(subtype);
        return new LiteraryArts(literaryArtsSubtype, randomLiteraryArtsManifestationList(), randomString());
    }

    private static List<LiteraryArtsManifestation> randomLiteraryArtsManifestationList() {
        var isbn = "9780099470434";
        var monograph = new LiteraryArtsMonograph(randomUnconfirmedPublisher(), randomPublicationDate(), List.of(isbn),
                                                  randomMonographPages());
        var audioVisual = new LiteraryArtsAudioVisual(randomLiteraryArtsAudioVisualSubtype(),
                                                      randomUnconfirmedPublisher(), randomPublicationDate(),
                                                      List.of(isbn), randomInteger());
        var performance = new LiteraryArtsPerformance(randomLiteraryArtsPerformanceSubtype(),
                                                      randomUnconfirmedPlace(), randomPublicationDate());
        var web = new LiteraryArtsWeb(randomUri(), randomUnconfirmedPublisher(), randomPublicationDate());
        return List.of(monograph, audioVisual, performance, web);
    }

    private static LiteraryArtsPerformanceSubtype randomLiteraryArtsPerformanceSubtype() {
        var subtype = randomElement(LiteraryArtsPerformanceSubtypeEnum.values());
        return LiteraryArtsPerformanceSubtypeEnum.OTHER.equals(subtype)
            ? LiteraryArtsPerformanceSubtype.createOther(randomString())
            : LiteraryArtsPerformanceSubtype.create(subtype);
    }

    public static LiteraryArtsAudioVisualSubtype randomLiteraryArtsAudioVisualSubtype() {
        var subtype = randomElement(LiteraryArtsAudioVisualSubtypeEnum.values());

        return LiteraryArtsAudioVisualSubtypeEnum.OTHER.equals(subtype)
                ? LiteraryArtsAudioVisualSubtype.createOther(randomString())
                : LiteraryArtsAudioVisualSubtype.create(subtype);
    }

    private static LiteraryArtsSubtype literaryArtsSubtype(LiteraryArtsSubtypeEnum subtype) {
        return LiteraryArtsSubtypeEnum.OTHER.equals(subtype)
                ? LiteraryArtsSubtype.createOther(randomString())
                : LiteraryArtsSubtype.create(subtype);
    }

    private static PublicationInstance<? extends Pages> generatePerformingArts() {
        var subtype = randomElement(PerformingArtsSubtypeEnum.values());
        return performingArts(subtype);
    }

    private static PublicationInstance<? extends Pages> generateRandomMotionPicture() {
        var subtype = randomElement(MovingPictureSubtypeEnum.values());
        return movingPicture(subtype);
    }

    private static PublicationInstance<? extends Pages> generateRandomArchitecture() {
        var subtype = randomElement(ArchitectureSubtypeEnum.values());
        return architecture(subtype);
    }

    private static PublicationInstance<? extends Pages> performingArts(PerformingArtsSubtypeEnum subtype) {
        PerformingArtsSubtype performingArtsSubtype = performingArtsSubtype(subtype);
        return new PerformingArts(performingArtsSubtype, randomString(), randomPerformingArtsOutputs());
    }

    private static PerformingArtsSubtype performingArtsSubtype(PerformingArtsSubtypeEnum subtype) {
        return PerformingArtsSubtypeEnum.OTHER == subtype
                ? PerformingArtsSubtype.createOther(randomString())
                : PerformingArtsSubtype.create(subtype);
    }

    private static List<PerformingArtsOutput> randomPerformingArtsOutputs() {
        return List.of(performingArtsVenue());
    }

    private static PerformingArtsVenue performingArtsVenue() {
        return new PerformingArtsVenue(randomUnconfirmedPlace(), randomNvaPeriod(), randomInteger());
    }

    private static PublicationInstance<? extends Pages> movingPicture(MovingPictureSubtypeEnum subtype) {
        return new MovingPicture(getMovingPictureSubtype(subtype), randomString(), randomMovingPictureOutputs(),
                                 NullDuration.create());
    }

    private static MovingPictureSubtype getMovingPictureSubtype(MovingPictureSubtypeEnum subtype) {
        return MovingPictureSubtypeEnum.OTHER == subtype
                ? MovingPictureSubtype.createOther(randomString())
                : MovingPictureSubtype.create(subtype);
    }

    private static List<MovingPictureOutput> randomMovingPictureOutputs() {
        return List.of(streaming(), cinematicRelease(), otherRelease());
    }

    private static OtherRelease otherRelease() {
        return new OtherRelease(randomString(), randomUnconfirmedPlace(), new UnconfirmedPublisher(randomString()),
                randomNvaInstant(), randomInteger());
    }

    private static CinematicRelease cinematicRelease() {
        return new CinematicRelease(randomUnconfirmedPlace(), randomNvaInstant(), randomInteger());
    }

    private static Broadcast streaming() {
        return new Broadcast(new UnconfirmedPublisher(randomString()), randomNvaInstant(), randomInteger());
    }

    private static PublicationInstance<? extends Pages> architecture(ArchitectureSubtypeEnum subtype) {
        return new Architecture(architectureSubtype(subtype), randomString(), randomArchitectureOutputs());
    }

    private static ArchitectureSubtype architectureSubtype(ArchitectureSubtypeEnum subtype) {
        return ArchitectureSubtypeEnum.OTHER == subtype
                ? ArchitectureSubtype.createOther(randomString())
                : ArchitectureSubtype.create(subtype);
    }

    private static List<ArchitectureOutput> randomArchitectureOutputs() {
        return List.of(randomCompetition(), randomMentionInPublication(), randomAward(), randomExhibition());
    }

    private static ArchitectureOutput randomAward() {
        return new Award(randomString(), randomString(), randomNvaInstant(), randomInteger(),
                randomString(), randomInteger());
    }

    private static ArchitectureOutput randomCompetition() {
        return new Competition(randomString(), randomString(), randomNvaInstant(), randomInteger());
    }

    private static Exhibition randomExhibition() {
        return new Exhibition(randomString(), randomUnconfirmedPlace(), randomString(), randomNvaPeriod(),
                randomString(), randomInteger());
    }

    private static ArchitectureOutput randomMentionInPublication() {
        return new MentionInPublication(randomString(), randomIssue(), randomNvaInstant(),
                randomString(), randomInteger());
    }

    private static VisualArts generateVisualArts() {
        var subtype = randomVisualArtsSubtype();
        var description = randomString();
        var venues = Set.of(randomVenue());
        return new VisualArts(subtype, description, venues);
    }

    private static VisualArtsSubtype randomVisualArtsSubtype() {
        var subtype = randomElement(VisualArtsSubtypeEnum.values());
        return VisualArtsSubtypeEnum.OTHER.equals(subtype)
                ? new VisualArtsSubtypeOther(subtype, randomString())
                : VisualArtsSubtype.create(subtype);
    }

    private static ArtisticDesign generateRandomArtisticDesign() {
        var subtype = randomElement(ArtisticDesignSubtypeEnum.values());
        return artisticDesign(subtype);
    }

    private static ArtisticDesign artisticDesign(ArtisticDesignSubtypeEnum subtype) {
        var materializedSubtype = ArtisticDesignSubtypeEnum.OTHER.equals(subtype)
                ? ArtisticDesignSubtype.createOther(randomString())
                : ArtisticDesignSubtype.create(subtype);
        return new ArtisticDesign(materializedSubtype, randomString(), randomVenues());
    }

    private static List<Venue> randomVenues() {
        return List.of(randomVenue(), randomVenue());
    }

    private static Venue randomVenue() {
        return new Venue(randomUnconfirmedPlace(), randomNvaPeriod(), randomInteger());
    }

    private static Instant randomNvaInstant() {
        return new Instant(randomInstant());
    }

    private static Period randomNvaPeriod() {
        var from = randomInstant();
        return new Period(from, randomInstant(from));
    }
}
