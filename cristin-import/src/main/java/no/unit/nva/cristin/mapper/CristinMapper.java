package no.unit.nva.cristin.mapper;

import static java.util.Objects.isNull;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_OWNER_AFFILIATION;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_SAMPLE_DOI;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.UNIT_CUSTOMER_ID;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.HRCS_ACTIVITIES_MAP;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.HRCS_CATEGORIES_MAP;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.PATH_CUSTOMER;
import static no.unit.nva.cristin.mapper.CristinHrcsCategoriesAndActivities.HRCS_ACTIVITY_URI;
import static no.unit.nva.cristin.mapper.CristinHrcsCategoriesAndActivities.HRCS_CATEGORY_URI;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isBook;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isReport;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.cristin.mapper.nva.CristinMappingModule;
import no.unit.nva.cristin.mapper.nva.ReferenceBuilder;
import no.unit.nva.cristin.mapper.nva.exceptions.MissingContributorsException;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Publication.Builder;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import nva.commons.core.attempt.Try;
import nva.commons.core.language.LanguageMapper;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class CristinMapper extends CristinMappingModule {
    
    public static final String EMPTY_STRING = "";
    
    public CristinMapper(CristinObject cristinObject) {
        super(cristinObject);
    }
    
    public Publication generatePublication() {
        Publication publication = new Builder()
                                      .withAdditionalIdentifiers(Set.of(extractIdentifier()))
                                      .withEntityDescription(generateEntityDescription())
                                      .withCreatedDate(extractEntryCreationDate())
                                      .withModifiedDate(extractEntryLastModifiedDate())
                                      .withPublishedDate(extractEntryCreationDate())
                                      .withPublisher(extractOrganization())
                                      .withResourceOwner(new ResourceOwner(cristinObject.getPublicationOwner(),
                                          HARDCODED_OWNER_AFFILIATION))
                                      .withStatus(PublicationStatus.PUBLISHED)
                                      .withLink(HARDCODED_SAMPLE_DOI)
                                      .withProjects(extractProjects())
                                      .withSubjects(generateNvaHrcsCategoriesAndActivities())
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
        UriWrapper customerId = UriWrapper.fromUri(NVA_API_DOMAIN).addChild(PATH_CUSTOMER, UNIT_CUSTOMER_ID);
        return new Organization.Builder().withId(customerId.getUri()).build();
    }
    
    private Instant extractEntryCreationDate() {
        return Optional.ofNullable(cristinObject.getEntryCreationDate())
                   .map(ld -> ld.atStartOfDay().toInstant(zoneOffset()))
                   .orElse(null);
    }
    
    private Instant extractEntryLastModifiedDate() {
        return Optional.ofNullable(cristinObject.getEntryLastModifiedDate())
                   .map(ld -> ld.atStartOfDay().toInstant(zoneOffset()))
                   .orElseGet(this::extractEntryCreationDate);
    }
    
    private ZoneOffset zoneOffset() {
        return ZoneOffset.UTC.getRules().getOffset(Instant.now());
    }
    
    private EntityDescription generateEntityDescription() {
        return new EntityDescription.Builder()
                   .withLanguage(extractLanguage())
                   .withMainTitle(extractMainTitle())
                   .withDate(extractPublicationDate())
                   .withReference(new ReferenceBuilder(cristinObject).buildReference())
                   .withContributors(extractContributors())
                   .withNpiSubjectHeading(extractNpiSubjectHeading())
                   .withAbstract(extractAbstract())
                   .withTags(extractTags())
                   .build();
    }
    
    private List<Contributor> extractContributors() {
        if (isNull(cristinObject.getContributors())) {
            throw new MissingContributorsException();
        }
        return cristinObject.getContributors()
                   .stream()
                   .map(attempt(CristinContributor::toNvaContributor))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }
    
    private List<URI> generateNvaHrcsCategoriesAndActivities() {
        if (isNull(extractCristinHrcsCategoriesAndActivities())) {
            return null;
        }
        List<URI> listOfCategoriesAndActivities = new ArrayList<>();
        listOfCategoriesAndActivities.addAll(extractHrcsCategories());
        listOfCategoriesAndActivities.addAll(extractHrcsActivities());
        return listOfCategoriesAndActivities;
    }
    
    private List<CristinHrcsCategoriesAndActivities> extractCristinHrcsCategoriesAndActivities() {
        return cristinObject.getHrcsCategoriesAndActivities();
    }
    
    private List<URI> extractHrcsCategories() {
        return extractCristinHrcsCategoriesAndActivities()
                   .stream()
                   .map(CristinHrcsCategoriesAndActivities::getCategory)
                   .filter(CristinHrcsCategoriesAndActivities::validateCategory)
                   .map(categoryId -> HRCS_CATEGORY_URI + HRCS_CATEGORIES_MAP.get(categoryId).toLowerCase(Locale.ROOT))
                   .map(URI::create)
                   .collect(Collectors.toList());
    }
    
    private Collection<URI> extractHrcsActivities() {
        return extractCristinHrcsCategoriesAndActivities()
                   .stream()
                   .map(CristinHrcsCategoriesAndActivities::getActivity)
                   .filter(CristinHrcsCategoriesAndActivities::validateActivity)
                   .map(activityId -> HRCS_ACTIVITY_URI + HRCS_ACTIVITIES_MAP.get(activityId).toLowerCase(Locale.ROOT))
                   .map(URI::create)
                   .collect(Collectors.toList());
    }
    
    private PublicationDate extractPublicationDate() {
        return new PublicationDate.Builder().withYear(cristinObject.getPublicationYear().toString()).build();
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
    
    private boolean resourceTypeIsNotExpectedToHaveAnNpiSubjectHeading() {
        return !(isBook(cristinObject) || isReport(cristinObject));
    }
    
    private CristinSubjectField extractSubjectField() {
        if (resourceTypeIsNotExpectedToHaveAnNpiSubjectHeading()) {
            return null;
        }
        return extractCristinBookReport().getSubjectField();
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
