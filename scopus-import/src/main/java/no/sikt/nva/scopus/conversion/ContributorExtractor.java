package no.sikt.nva.scopus.conversion;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.AFFILIATION_DELIMITER;
import static no.sikt.nva.scopus.ScopusConstants.ORCID_DOMAIN_URL;
import static no.sikt.nva.scopus.ScopusConverter.extractContentString;
import static no.sikt.nva.scopus.conversion.AffiliationGenerator.getLanguageIso6391Code;
import static no.sikt.nva.scopus.conversion.CristinContributorExtractor.generateContributorFromCristinPerson;
import static nva.commons.core.StringUtils.isNotBlank;
import java.net.URI;
import java.util.ArrayList;
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
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.sikt.nva.scopus.exception.MissingNvaContributorException;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.UnconfirmedOrganization;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.importcandidate.NvaCustomerContributor;
import nva.commons.core.StringUtils;

@SuppressWarnings("PMD.GodClass")
public class ContributorExtractor {

    public static final String FIRST_NAME_CRISTIN_FIELD_NAME = "FirstName";
    public static final String LAST_NAME_CRISTIN_FIELD_NAME = "LastName";
    public static final String NORWAY = "norway";
    public static final String NO_LAND_CODE = "NO";
    public static final String OTHER_INSTITUTIONS = "Andre institusjoner";
    public static final String MISSING_CONTRIBUTORS_OF_NVA_CUSTOMERS_MESSAGE = "None of contributors belongs to NVA "
                                                                               + "customer, all contributors "
                                                                               + "affiliations: ";
    public static final String SCOPUS_AUID = "scopus-auid";
    private final List<CorrespondenceTp> correspondenceTps;
    private final List<AuthorGroupTp> authorGroupTps;
    private final List<NvaCustomerContributor> contributors;
    private final PiaConnection piaConnection;
    private final CristinConnection cristinConnection;
    private final NvaCustomerConnection nvaCustomerConnection;

    public ContributorExtractor(List<CorrespondenceTp> correspondenceTps, List<AuthorGroupTp> authorGroupTps,
                                PiaConnection piaConnection, CristinConnection cristinConnection,
                                NvaCustomerConnection nvaCustomerConnection) {
        this.correspondenceTps = correspondenceTps;
        this.authorGroupTps = authorGroupTps;
        this.contributors = new ArrayList<>();
        this.piaConnection = piaConnection;
        this.cristinConnection = cristinConnection;
        this.nvaCustomerConnection = nvaCustomerConnection;
    }

    public List<NvaCustomerContributor> generateContributors() {
        var contributors = authorGroupTps.stream()
                               .map(this::generateContributorsFromAuthorGroup)
                               .flatMap(List::stream)
                               .toList();
        if (noContributorsBelongingToNvaCustomer(contributors)) {
            var affiliationsIds = getAllAffiliationIds(contributors);
            throw new MissingNvaContributorException(MISSING_CONTRIBUTORS_OF_NVA_CUSTOMERS_MESSAGE + affiliationsIds);
        } else {
            return contributors;
        }
    }

    private static boolean isNotOtherNorwegianInstitution(CristinOrganization org) {
        return !(NO_LAND_CODE.equals(org.country()) && org.labels().containsValue(OTHER_INSTITUTIONS));
    }

    private static boolean isNotNorway(Map<String, String> labels) {
        return labels.values().stream().map(String::toLowerCase).noneMatch(NORWAY::equals);
    }

    private static String toOrganizationName(List<OrganizationTp> organizationTps) {
        return organizationTps.stream()
                   .map(organizationTp -> extractContentString(organizationTp.getContent()))
                   .collect(Collectors.joining(AFFILIATION_DELIMITER));
    }

    private List<Contributor> getContributors() {
        return contributors.stream().map(Contributor.class::cast).toList();
    }

    private boolean noContributorsBelongingToNvaCustomer(List<NvaCustomerContributor> contributors) {
        return contributors.stream().noneMatch(NvaCustomerContributor::belongsToNvaCustomer);
    }

