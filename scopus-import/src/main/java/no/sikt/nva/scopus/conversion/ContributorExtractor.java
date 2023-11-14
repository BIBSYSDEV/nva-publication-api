package no.sikt.nva.scopus.conversion;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.AFFILIATION_DELIMITER;
import static no.sikt.nva.scopus.ScopusConstants.ORCID_DOMAIN_URL;
import static no.sikt.nva.scopus.ScopusConverter.extractContentString;
import static no.sikt.nva.scopus.conversion.CristinContributorExtractor.generateContributorFromCristinPerson;
import static no.unit.nva.language.LanguageConstants.ENGLISH;
import static nva.commons.core.StringUtils.isNotBlank;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.scopus.generated.AffiliationTp;
import no.scopus.generated.AffiliationType;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.OrganizationTp;
import no.scopus.generated.PersonalnameType;
import no.sikt.nva.scopus.conversion.model.cristin.CristinOrganization;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.unit.nva.language.LanguageMapper;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("PMD.GodClass")
public class ContributorExtractor {

    public static final String NAME_DELIMITER = ", ";
    public static final String FIRST_NAME_CRISTIN_FIELD_NAME = "FirstName";
    public static final String LAST_NAME_CRISTIN_FIELD_NAME = "LastName";
    public static final String NORWAY = "norway";
    public static final String NO_LAND_CODE = "NO";
    public static final String OTHER_INSTITUTIONS = "Andre institusjoner";
    private final List<CorrespondenceTp> correspondenceTps;
    private final List<AuthorGroupTp> authorGroupTps;
    private final List<Contributor> contributors;
    private final PiaConnection piaConnection;
    private final CristinConnection cristinConnection;

    public ContributorExtractor(List<CorrespondenceTp> correspondenceTps, List<AuthorGroupTp> authorGroupTps,
                                PiaConnection piaConnection, CristinConnection cristinConnection) {
        this.correspondenceTps = correspondenceTps;
        this.authorGroupTps = authorGroupTps;
        this.contributors = new ArrayList<>();
        this.piaConnection = piaConnection;
        this.cristinConnection = cristinConnection;
    }

    public List<Contributor> generateContributors() {
        authorGroupTps.forEach(this::generateContributorsFromAuthorGroup);
        return contributors;
    }

    private Optional<PersonalnameType> extractPersonalNameType(CorrespondenceTp correspondenceTp) {
        return Optional.ofNullable(correspondenceTp.getPerson());
    }

    private void generateContributorsFromAuthorGroup(AuthorGroupTp authorGroupTp) {
        authorGroupTp.getAuthorOrCollaboration()
            .forEach(authorOrCollaboration -> extractContributorFromAuthorOrCollaboration(authorOrCollaboration, authorGroupTp));
    }

    private void extractContributorFromAuthorOrCollaboration(Object authorOrCollaboration,
                                                             AuthorGroupTp authorGroupTp) {
        var existingContributor = getExistingContributor(authorOrCollaboration);
        if (existingContributor.isPresent()) {
            replaceExistingContributor(existingContributor.get(), authorGroupTp);
        } else {
            generateContributorFromAuthorOrCollaboration(authorOrCollaboration, authorGroupTp);
        }
    }

    @Nullable
    private PersonalnameType getCorrespondencePerson() {
        return correspondenceTps.stream()
                   .map(this::extractPersonalNameType)
                   .findFirst()
                   .orElse(Optional.empty()).orElse(null);
    }

    @NotNull
    private Optional<Contributor> getExistingContributor(Object authorOrCollaboration) {
        return contributors.stream()
                   .filter(contributor -> compareContributorToAuthorOrCollaboration(contributor, authorOrCollaboration))
                   .findAny();
    }

    private void replaceExistingContributor(Contributor existingContributor, AuthorGroupTp authorGroupTp) {
        if (isNull(existingContributor.getIdentity().getId())) {
            var optionalNewAffiliations = generateAffiliationFromAuthorGroupTp(authorGroupTp);
            optionalNewAffiliations.ifPresent(
                organizations -> updateContributorWithAdditionalAffiliationsInContributorList(organizations,
                                                                                              existingContributor));
        }
    }

    private void updateContributorWithAdditionalAffiliationsInContributorList(Organization newAffiliation,
                                                                              Contributor matchingContributor) {
        var newContributor = cloneContributorAddingAffiliation(matchingContributor, newAffiliation);
        replaceContributor(matchingContributor, newContributor);
    }

    private void replaceContributor(Contributor oldContributor, Contributor newContributor) {
        contributors.remove(oldContributor);
        contributors.add(newContributor);
    }

    private Contributor cloneContributorAddingAffiliation(Contributor existingContributor,
                                                          Organization newAffiliation) {
        List<Organization> affiliations = new ArrayList<>(existingContributor.getAffiliations());
        affiliations.add(newAffiliation);

        return new Contributor.Builder().withIdentity(existingContributor.getIdentity())
                   .withAffiliations(affiliations)
                   .withRole(existingContributor.getRole())
                   .withSequence(existingContributor.getSequence())
                   .withCorrespondingAuthor(existingContributor.isCorrespondingAuthor())
                   .build();
    }

