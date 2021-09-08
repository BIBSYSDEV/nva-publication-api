package no.unit.nva.cristin.mapper;

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
import java.net.MalformedURLException;
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
import no.unit.nva.model.contexttypes.Chapter;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import nva.commons.core.attempt.Try;
import nva.commons.core.language.LanguageMapper;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class CristinMapper {

    private final CristinObject cristinObject;

    public CristinMapper(CristinObject cristinObject) {
        this.cristinObject = cristinObject;
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
        throws InvalidIsbnException, MalformedURLException, InvalidIssnException {
        if (isBook(cristinObject)) {
            return buildBookForPublicationContext();
        }
        if (isJournal(cristinObject)) {
            return buildJournalForPublicationContext();
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
        return new Book.Builder()
                .withIsbnList(isbnList)
                .withPublisher(extractPublisherName())
                .build();
    }

    private Journal buildJournalForPublicationContext() throws InvalidIssnException {
        return new Journal.Builder()
                .withOnlineIssn(extractIssnOnline())
                .withPrintIssn(extractIssn())
                .withTitle(extractPublisherTitle())
                .build();
    }

    private PublicationContext buildPublicationContextWhenMainCategoryIsReport()
            throws InvalidIsbnException, InvalidIssnException {
        List<String> isbnList = extractIsbn().stream().collect(Collectors.toList());
        if (isDegreePhd(cristinObject) || isDegreeMaster(cristinObject)) {
            return new Degree.Builder()
                    .withPublisher(extractPublisherName())
                    .withIsbnList(isbnList)
                    .build();
        }
        return new Report.Builder()
                .withPublisher(extractPublisherName())
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

    public CristinBookOrReportMetadata extractCristinBookReport() {
        return Optional.ofNullable(cristinObject)
            .map(CristinObject::getBookOrReportMetadata)
            .orElse(null);
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
        if (!isBook(cristinObject) && !isReport(cristinObject)) {
            return null;
        }
        if (extractSubjectField() == null) {
            if (cristinObject.getSecondaryCategory().equals(CristinSecondaryCategory.MONOGRAPH)) {
                throw new MissingFieldsException(CristinBookOrReportMetadata.SUBJECT_FIELD_IS_A_REQUIRED_FIELD);
            }
            return null;
        }
        Integer code = extractSubjectField().getSubjectFieldCode();
        if (code == null) {
            throw new MissingFieldsException(CristinSubjectField.MISSING_SUBJECT_FIELD_CODE);
        }
        return String.valueOf(code);
    }

    private CristinSubjectField extractSubjectField() {
        return extractCristinBookReport().getSubjectField();
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
        if (isJournal(cristinObject) && extractCristinJournalPublication().getDoi() != null) {
            return URI.create(extractCristinJournalPublication().getDoi());
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
