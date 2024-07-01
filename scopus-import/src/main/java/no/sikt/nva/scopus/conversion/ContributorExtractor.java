package no.sikt.nva.scopus.conversion;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.ORCID_DOMAIN_URL;
import static no.sikt.nva.scopus.conversion.CristinContributorExtractor.generateContributorFromCristinPerson;
import static nva.commons.core.StringUtils.isNotBlank;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.PersonalnameType;
import no.sikt.nva.scopus.conversion.model.AuthorIdentifiers;
import no.sikt.nva.scopus.conversion.model.CorporationWithContributors;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.sikt.nva.scopus.exception.MissingNvaContributorException;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.StringUtils;

@SuppressWarnings("PMD.GodClass")
public class ContributorExtractor {

    public static final String FIRST_NAME_CRISTIN_FIELD_NAME = "FirstName";
    public static final String LAST_NAME_CRISTIN_FIELD_NAME = "LastName";
    public static final String MISSING_CONTRIBUTORS_OF_NVA_CUSTOMERS_MESSAGE = "None of contributors belongs to NVA "
                                                                               + "customer, all contributors "
                                                                               + "affiliations: ";
    public static final String SCOPUS_AUID = "scopus-auid";
    private final List<CorrespondenceTp> correspondenceTps;
    private final List<AuthorGroupTp> authorGroupTps;
    private final List<Contributor> contributors;
    private final NvaCustomerConnection nvaCustomerConnection;
    private final AffiliationGenerator affiliationGenerator;
    private final CristinPersonRetriever cristinPersonRetriever;

    public ContributorExtractor(List<CorrespondenceTp> correspondenceTps, List<AuthorGroupTp> authorGroupTps,
                                PiaConnection piaConnection, CristinConnection cristinConnection,
                                NvaCustomerConnection nvaCustomerConnection) {
        this.correspondenceTps = correspondenceTps;
        this.authorGroupTps = authorGroupTps;
        this.contributors = new ArrayList<>();
        this.nvaCustomerConnection = nvaCustomerConnection;
        this.affiliationGenerator = new AffiliationGenerator(piaConnection, cristinConnection);
        this.cristinPersonRetriever = new CristinPersonRetriever(cristinConnection, piaConnection);
    }

    public List<Contributor> generateContributors() {
        var cristinAffiliationsAuthorgroupsTps = affiliationGenerator.getCorporations(authorGroupTps);
        var cristinPersons = cristinPersonRetriever.retrieveCristinPersons(authorGroupTps);
        var contributorList = cristinAffiliationsAuthorgroupsTps.stream()
                               .map(cristinAffiliationsAuthorgroup -> generateContributorsFromAuthorGroup(
                                   cristinAffiliationsAuthorgroup, cristinPersons))
                               .flatMap(List::stream)
                               .toList();
        if (noContributorsBelongingToNvaCustomer(cristinAffiliationsAuthorgroupsTps)) {
            var affiliationsIds = getAllAffiliationIds(contributorList);
            throw new MissingNvaContributorException(MISSING_CONTRIBUTORS_OF_NVA_CUSTOMERS_MESSAGE + affiliationsIds);
        } else {
            return getContributors();
        }
    }

    protected static List<AdditionalIdentifier> extractAdditionalIdentifiers(AuthorTp authorTp) {
        return isNotBlank(authorTp.getAuid())
                   ? List.of(createAuidAdditionalIdentifier(authorTp))
                   : emptyList();
    }

    private static List<URI> getAllAffiliationIds(List<Contributor> contributors) {
        return contributors.stream()
                   .map(Contributor::getAffiliations)
                   .map(ContributorExtractor::toAffiliationIdList)
                   .flatMap(List::stream)
                   .toList();
    }

    private static List<URI> toAffiliationIdList(List<Corporation> organizations) {
        return Optional.of(organizations.stream()
                               .filter(Organization.class::isInstance)
                               .map(Organization.class::cast)
                               .map(Organization::getId).toList())
                   .orElse(List.of());
    }