    private boolean compareContributorToAuthorOrCollaboration(Contributor contributor, Object authorOrCollaboration) {
        return authorOrCollaboration instanceof AuthorTp authorTp
                   ? isSamePerson(authorTp, contributor)
                   : isSameSequenceElement((CollaborationTp) authorOrCollaboration, contributor);
    }

    private boolean isSameSequenceElement(CollaborationTp collaboration, Contributor contributor) {
        return collaboration.getSeq().equals(contributor.getSequence().toString());
    }

    private boolean isSamePerson(AuthorTp author, Contributor contributor) {
        if (author.getSeq().equals(contributor.getSequence().toString())) {
            return true;
        } else if (nonNull(author.getOrcid()) && nonNull(contributor.getIdentity().getOrcId())) {
            return craftOrcidUriString(author.getOrcid()).equals(contributor.getIdentity().getOrcId());
        } else {
            return false;
        }
    }

    private void generateContributorFromAuthorOrCollaboration(Object authorOrCollaboration, AuthorGroupTp authorGroupTp) {
        if (authorOrCollaboration instanceof AuthorTp authorTp) {
            generateContributorFromAuthorTp(authorTp, authorGroupTp);
        } else {
            generateContributorFromCollaborationTp((CollaborationTp) authorOrCollaboration, authorGroupTp,
                                                   getCorrespondencePerson());
        }
    }

    private void generateContributorFromAuthorTp(AuthorTp author,
                                                 AuthorGroupTp authorGroup) {

        var cristinOrganization = fetchCristinOrganization(authorGroup);

        var contributor = fetchCristinPerson(author)
                              .map(person -> generateContributorFromCristinPerson(person, author, getCorrespondencePerson(), cristinOrganization))
                              .orElseGet(() -> generateContributorFromAuthorTp(authorGroup, author, getCorrespondencePerson(), cristinOrganization));

        contributors.add(contributor);
    }

    @NotNull
    private Optional<CristinPerson> fetchCristinPerson(AuthorTp author) {
        return piaConnection.getCristinPersonIdentifier(author.getAuid())
                   .flatMap(cristinConnection::getCristinPersonByCristinId);
    }

    private CristinOrganization fetchCristinOrganization(AuthorGroupTp authorGroup) {
        return getCristinOrganizationUri(authorGroup)
                   .map(cristinConnection::getCristinOrganizationByCristinId)
                   .filter(ContributorExtractor::isNotOtherNorwegianInstitution)
                   .orElse(null);
    }

    private static boolean isNotOtherNorwegianInstitution(CristinOrganization org) {
        return !(NO_LAND_CODE.equals(org.getCountry()) && org.getLabels().containsValue(OTHER_INSTITUTIONS));
    }

    private Optional<URI> getCristinOrganizationUri(AuthorGroupTp authorGroup) {
        return Optional.ofNullable(authorGroup)
                   .map(AuthorGroupTp::getAffiliation)
                   .map(AffiliationTp::getAfid)
                   .flatMap(piaConnection::getCristinOrganizationIdentifier);
    }

    private Contributor generateContributorFromAuthorTp(
        AuthorGroupTp authorGroup,
        AuthorTp author,
        PersonalnameType correspondencePerson,
        CristinOrganization cristinOrganization) {
        return new Contributor.Builder()
                   .withIdentity(generateContributorIdentityFromAuthorTp(author))
                   .withAffiliations(nonNull(cristinOrganization)
                                         ? AffiliationGenerator.fromCristinOrganization(cristinOrganization)
                                         : AffiliationGenerator.fromAuthorGroupTp(authorGroup) )
                   .withRole(new RoleType(Role.CREATOR))
                   .withSequence(getSequenceNumber(author))
                   .withCorrespondingAuthor(isCorrespondingAuthor(author, correspondencePerson))
                   .build();
    }

    private Optional<Organization> generateAffiliationFromCristinOrganization(
        CristinOrganization cristinOrganization) {
        return Optional.of(
            new Organization.Builder().withId(cristinOrganization.getId()).withLabels(cristinOrganization.getLabels()).build());
    }

    private void generateContributorFromCollaborationTp(CollaborationTp collaboration, AuthorGroupTp authorGroupTp,
                                                        PersonalnameType correspondencePerson) {
        var cristinOrganization = fetchCristinOrganization(authorGroupTp);

        var newContributor = new Contributor.Builder()
                                 .withIdentity(new Identity.Builder().withName(determineContributorName(collaboration)).build())
                                 .withAffiliations(generateAffiliation(cristinOrganization, authorGroupTp)
                                                       .map(List::of)
                                                       .orElse(Collections.emptyList()))
                                 .withRole(new RoleType(Role.OTHER))
                                 .withSequence(getSequenceNumber(collaboration))
                                 .withCorrespondingAuthor(isCorrespondingAuthor(collaboration, correspondencePerson))
                                 .build();
        contributors.add(newContributor);
    }