    private static List<URI> getAllAffiliationIds(List<NvaCustomerContributor> contributors) {
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

    private Optional<PersonalnameType> extractPersonalNameType(CorrespondenceTp correspondenceTp) {
        return Optional.ofNullable(correspondenceTp.getPerson());
    }

    private List<NvaCustomerContributor> generateContributorsFromAuthorGroup(AuthorGroupTp authorGroupTp) {
        authorGroupTp.getAuthorOrCollaboration()
            .forEach(authorOrCollaboration -> extractContributorFromAuthorOrCollaboration(authorOrCollaboration,
                                                                                          authorGroupTp));
        return contributors;
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

    private PersonalnameType getCorrespondencePerson() {
        return correspondenceTps.stream()
                   .map(this::extractPersonalNameType)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .findFirst()
                   .orElse(null);
    }

    private Optional<NvaCustomerContributor> getExistingContributor(Object authorOrCollaboration) {
        return contributors.stream()
                   .filter(contributor -> compareContributorToAuthorOrCollaboration(contributor, authorOrCollaboration))
                   .findAny();
    }

    private void replaceExistingContributor(NvaCustomerContributor existingContributor, AuthorGroupTp authorGroupTp) {
        if (isNull(existingContributor.getIdentity().getId())) {
            var optionalNewAffiliations = generateAffiliationFromAuthorGroupTp(authorGroupTp);
            optionalNewAffiliations.ifPresent(
                organizations -> updateContributorWithAdditionalAffiliationsInContributorList(organizations,
                                                                                              existingContributor));
        }
    }

    private void updateContributorWithAdditionalAffiliationsInContributorList(
        Corporation newAffiliation, NvaCustomerContributor matchingContributor) {
        var newContributor = cloneContributorAddingAffiliation(matchingContributor, newAffiliation);
        replaceContributor(matchingContributor, newContributor);
    }

    private void replaceContributor(NvaCustomerContributor oldContributor, NvaCustomerContributor newContributor) {
        contributors.remove(oldContributor);
        contributors.add(newContributor);
    }

    private NvaCustomerContributor cloneContributorAddingAffiliation(NvaCustomerContributor existingContributor,
                                                                     Corporation newAffiliation) {
        var affiliations = new ArrayList<>(existingContributor.getAffiliations());
        affiliations.add(newAffiliation);

        return new NvaCustomerContributor.Builder().withIdentity(existingContributor.getIdentity())
                   .withAffiliations(affiliations)
                   .withRole(existingContributor.getRole())
                   .withSequence(existingContributor.getSequence())
                   .withCorrespondingAuthor(existingContributor.isCorrespondingAuthor())
                   .build();
    }

    private boolean compareContributorToAuthorOrCollaboration(NvaCustomerContributor contributor,
                                                              Object authorOrCollaboration) {
        return authorOrCollaboration instanceof AuthorTp authorTp ? isSamePerson(authorTp, contributor)
                   : isSameSequenceElement((CollaborationTp) authorOrCollaboration, contributor);
    }

    private boolean isSameSequenceElement(CollaborationTp collaboration, NvaCustomerContributor contributor) {
        return collaboration.getSeq().equals(contributor.getSequence().toString());
    }

    private boolean isSamePerson(AuthorTp author, NvaCustomerContributor contributor) {
        if (author.getSeq().equals(contributor.getSequence().toString())) {
            return true;
        } else if (nonNull(author.getOrcid()) && nonNull(contributor.getIdentity().getOrcId())) {
            return craftOrcidUriString(author.getOrcid()).equals(contributor.getIdentity().getOrcId());
        } else {
            return false;
        }
    }

    private void generateContributorFromAuthorOrCollaboration(Object authorOrCollaboration,
                                                              AuthorGroupTp authorGroupTp) {
        if (authorOrCollaboration instanceof AuthorTp authorTp) {
            generateContributorFromAuthorTp(authorTp, authorGroupTp);
        } else {
            generateContributorFromCollaborationTp((CollaborationTp) authorOrCollaboration, authorGroupTp,
                                                   getCorrespondencePerson());
        }
    }

    private void generateContributorFromAuthorTp(AuthorTp author, AuthorGroupTp authorGroup) {

        var cristinOrganization = fetchCristinOrganization(authorGroup);
        var isNvaCustomer = nvaCustomerConnection.isNvaCustomer(cristinOrganization);

        var contributor = fetchCristinPerson(author).map(
                cristinPerson -> generateContributorFromCristinPerson(cristinPerson, author, getCorrespondencePerson(),
                                                                      cristinOrganization, isNvaCustomer))
                              .orElseGet(
                                  () -> generateContributorFromAuthorTp(authorGroup, author, getCorrespondencePerson(),
                                                                        cristinOrganization, isNvaCustomer));

        contributors.add(contributor);
    }

    private NvaCustomerContributor generateContributorFromAuthorTp(AuthorGroupTp authorGroup, AuthorTp author,
                                                                   PersonalnameType correspondencePerson,
                                                                   CristinOrganization cristinOrganization,
                                                                   boolean isNvaCustomer) {
        return new NvaCustomerContributor.Builder().withIdentity(generateContributorIdentityFromAuthorTp(author))
                   .withAffiliations(extractAffiliation(authorGroup, cristinOrganization))
                   .withRole(new RoleType(Role.CREATOR))
                   .withSequence(getSequenceNumber(author))
                   .withCorrespondingAuthor(isCorrespondingAuthor(author, correspondencePerson))
                   .withBelongsToNvaCustomer(isNvaCustomer)
                   .build();
    }

    private List<Corporation> extractAffiliation(AuthorGroupTp authorGroup, CristinOrganization cristinOrganization) {
        return nonNull(cristinOrganization)
                   ? AffiliationGenerator.fromCristinOrganization(cristinOrganization)
                   : AffiliationGenerator.fromAuthorGroupTp(authorGroup, cristinConnection);
    }

    private Optional<CristinPerson> fetchCristinPerson(AuthorTp author) {
        var cristinPerson = fetchCristinPersonByScopusId(author);
        return cristinPerson.isPresent()
                   ? cristinPerson
                   : fetchCristinPersonByOrcId(author);
    }

    private Optional<CristinPerson> fetchCristinPersonByOrcId(AuthorTp author) {
        return nonNull(author.getOrcid())
                   ? cristinConnection.getCristinPersonByOrcId(author.getOrcid())
                   : Optional.empty();
    }

    private Optional<CristinPerson> fetchCristinPersonByScopusId(AuthorTp author) {
        return piaConnection.getCristinPersonIdentifier(author.getAuid())
                   .flatMap(cristinConnection::getCristinPersonByCristinId);
    }

    private CristinOrganization fetchCristinOrganization(AuthorGroupTp authorGroup) {
        return getCristinOrganizationUri(authorGroup).map(cristinConnection::fetchCristinOrganizationByCristinId)
                   .filter(ContributorExtractor::isNotOtherNorwegianInstitution)
                   .orElse(null);
    }

    private Optional<URI> getCristinOrganizationUri(AuthorGroupTp authorGroup) {
        return Optional.ofNullable(authorGroup)
                   .map(AuthorGroupTp::getAffiliation)
                   .map(AffiliationTp::getAfid)
                   .flatMap(piaConnection::fetchCristinOrganizationIdentifier);
    }

    private Optional<Corporation> generateAffiliationFromCristinOrganization(CristinOrganization cristinOrganization) {
        return Optional.of(new Organization.Builder().withId(cristinOrganization.id()).build());
    }

    private void generateContributorFromCollaborationTp(CollaborationTp collaboration, AuthorGroupTp authorGroupTp,
                                                        PersonalnameType correspondencePerson) {
        var cristinOrganization = fetchCristinOrganization(authorGroupTp);

        var newContributor = new NvaCustomerContributor.Builder().withIdentity(generateIdentity(collaboration))
                                 .withAffiliations(generateAffiliation(cristinOrganization, authorGroupTp).map(List::of)
                                                       .orElse(emptyList()))
                                 .withRole(new RoleType(Role.OTHER))
                                 .withSequence(getSequenceNumber(collaboration))
                                 .withCorrespondingAuthor(isCorrespondingAuthor(collaboration, correspondencePerson))
                                 .withBelongsToNvaCustomer(nvaCustomerConnection.isNvaCustomer(cristinOrganization))
                                 .build();
        contributors.add(newContributor);
    }

    private Identity generateIdentity(CollaborationTp collaboration) {
        return new Identity.Builder().withName(determineContributorName(collaboration)).build();
    }

    private Optional<Corporation> generateAffiliation(CristinOrganization cristinOrganization,
                                                       AuthorGroupTp authorGroupTp) {
        return nonNull(cristinOrganization) ? generateAffiliationFromCristinOrganization(cristinOrganization)
                   : generateAffiliationFromAuthorGroupTp(authorGroupTp);
    }

    private Identity generateContributorIdentityFromAuthorTp(AuthorTp authorTp) {
        var identity = new Identity();
        identity.setName(determineContributorName(authorTp));
        identity.setOrcId(getOrcidAsUriString(authorTp));
        identity.setAdditionalIdentifiers(extractAdditionalIdentifiers(authorTp));
        return identity;
    }

    protected static List<AdditionalIdentifier> extractAdditionalIdentifiers(AuthorTp authorTp) {
        return isNotBlank(authorTp.getAuid())
                   ? List.of(createAuidAdditionalIdentifier(authorTp))
                   : emptyList();
    }

    private static AdditionalIdentifier createAuidAdditionalIdentifier(AuthorTp authorTp) {
        return new AdditionalIdentifier(SCOPUS_AUID, authorTp.getAuid());
    }

    private Optional<Corporation> generateAffiliationFromAuthorGroupTp(AuthorGroupTp authorGroup) {
        var name =  getOrganizationNameFromAuthorGroup(authorGroup);
        var labels =  name.isPresent() && !name.get().isEmpty() ? name.map(
            organizationName -> Map.of(getLanguageIso6391Code(organizationName), organizationName))
                          : extractCountryNameAsAffiliation(authorGroup);
        return isNotNorway(labels.orElse(Map.of()))
                   ? Optional.of(new UnconfirmedOrganization(name.orElse(null)))
                   : Optional.empty();
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

    private Optional<Map<String, String>> extractCountryNameAsAffiliation(AuthorGroupTp authorGroup) {
        return Optional.ofNullable(authorGroup.getAffiliation())
                   .map(AffiliationTp::getCountry)
                   .map(country -> Map.of(getLanguageIso6391Code(country), country));
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
}