    private static AdditionalIdentifier createAuidAdditionalIdentifier(AuthorTp authorTp) {
        return new AdditionalIdentifier(SCOPUS_AUID, authorTp.getAuid());
    }

    private List<Contributor> getContributors() {
        return contributors.stream().map(Contributor.class::cast).toList();
    }

    private boolean noContributorsBelongingToNvaCustomer(
        List<CorporationWithContributors> corporationWithContributors) {
        var cristinOrganizations =
            corporationWithContributors.stream()
                .map(CorporationWithContributors::getCristinOrganizations)
                .flatMap(Collection::stream).collect(Collectors.toSet());
        return !nvaCustomerConnection.atLeastOneNvaCustomerPresent(cristinOrganizations);
    }

    private Optional<PersonalnameType> extractPersonalNameType(CorrespondenceTp correspondenceTp) {
        return Optional.ofNullable(correspondenceTp.getPerson());
    }

    private List<Contributor> generateContributorsFromAuthorGroup(
        CorporationWithContributors corporationWithContributors,
        Map<AuthorIdentifiers, CristinPerson> cristinPersons) {
        corporationWithContributors.getScopusAuthors().getAuthorOrCollaboration()
            .forEach(authorOrCollaboration -> extractContributorFromAuthorOrCollaboration(authorOrCollaboration,
                                                                                          corporationWithContributors,
                                                                                          cristinPersons));
        return contributors;
    }

    private void extractContributorFromAuthorOrCollaboration(Object authorOrCollaboration,
                                                             CorporationWithContributors corporationWithContributors,
                                                             Map<AuthorIdentifiers, CristinPerson> cristinPersons) {
        var existingContributor = getExistingContributor(authorOrCollaboration);
        if (existingContributor.isPresent()) {
            replaceExistingContributor(existingContributor.get(), corporationWithContributors);
        } else {
            generateContributorFromAuthorOrCollaboration(authorOrCollaboration, corporationWithContributors,
                                                         cristinPersons);
        }
    }

    private PersonalnameType getCorrespondencePerson() {
        return correspondenceTps.stream()
                   .map(this::extractPersonalNameType)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .findFirst()
                   .orElse(null);
    }

    private Optional<Contributor> getExistingContributor(Object authorOrCollaboration) {
        return contributors.stream()
                   .filter(contributor -> compareContributorToAuthorOrCollaboration(contributor, authorOrCollaboration))
                   .findAny();
    }

    private void replaceExistingContributor(Contributor existingContributor,
                                            CorporationWithContributors corporationWithContributors) {
        if (isNull(existingContributor.getIdentity().getId())) {
            var newAffiliations = corporationWithContributors.toCorporations();
            if (!newAffiliations.isEmpty()) {
                updateContributorWithAdditionalAffiliationsInContributorList(newAffiliations,
                                                                             existingContributor);
            }
        }
    }

    private void updateContributorWithAdditionalAffiliationsInContributorList(
        List<Corporation> newAffiliations, Contributor matchingContributor) {
        var newContributor = cloneContributorAddingAffiliations(matchingContributor, newAffiliations);
        replaceContributor(matchingContributor, newContributor);
    }

    private void replaceContributor(Contributor oldContributor, Contributor newContributor) {
        contributors.remove(oldContributor);
        contributors.add(newContributor);
    }

    private Contributor cloneContributorAddingAffiliations(Contributor existingContributor,
                                                           List<Corporation> newAffiliations) {
        var affiliations = new ArrayList<>(existingContributor.getAffiliations());
        affiliations.addAll(newAffiliations);

        return new Contributor.Builder().withIdentity(existingContributor.getIdentity())
                   .withAffiliations(affiliations)
                   .withRole(existingContributor.getRole())
                   .withSequence(existingContributor.getSequence())
                   .withCorrespondingAuthor(existingContributor.isCorrespondingAuthor())
                   .build();
    }