    private Optional<Organization> generateAffiliation(
        CristinOrganization cristinOrganization, AuthorGroupTp authorGroupTp) {
        return nonNull(cristinOrganization)
                   ? generateAffiliationFromCristinOrganization(cristinOrganization)
                   : generateAffiliationFromAuthorGroupTp(authorGroupTp);
    }

    private static boolean isNotNorway(Map<String, String> labels) {
        return labels.values()
                        .stream().map(String::toLowerCase)
                        .noneMatch(NORWAY::equals);
    }

    private Identity generateContributorIdentityFromAuthorTp(AuthorTp authorTp) {
        var identity = new Identity();
        identity.setName(determineContributorName(authorTp));
        identity.setOrcId(getOrcidAsUriString(authorTp));
        return identity;
    }

    private Optional<Organization> generateAffiliationFromAuthorGroupTp(AuthorGroupTp authorGroup) {
        return getOrganizationLabels(authorGroup)
                   .filter(ContributorExtractor::isNotNorway)
                   .map(this::generateOrganizationWithLabel);
    }

    private boolean isCorrespondingAuthor(CollaborationTp collaboration, PersonalnameType correspondencePerson) {
        return nonNull(correspondencePerson) && collaboration.getIndexedName()
                                                    .equals(correspondencePerson.getIndexedName());
    }

    private boolean isCorrespondingAuthor(AuthorTp author, PersonalnameType correspondencePerson) {
        return nonNull(correspondencePerson) && author.getIndexedName().equals(correspondencePerson.getIndexedName());
    }

    private int getSequenceNumber(CollaborationTp collaborationTp) {
        return Integer.parseInt(collaborationTp.getSeq());
    }

    private int getSequenceNumber(AuthorTp authorTp) {
        return Integer.parseInt(authorTp.getSeq());
    }

    private String determineContributorName(AuthorTp author) {
        return author.getPreferredName().getSurname() + NAME_DELIMITER + author.getPreferredName().getGivenName();
    }

    private String determineContributorName(CollaborationTp collaborationTp) {
        return collaborationTp.getIndexedName();
    }

    private String getOrcidAsUriString(AuthorTp authorTp) {
        return isNotBlank(authorTp.getOrcid()) ? craftOrcidUriString(authorTp.getOrcid()) : null;
    }

    private Organization generateOrganizationWithLabel(Map<String, String> label) {
        Organization organization = new Organization();
        organization.setLabels(label);
        return organization;
    }

    private Optional<Map<String, String>> getOrganizationLabels(AuthorGroupTp authorGroup) {
        var organizationName = getOrganizationNameFromAuthorGroup(authorGroup);
        if (isEmpty(organizationName)) {
            return organizationName.map(name -> Map.of(getLanguageIso6391Code(name), name));
        } else {
            return Optional.ofNullable(authorGroup.getAffiliation()).map(AffiliationTp::getCountry)
                       .map(country -> Map.of(getLanguageIso6391Code(country), country));
        }
    }

    private static boolean isEmpty(Optional<String> organizationNameOptional) {
        return organizationNameOptional.isPresent() && !organizationNameOptional.get().isEmpty();
    }

    private String craftOrcidUriString(String potentiallyMalformedOrcidString) {
        return potentiallyMalformedOrcidString.contains(ORCID_DOMAIN_URL) ? potentiallyMalformedOrcidString
                   : ORCID_DOMAIN_URL + potentiallyMalformedOrcidString;
    }

    private Optional<String> getOrganizationNameFromAuthorGroup(AuthorGroupTp authorGroup) {
        return Optional.ofNullable(authorGroup.getAffiliation())
            .map(AffiliationType::getOrganization)
            .map(ContributorExtractor::toOrganizationName);
    }

    private static String toOrganizationName(List<OrganizationTp> organizationTps) {
        return organizationTps.stream()
                   .map(organizationTp -> extractContentString(organizationTp.getContent()))
                   .collect(Collectors.joining(AFFILIATION_DELIMITER));
    }

    private String getLanguageIso6391Code(String textToBeGuessedLanguageCodeFrom) {
        var detector = new OptimaizeLangDetector().loadModels();
        var result = detector.detect(textToBeGuessedLanguageCodeFrom);
        return result.isReasonablyCertain() ? getIso6391LanguageCodeForSupportedNvaLanguage(result.getLanguage())
                   : ENGLISH.getIso6391Code();
    }

    private String getIso6391LanguageCodeForSupportedNvaLanguage(String possiblyUnsupportedLanguageIso6391code) {
        var language = LanguageMapper.getLanguageByIso6391Code(possiblyUnsupportedLanguageIso6391code);
        return nonNull(language.getIso6391Code()) ? language.getIso6391Code() : ENGLISH.getIso6391Code();
    }
}
