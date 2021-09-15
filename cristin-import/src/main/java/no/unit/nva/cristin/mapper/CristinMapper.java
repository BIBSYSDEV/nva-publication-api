package no.unit.nva.cristin.mapper;

import static java.util.Objects.isNull;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_NVA_CUSTOMER;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_SAMPLE_DOI;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isBook;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isChapter;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isJournal;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isReport;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreeMaster;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreePhd;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Publication.Builder;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Chapter;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import nva.commons.core.attempt.Try;
import nva.commons.core.language.LanguageMapper;
import nva.commons.doi.DoiConverter;
import nva.commons.doi.DoiValidator;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class CristinMapper {

    public static final String EMPTY_STRING = "";
    public static final BookSeries EMPTY_SERIES = null;
    public static final String EMPTY_SERIES_NUMBER = null;
    private static final Config config = loadConfiguration();
    private static final boolean VALIDATE_DOI_ONLINE = parseValidateDoiOnline();
    private final CristinObject cristinObject;
    private final DoiConverter doiConverter;

    public CristinMapper(CristinObject cristinObject) {
        this.cristinObject = cristinObject;
        DoiValidator doiValidator = new DoiValidator();
        doiConverter = new DoiConverter(doiUri -> validateDoi(doiValidator, doiUri));
    }

    private static boolean validateDoi(DoiValidator doiValidator, URI doiUri) {
        return VALIDATE_DOI_ONLINE ? doiValidator.validateOnline(doiUri) : DoiValidator.validateOffline(doiUri);
    }

    public Publication generatePublication() {
        Publication publication = new Builder()
            .withAdditionalIdentifiers(Set.of(extractIdentifier()))
            .withEntityDescription(generateEntityDescription())
            .withCreatedDate(extractEntryCreationDate())
            .withPublisher(extractOrganization())
            .withOwner(cristinObject.getPublicationOwner())
            .withStatus(PublicationStatus.DRAFT)
            .withLink(HARDCODED_SAMPLE_DOI)
            .withProjects(extractProjects())
            .build();
        assertPublicationDoesNotHaveEmptyFields(publication);
        return publication;
    }

    public CristinBookOrReportMetadata extractCristinBookReport() {
        return Optional.ofNullable(cristinObject)
            .map(CristinObject::getBookOrReportMetadata)
            .orElse(null);
    }

    private static boolean parseValidateDoiOnline() {
        return config.getBoolean("doi.validation.online");
    }

    private static Config loadConfiguration() {
        return ConfigFactory.load(ConfigFactory.defaultApplication());
    }



    private void assertPublicationDoesNotHaveEmptyFields(Publication publication) {
        try {
            assertThat(publication,
                       doesNotHaveEmptyValuesIgnoringFields(IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS));
        } catch (Error error) {
            String message = error.getMessage();
            throw new MissingFieldsException(message);
        }
    }

    private List<ResearchProject> extractProjects() {
        if (cristinObject.getPresentationalWork() == null) {
            return null;
        }
        return cristinObject.getPresentationalWork()
            .stream()
            .filter(CristinPresentationalWork::isProject)
            .map(CristinPresentationalWork::toNvaResearchProject)
            .collect(Collectors.toList());
    }

    private Organization extractOrganization() {
        return new Organization.Builder().withId(HARDCODED_NVA_CUSTOMER).build();
    }

    private Instant extractEntryCreationDate() {
        return Optional.ofNullable(cristinObject.getEntryCreationDate())
            .map(ld -> ld.atStartOfDay().toInstant(zoneOffset()))
            .orElse(null);
    }

    private ZoneOffset zoneOffset() {
        return ZoneOffset.UTC.getRules().getOffset(Instant.now());
    }

    private EntityDescription generateEntityDescription() {
        return new EntityDescription.Builder()
            .withLanguage(extractLanguage())
            .withMainTitle(extractMainTitle())
            .withDate(extractPublicationDate())
            .withReference(buildReference())
            .withContributors(extractContributors())
            .withNpiSubjectHeading(extractNpiSubjectHeading())
            .withAbstract(extractAbstract())
            .withTags(extractTags())
            .build();
    }

    private List<Contributor> extractContributors() {
        return cristinObject.getContributors()
            .stream()
            .map(attempt(CristinContributor::toNvaContributor))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }

    private Reference buildReference() {
        PublicationInstanceBuilderImpl publicationInstanceBuilderImpl
            = new PublicationInstanceBuilderImpl(cristinObject);
        PublicationInstance<? extends Pages> publicationInstance
            = publicationInstanceBuilderImpl.build();
        PublicationContext publicationContext = attempt(this::buildPublicationContext).orElseThrow();
        return new Reference.Builder()
            .withPublicationInstance(publicationInstance)
            .withPublishingContext(publicationContext)
            .withDoi(extractDoi())
            .build();
    }

    private PublicationContext buildPublicationContext()
        throws InvalidIsbnException, InvalidIssnException, InvalidUnconfirmedSeriesException {
        if (isBook(cristinObject)) {
            return buildBookForPublicationContext();
        }
        if (isJournal(cristinObject)) {
            return buildUnconfirmedJournalForPublicationContext();
        }
        if (isReport(cristinObject)) {
            return buildPublicationContextWhenMainCategoryIsReport();
        }
        if (isChapter(cristinObject)) {
            return buildChapterForPublicationContext();
        }
        return null;
    }

    private Book buildBookForPublicationContext() throws InvalidIsbnException {
        List<String> isbnList = extractIsbn().stream().collect(Collectors.toList());
        return new Book(EMPTY_SERIES, EMPTY_SERIES_NUMBER, buildUnconfirmedPublisher(), isbnList);
    }

    private UnconfirmedPublisher buildUnconfirmedPublisher() {
        return new UnconfirmedPublisher(extractPublisherName());
    }

    private UnconfirmedJournal buildUnconfirmedJournalForPublicationContext() throws InvalidIssnException {
        return new UnconfirmedJournal(extractPublisherTitle(), extractIssn(), extractIssnOnline());
    }

    private PublicationContext buildPublicationContextWhenMainCategoryIsReport()
        throws InvalidIsbnException, InvalidIssnException, InvalidUnconfirmedSeriesException {
        List<String> isbnList = extractIsbn().stream().collect(Collectors.toList());
        if (isDegreePhd(cristinObject) || isDegreeMaster(cristinObject)) {
            return new Degree.Builder()
                .withPublisher(buildUnconfirmedPublisher())
                .withIsbnList(isbnList)
                .build();
        }
        return new Report.Builder()
            .withPublisher(buildUnconfirmedPublisher())
            .withIsbnList(isbnList)
            .build();
    }

    private Chapter buildChapterForPublicationContext() {
        return new Chapter.Builder()
            .build();
    }

    private PublicationDate extractPublicationDate() {
        return new PublicationDate.Builder().withYear(cristinObject.getPublicationYear()).build();
    }

    private String extractMainTitle() {
        return extractCristinTitles()
            .filter(CristinTitle::isMainTitle)
            .findFirst()
            .map(CristinTitle::getTitle)
            .orElseThrow();
    }

    private Stream<CristinTitle> extractCristinTitles() {
        return Optional.ofNullable(cristinObject)
            .map(CristinObject::getCristinTitles)
            .stream()
            .flatMap(Collection::stream);
    }

    private String extractPublisherName() {
        return extractCristinBookReport().getPublisherName();
    }

    private Optional<String> extractIsbn() {
        return Optional.ofNullable(extractCristinBookReport().getIsbn());
    }

    private URI extractLanguage() {
        return extractCristinTitles()
            .filter(CristinTitle::isMainTitle)
            .findFirst()
            .map(CristinTitle::getLanguagecode)
            .map(LanguageMapper::toUri)
            .orElse(LanguageMapper.LEXVO_URI_UNDEFINED);
    }

    private AdditionalIdentifier extractIdentifier() {
        return new AdditionalIdentifier(CristinObject.IDENTIFIER_ORIGIN, cristinObject.getId().toString());
    }

    private String extractNpiSubjectHeading() {
        CristinSubjectField subjectField = extractSubjectField();
        if (isNull(subjectField)) {
            return EMPTY_STRING;
        } else {
            return extractSubjectFieldCode(subjectField);
        }
    }

    private String extractSubjectFieldCode(CristinSubjectField subjectField) {
        return Optional.ofNullable(subjectField.getSubjectFieldCode())
            .map(String::valueOf)
            .orElseThrow(() -> new MissingFieldsException(CristinSubjectField.MISSING_SUBJECT_FIELD_CODE));
    }

    private boolean resourceShouldAlwaysHaveAnNpiSubjectHeading() {
        return cristinObject.getSecondaryCategory().equals(CristinSecondaryCategory.MONOGRAPH);
    }

    private boolean resourceTypeIsNotExpectedToHaveAnNpiSubjectHeading() {
        return !(isBook(cristinObject) || isReport(cristinObject));
    }

    private CristinSubjectField extractSubjectField() {
        if (resourceTypeIsNotExpectedToHaveAnNpiSubjectHeading()) {
            return null;
        }
        CristinSubjectField subjectField = extractCristinBookReport().getSubjectField();
        if (resourceShouldAlwaysHaveAnNpiSubjectHeading() && isNull(subjectField)) {
            throw new MissingFieldsException(CristinBookOrReportMetadata.SUBJECT_FIELD_IS_A_REQUIRED_FIELD);
        }
        return subjectField;
    }

    private CristinJournalPublication extractCristinJournalPublication() {
        return Optional.ofNullable(cristinObject)
            .map(CristinObject::getJournalPublication)
            .orElse(null);
    }

    private String extractIssn() {
        return extractCristinJournalPublication().getJournal().getIssn();
    }

    private String extractIssnOnline() {
        return extractCristinJournalPublication().getJournal().getIssnOnline();
    }

    private String extractPublisherTitle() {
        return extractCristinJournalPublication().getJournal().getJournalTitle();
    }

    private URI extractDoi() {
        if (isJournal(cristinObject)) {
            return Optional.of(extractCristinJournalPublication())
                .map(CristinJournalPublication::getDoi)
                .map(doiConverter::toUri)
                .orElse(null);
        }
        return null;
    }

    private List<CristinTags> extractCristinTags() {
        return Optional.ofNullable(cristinObject)
            .map(CristinObject::getTags)
            .orElse(null);
    }

    private String extractAbstract() {
        return extractCristinTitles()
            .filter(CristinTitle::isMainTitle)
            .findFirst()
            .map(CristinTitle::getAbstractText)
            .orElse(null);
    }

    private List<String> extractTags() {
        if (extractCristinTags() == null) {
            return null;
        }
        List<String> listOfTags = new ArrayList<>();
        for (CristinTags cristinTags : extractCristinTags()) {
            if (cristinTags.getBokmal() != null) {
                listOfTags.add(cristinTags.getBokmal());
            }
            if (cristinTags.getEnglish() != null) {
                listOfTags.add(cristinTags.getEnglish());
            }
            if (cristinTags.getNynorsk() != null) {
                listOfTags.add(cristinTags.getNynorsk());
            }
        }
        return listOfTags;
    }
}