    private boolean compareContributorToAuthorOrCollaboration(Contributor contributor,
                                                              Object authorOrCollaboration) {
        return authorOrCollaboration instanceof AuthorTp authorTp ? isSamePerson(authorTp, contributor)
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
                                                              CorporationWithContributors corporationWithContributors,
                                                              Map<AuthorIdentifiers, CristinPerson> cristinPersons) {
        if (authorOrCollaboration instanceof AuthorTp authorTp) {
            generateContributorFromAuthorTp(authorTp, corporationWithContributors,
                                            cristinPersons);
        } else {
            generateContributorFromCollaborationTp((CollaborationTp) authorOrCollaboration,
                                                   corporationWithContributors,
                                                   getCorrespondencePerson());
        }
    }

    private void generateContributorFromAuthorTp(AuthorTp author,
                                                 CorporationWithContributors corporationWithContributors,
                                                 Map<AuthorIdentifiers, CristinPerson> cristinPersons) {

        var cristinOrganizations = corporationWithContributors.getCristinOrganizations();
        var authorIdentifiers = new AuthorIdentifiers(author.getAuid(), author.getOrcid());
        var contributor =
            Optional.ofNullable(cristinPersons.get(authorIdentifiers))
                .map(cristinPerson -> generateContributorFromCristinPerson(cristinPerson, author,
                                                                           getCorrespondencePerson(),
                                                                           cristinOrganizations))
                .orElseGet(() -> generateContributorFromAuthorTp(corporationWithContributors, author,
                                                                 getCorrespondencePerson()));

        contributors.add(contributor);
    }

    private Contributor generateContributorFromAuthorTp(
        CorporationWithContributors corporationWithContributors,
        AuthorTp author,
        PersonalnameType correspondencePerson) {
        return new Contributor.Builder().withIdentity(generateContributorIdentityFromAuthorTp(author))
                   .withAffiliations(corporationWithContributors.toCorporations())
                   .withRole(new RoleType(Role.CREATOR))
                   .withSequence(getSequenceNumber(author))
                   .withCorrespondingAuthor(isCorrespondingAuthor(author, correspondencePerson))
                   .build();
    }

    private void generateContributorFromCollaborationTp(CollaborationTp collaboration,
                                                        CorporationWithContributors corporationWithContributors,
                                                        PersonalnameType correspondencePerson) {

        var newContributor = new Contributor.Builder().withIdentity(generateIdentity(collaboration))
                                 .withAffiliations(corporationWithContributors.toCorporations())
                                 .withRole(new RoleType(Role.OTHER))
                                 .withSequence(getSequenceNumber(collaboration))
                                 .withCorrespondingAuthor(isCorrespondingAuthor(collaboration, correspondencePerson))
                                 .build();
        contributors.add(newContributor);
    }

    private Identity generateIdentity(CollaborationTp collaboration) {
        return new Identity.Builder().withName(determineContributorName(collaboration)).build();
    }

    private Identity generateContributorIdentityFromAuthorTp(AuthorTp authorTp) {
        var identity = new Identity();
        identity.setName(determineContributorName(authorTp));
        identity.setOrcId(getOrcidAsUriString(authorTp));
        identity.setAdditionalIdentifiers(extractAdditionalIdentifiers(authorTp));
        return identity;
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
        return nonNull(author.getPreferredName()) ? author.getPreferredName().getGivenName()
                                                    + StringUtils.SPACE
                                                    + author.getPreferredName().getSurname()
                   : author.getGivenName() + StringUtils.SPACE + author.getSurname();
    }

    private String determineContributorName(CollaborationTp collaborationTp) {
        return collaborationTp.getIndexedName();
    }

    private String getOrcidAsUriString(AuthorTp authorTp) {
        return isNotBlank(authorTp.getOrcid()) ? craftOrcidUriString(authorTp.getOrcid()) : null;
    }

    private String craftOrcidUriString(String potentiallyMalformedOrcidString) {
        return potentiallyMalformedOrcidString.contains(ORCID_DOMAIN_URL) ? potentiallyMalformedOrcidString
                   : ORCID_DOMAIN_URL + potentiallyMalformedOrcidString;
    }
}
