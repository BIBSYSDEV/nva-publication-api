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
import java.util.Objects;
import java.util.Optional;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.PersonalnameType;
import no.sikt.nva.scopus.conversion.model.AuthorIdentifiers;
import no.sikt.nva.scopus.conversion.model.CorporationWithContributors;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.Identity;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.StringUtils;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class ContributorExtractor {

    public static final String FIRST_NAME_CRISTIN_FIELD_NAME = "FirstName";
    public static final String LAST_NAME_CRISTIN_FIELD_NAME = "LastName";
    public static final String SCOPUS_AUID = "scopus-auid";

    private final AffiliationGenerator affiliationGenerator;
    private final CristinPersonRetriever cristinPersonRetriever;

    public ContributorExtractor(PiaConnection piaConnection, CristinConnection cristinConnection) {
        this.affiliationGenerator = new AffiliationGenerator(piaConnection, cristinConnection);
        this.cristinPersonRetriever = new CristinPersonRetriever(cristinConnection, piaConnection);
    }

    public static String getOrcidAsUriString(AuthorTp authorTp) {
        return isNotBlank(authorTp.getOrcid()) ? craftOrcidUriString(authorTp.getOrcid()) : null;
    }

    public ContributorsOrganizationsWrapper generateContributors(List<CorrespondenceTp> correspondenceTps,
                                                                 List<AuthorGroupTp> authorGroupTps) {
        var cristinAffiliationsAuthorgroupsTps = affiliationGenerator.getCorporations(authorGroupTps);
        var cristinPersons = cristinPersonRetriever.retrieveCristinPersons(authorGroupTps);

        var contributors = cristinAffiliationsAuthorgroupsTps.stream()
                               .map(corporationWithContributors -> generateContributorsFromAuthorGroup(corporationWithContributors, cristinPersons, emptyList(), correspondenceTps))
                               .reduce(emptyList(), this::mergeContributorLists);

        var cristinTopLevelOrganizations = getCristinOrganizations(cristinAffiliationsAuthorgroupsTps);

        return new ContributorsOrganizationsWrapper(contributors, cristinTopLevelOrganizations);
    }

    protected static List<AdditionalIdentifier> extractAdditionalIdentifiers(AuthorTp authorTp) {
        return isNotBlank(authorTp.getAuid()) ? List.of(createAuidAdditionalIdentifier(authorTp)) : emptyList();
    }

    private static AdditionalIdentifier createAuidAdditionalIdentifier(AuthorTp authorTp) {
        return new AdditionalIdentifier(SCOPUS_AUID, authorTp.getAuid());
    }

    private static String craftOrcidUriString(String potentiallyMalformedOrcidString) {
        return potentiallyMalformedOrcidString.contains(ORCID_DOMAIN_URL)
                   ? potentiallyMalformedOrcidString
                   : ORCID_DOMAIN_URL + potentiallyMalformedOrcidString;
    }

    private static List<URI> getCristinOrganizations(List<CorporationWithContributors> corporationWithContributors) {
        return corporationWithContributors.stream()
                   .map(CorporationWithContributors::getCristinOrganizations)
                   .flatMap(Collection::stream)
                   .filter(Objects::nonNull)
                   .map(CristinOrganization::getTopLevelOrgId)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .toList();
    }

    private List<Contributor> mergeContributorLists(List<Contributor> existing, List<Contributor> newContributors) {
        var result = new ArrayList<>(existing);
        result.addAll(newContributors);
        return result;
    }

    private List<Contributor> generateContributorsFromAuthorGroup(
        CorporationWithContributors corporationWithContributors,
        Map<AuthorIdentifiers, CristinPerson> cristinPersons,
        List<Contributor> existingContributors, List<CorrespondenceTp> correspondenceTps) {

        return corporationWithContributors.getScopusAuthors()
                   .getAuthorOrCollaboration()
                   .stream()
                   .map(authorOrCollaboration ->
                            processAuthorOrCollaboration(authorOrCollaboration, corporationWithContributors,
                                                         cristinPersons, existingContributors, correspondenceTps))
                   .reduce(existingContributors, this::mergeContributorLists);
    }

    private List<Contributor> processAuthorOrCollaboration(
        Object authorOrCollaboration,
        CorporationWithContributors corporationWithContributors,
        Map<AuthorIdentifiers, CristinPerson> cristinPersons,
        List<Contributor> existingContributors,
        List<CorrespondenceTp> correspondenceTps) {

        var existingContributor = findExistingContributor(authorOrCollaboration, existingContributors);

        if (existingContributor.isPresent()) {
            return updateExistingContributor(existingContributor.get(), corporationWithContributors,
                                             existingContributors);
        } else {
            var newContributor = createContributor(authorOrCollaboration, corporationWithContributors, cristinPersons, correspondenceTps);
            return addContributor(newContributor, existingContributors);
        }
    }

    private List<Contributor> addContributor(Contributor contributor, List<Contributor> existingContributors) {
        var result = new ArrayList<>(existingContributors);
        result.add(contributor);
        return result;
    }

    private List<Contributor> updateExistingContributor(
        Contributor existingContributor,
        CorporationWithContributors corporationWithContributors,
        List<Contributor> existingContributors) {

        if (isNull(existingContributor.getIdentity().getId())) {
            var newAffiliations = corporationWithContributors.toCorporations();
            if (!newAffiliations.isEmpty()) {
                return replaceContributor(existingContributor,
                                          enrichContributorWithAffiliations(existingContributor, newAffiliations),
                                          existingContributors);
            }
        }
        return existingContributors;
    }

    private List<Contributor> replaceContributor(Contributor oldContributor,
                                                 Contributor newContributor,
                                                 List<Contributor> existingContributors) {
        return existingContributors.stream()
                   .map(contributor -> contributor.equals(oldContributor) ? newContributor : contributor)
                   .toList();
    }

    private Contributor enrichContributorWithAffiliations(Contributor existingContributor,
                                                          List<Corporation> newAffiliations) {
        var affiliations = new ArrayList<>(existingContributor.getAffiliations());
        affiliations.addAll(newAffiliations);

        return new Contributor.Builder()
                   .withIdentity(existingContributor.getIdentity())
                   .withAffiliations(affiliations.stream().distinct().toList())
                   .withRole(existingContributor.getRole())
                   .withSequence(existingContributor.getSequence())
                   .withCorrespondingAuthor(existingContributor.isCorrespondingAuthor())
                   .build();
    }

    private Optional<Contributor> findExistingContributor(Object authorOrCollaboration,
                                                          List<Contributor> contributors) {
        return contributors.stream()
                   .filter(contributor -> matchesAuthorOrCollaboration(contributor, authorOrCollaboration))
                   .findAny();
    }

    private boolean matchesAuthorOrCollaboration(Contributor contributor, Object authorOrCollaboration) {
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
        }
        return false;
    }

    private Contributor createContributor(Object authorOrCollaboration,
                                          CorporationWithContributors corporationWithContributors,
                                          Map<AuthorIdentifiers, CristinPerson> cristinPersons, List<CorrespondenceTp> correspondenceTps) {
        return authorOrCollaboration instanceof AuthorTp authorTp
                   ? createContributorFromAuthorTp(authorTp, corporationWithContributors, cristinPersons, correspondenceTps)
                   : createContributorFromCollaborationTp((CollaborationTp) authorOrCollaboration,
                                                          corporationWithContributors, correspondenceTps);
    }

    private Contributor createContributorFromAuthorTp(
        AuthorTp author,
        CorporationWithContributors corporationWithContributors,
        Map<AuthorIdentifiers, CristinPerson> cristinPersons, List<CorrespondenceTp> correspondenceTps) {

        var cristinOrganizations = corporationWithContributors.getCristinOrganizations();
        var authorIdentifiers = new AuthorIdentifiers(author.getAuid(), author.getOrcid());

        return Optional.ofNullable(cristinPersons.get(authorIdentifiers))
                   .map(cristinPerson -> generateContributorFromCristinPerson(
                       cristinPerson, author, getCorrespondencePerson(correspondenceTps), cristinOrganizations))
                   .orElseGet(() -> buildContributorFromAuthorTp(corporationWithContributors, author, correspondenceTps));
    }

    private Contributor buildContributorFromAuthorTp(CorporationWithContributors corporationWithContributors,
                                                     AuthorTp author, List<CorrespondenceTp> correspondenceTps) {
        return new Contributor.Builder()
                   .withIdentity(generateIdentityFromAuthorTp(author))
                   .withAffiliations(corporationWithContributors.toCorporations())
                   .withRole(new RoleType(Role.CREATOR))
                   .withSequence(getSequenceNumber(author))
                   .withCorrespondingAuthor(isCorrespondingAuthor(author, getCorrespondencePerson(correspondenceTps)))
                   .build();
    }

    private Contributor createContributorFromCollaborationTp(
        CollaborationTp collaboration,
        CorporationWithContributors corporationWithContributors,
        List<CorrespondenceTp> correspondenceTps) {

        return new Contributor.Builder()
                   .withIdentity(generateIdentity(collaboration))
                   .withAffiliations(corporationWithContributors.toCorporations())
                   .withRole(new RoleType(Role.OTHER))
                   .withSequence(getSequenceNumber(collaboration))
                   .withCorrespondingAuthor(isCorrespondingAuthor(collaboration, getCorrespondencePerson(correspondenceTps)))
                   .build();
    }

    private Identity generateIdentity(CollaborationTp collaboration) {
        return new Identity.Builder()
                   .withName(determineContributorName(collaboration))
                   .build();
    }

    private Identity generateIdentityFromAuthorTp(AuthorTp authorTp) {
        var identity = new Identity();
        identity.setName(determineContributorName(authorTp));
        identity.setOrcId(getOrcidAsUriString(authorTp));
        identity.setAdditionalIdentifiers(extractAdditionalIdentifiers(authorTp));
        return identity;
    }

    private PersonalnameType getCorrespondencePerson(List<CorrespondenceTp> correspondenceTps) {
        return correspondenceTps.stream()
                   .map(this::extractPersonalNameType)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .findFirst()
                   .orElse(null);
    }

    private Optional<PersonalnameType> extractPersonalNameType(CorrespondenceTp correspondenceTp) {
        return Optional.ofNullable(correspondenceTp.getPerson());
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
        return nonNull(author.getPreferredName())
                   ? author.getPreferredName().getGivenName() + StringUtils.SPACE + author.getPreferredName().getSurname()
                   : author.getGivenName() + StringUtils.SPACE + author.getSurname();
    }

    private String determineContributorName(CollaborationTp collaborationTp) {
        return collaborationTp.getIndexedName();
    }

    public record ContributorsOrganizationsWrapper(List<Contributor> contributors, List<URI> topLevelOrgs) {}
}