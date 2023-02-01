package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.AFFILIATION_DELIMITER;
import static no.sikt.nva.scopus.ScopusConstants.ORCID_DOMAIN_URL;
import static no.sikt.nva.scopus.ScopusConverter.extractContentString;
import static no.unit.nva.language.LanguageConstants.ENGLISH;
import static nva.commons.core.StringUtils.isNotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.PersonalnameType;
import no.unit.nva.language.LanguageMapper;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Role;
import org.apache.tika.langdetect.OptimaizeLangDetector;

public class ContributorExtractor {

    public static final String NAME_DELIMITER = ", ";
    public static final String FIRST_NAME_CRISTIN_FIELD_NAME = "FirstName";
    public static final String LAST_NAME_CRISTIN_FIELD_NAME = "LastName";
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
            .forEach(authorOrCollaboration -> extractContributorFromAuthorOrCollaboration(authorOrCollaboration,
                                                                                          authorGroupTp));
    }

    private void extractContributorFromAuthorOrCollaboration(Object authorOrCollaboration,
                                                             AuthorGroupTp authorGroupTp) {
        Optional<Contributor> matchingContributor = contributors.stream()
                                                        .filter(
                                                            contributor -> compareContributorToAuthorOrCollaboration(
                                                                contributor,
                                                                authorOrCollaboration))
                                                        .findAny();
        if (matchingContributor.isPresent()) {
            replaceExistingContributor(matchingContributor.get(), authorGroupTp);
        } else {
            Optional<PersonalnameType> correspondencePerson = correspondenceTps
                                                                  .stream()
                                                                  .map(this::extractPersonalNameType)
                                                                  .findFirst().orElse(Optional.empty());
            generateContributorFromAuthorOrCollaboration(authorOrCollaboration, authorGroupTp,
                                                         correspondencePerson.orElse(null));
        }
    }

    private void replaceExistingContributor(Contributor existingContributor, AuthorGroupTp authorGroupTp) {
        if (Objects.isNull(existingContributor.getIdentity().getId())) {
            var optionalNewAffiliations = generateAffiliation(authorGroupTp);
            optionalNewAffiliations.ifPresent(
                organizations ->
                    createNewContributorWithAdditionalAffiliationsAndSwapItInContributorList(organizations,
                                                                                             existingContributor));
        }
    }

    private void createNewContributorWithAdditionalAffiliationsAndSwapItInContributorList(
        Organization newAffiliation, Contributor matchingContributor) {
        var newContributor =
            cloneContributorAddingAffiliation(matchingContributor, newAffiliation);
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
        return new Contributor(existingContributor.getIdentity(),
                               affiliations,
                               existingContributor.getRole(),
                               existingContributor.getSequence(),
                               existingContributor.isCorrespondingAuthor());
    }

    private boolean compareContributorToAuthorOrCollaboration(Contributor contributor, Object authorOrCollaboration) {
        return authorOrCollaboration instanceof AuthorTp
                   ? isSamePerson((AuthorTp) authorOrCollaboration, contributor)
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

    private void generateContributorFromAuthorOrCollaboration(Object authorOrCollaboration,
                                                              AuthorGroupTp authorGroupTp,
                                                              PersonalnameType correspondencePerson) {
        if (authorOrCollaboration instanceof AuthorTp) {
            generateContributor((AuthorTp) authorOrCollaboration, authorGroupTp,
                                correspondencePerson);
        } else {
            generateContributorFromCollaborationTp(
                (CollaborationTp) authorOrCollaboration, authorGroupTp, correspondencePerson);
        }
    }

    private void generateContributor(AuthorTp author, AuthorGroupTp authorGroup,
                                     PersonalnameType correspondencePerson) {
        var cristinUri = piaConnection.getCristinID(author.getAuid());
        var cristinPerson = cristinConnection.getCristinPersonByCristinId(cristinUri);
        contributors.add(
            cristinPerson
                .map(person -> CristinContributorExtractor.generateContributorFromCristin(person, author,
                                                                                          correspondencePerson))
                .orElseGet(() -> generateContributorFromAuthorTp(author, authorGroup, correspondencePerson)));
    }

    private Contributor generateContributorFromAuthorTp(AuthorTp author, AuthorGroupTp authorGroup,
                                                        PersonalnameType correspondencePerson) {
        var identity = generateContributorIdentityFromAuthorTp(author);
        var affiliation = generateAffiliation(authorGroup);
        return new Contributor(identity,
                               affiliation.map(List::of).orElse(List.of()),
                               Role.CREATOR,
                               getSequenceNumber(author),
                               isCorrespondingAuthor(author, correspondencePerson));
    }

    private void generateContributorFromCollaborationTp(CollaborationTp collaboration,
                                                        AuthorGroupTp authorGroupTp,
                                                        PersonalnameType correspondencePerson) {
        var identity = new Identity();
        identity.setName(determineContributorName(collaboration));
        var affiliation = generateAffiliation(authorGroupTp);
        var newContributor = new Contributor(identity, affiliation.map(List::of).orElse(List.of()), null,
                                             getSequenceNumber(collaboration),
                                             isCorrespondingAuthor(collaboration, correspondencePerson));
        contributors.add(newContributor);
    }

    private Identity generateContributorIdentityFromAuthorTp(AuthorTp authorTp) {
        var identity = new Identity();
        identity.setName(determineContributorName(authorTp));
        identity.setOrcId(getOrcidAsUriString(authorTp));
        return identity;
    }

    private Optional<Organization> generateAffiliation(AuthorGroupTp authorGroup) {
        var labels = getOrganizationLabels(authorGroup);
        return labels.map(this::generateOrganizationWithLabel);
    }

    private boolean isCorrespondingAuthor(CollaborationTp collaboration, PersonalnameType correspondencePerson) {
        return nonNull(correspondencePerson)
               && collaboration.getIndexedName().equals(correspondencePerson.getIndexedName());
    }

    private boolean isCorrespondingAuthor(AuthorTp author, PersonalnameType correspondencePerson) {
        return nonNull(correspondencePerson)
               && author.getIndexedName().equals(correspondencePerson.getIndexedName());
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
        var organizationNameOptional = getOrganizationNameFromAuthorGroup(authorGroup);
        return organizationNameOptional.map(organizationName -> Map.of(getLanguageIso6391Code(organizationName),
                                                                       organizationName));
    }

    private String craftOrcidUriString(String potentiallyMalformedOrcidString) {
        return potentiallyMalformedOrcidString.contains(ORCID_DOMAIN_URL)
                   ? potentiallyMalformedOrcidString
                   : ORCID_DOMAIN_URL + potentiallyMalformedOrcidString;
    }

    private Optional<String> getOrganizationNameFromAuthorGroup(AuthorGroupTp authorGroup) {
        var affiliation = authorGroup.getAffiliation();
        return nonNull(affiliation)
                   ? Optional.of(affiliation
                                     .getOrganization()
                                     .stream()
                                     .map(organizationTp -> extractContentString(
                                         organizationTp.getContent()))
                                     .collect(Collectors.joining(AFFILIATION_DELIMITER)))
                   : Optional.empty();
    }

    private String getLanguageIso6391Code(String textToBeGuessedLanguageCodeFrom) {
        var detector = new OptimaizeLangDetector().loadModels();
        var result = detector.detect(textToBeGuessedLanguageCodeFrom);
        return result.isReasonablyCertain()
                   ? getIso6391LanguageCodeForSupportedNvaLanguage(result.getLanguage())
                   : ENGLISH.getIso6391Code();
    }

    private String getIso6391LanguageCodeForSupportedNvaLanguage(String possiblyUnsupportedLanguageIso6391code) {
        var language = LanguageMapper.getLanguageByIso6391Code(possiblyUnsupportedLanguageIso6391code);
        return nonNull(language.getIso6391Code())
                   ? language.getIso6391Code()
                   : ENGLISH.getIso6391Code();
    }
}
