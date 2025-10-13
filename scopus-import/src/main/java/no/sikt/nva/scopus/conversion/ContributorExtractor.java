package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.ORCID_DOMAIN_URL;
import static no.sikt.nva.scopus.conversion.CristinContributorExtractor.generateContributorFromCristinPerson;
import static nva.commons.core.StringUtils.isNotBlank;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.PersonalnameType;
import no.sikt.nva.scopus.conversion.model.AuthorIdentifiers;
import no.sikt.nva.scopus.conversion.model.CorporationWithContributors;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.model.Contributor;
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

    public ContributorsOrganizationsWrapper generateContributors(DocTp document) {
        var bibrecord = document.getItem().getItem().getBibrecord().getHead();
        return generateContributors(bibrecord.getCorrespondence(), bibrecord.getAuthorGroup());
    }

    public ContributorsOrganizationsWrapper generateContributors(List<CorrespondenceTp> correspondenceTps,
                                                                 List<AuthorGroupTp> authorGroupTps) {
        var corporationsWithAuthors = affiliationGenerator.getCorporations(authorGroupTps);
        var cristinPersons = cristinPersonRetriever.retrieveCristinPersons(authorGroupTps);
        var correspondencePerson = getCorrespondencePerson(correspondenceTps);

        var contributors = corporationsWithAuthors.stream()
                               .flatMap(corp -> processAuthorGroup(corp, cristinPersons, correspondencePerson).stream())
                               .toList();

        var topLevelOrgs = extractTopLevelOrganizations(corporationsWithAuthors);

        return new ContributorsOrganizationsWrapper(contributors, topLevelOrgs);
    }

    private List<Contributor> processAuthorGroup(CorporationWithContributors corporationWithContributors,
                                                 Map<AuthorIdentifiers, CristinPerson> cristinPersons,
                                                 PersonalnameType correspondencePerson) {
        return corporationWithContributors.getScopusAuthors()
                   .getAuthorOrCollaboration()
                   .stream()
                   .map(authorOrCollaboration -> createContributor(authorOrCollaboration, corporationWithContributors,
                                                                   cristinPersons, correspondencePerson))
                   .toList();
    }

    private Contributor createContributor(Object authorOrCollaboration,
                                          CorporationWithContributors corporationWithContributors,
                                          Map<AuthorIdentifiers, CristinPerson> cristinPersons,
                                          PersonalnameType correspondencePerson) {
        return switch (authorOrCollaboration) {
            case AuthorTp author ->
                createFromAuthor(author, corporationWithContributors, cristinPersons, correspondencePerson);
            case CollaborationTp collaboration ->
                createFromCollaboration(collaboration, corporationWithContributors, correspondencePerson);
            default -> throw new IllegalArgumentException("Unknown type: " + authorOrCollaboration.getClass());
        };
    }

    private Contributor createFromAuthor(AuthorTp author, CorporationWithContributors corporationWithContributors,
                                         Map<AuthorIdentifiers, CristinPerson> cristinPersons,
                                         PersonalnameType correspondencePerson) {
        var authorIdentifiers = new AuthorIdentifiers(author.getAuid(), author.getOrcid());

        return Optional.ofNullable(cristinPersons.get(authorIdentifiers))
                   .map(cristinPerson -> generateContributorFromCristinPerson(cristinPerson, author,
                                                                              correspondencePerson,
                                                                              corporationWithContributors.getCristinOrganizations()))
                   .orElseGet(() -> buildFromScopusAuthor(author, corporationWithContributors, correspondencePerson));
    }

    private Contributor buildFromScopusAuthor(AuthorTp author, CorporationWithContributors corporationWithContributors,
                                              PersonalnameType correspondencePerson) {
        return new Contributor.Builder().withIdentity(createIdentity(author))
                   .withAffiliations(corporationWithContributors.toCorporations())
                   .withRole(new RoleType(Role.CREATOR))
                   .withSequence(Integer.parseInt(author.getSeq()))
                   .withCorrespondingAuthor(isCorrespondingAuthor(author, correspondencePerson))
                   .build();
    }

    private Contributor createFromCollaboration(CollaborationTp collaboration,
                                                CorporationWithContributors corporationWithContributors,
                                                PersonalnameType correspondencePerson) {
        return new Contributor.Builder().withIdentity(
                new Identity.Builder().withName(collaboration.getIndexedName()).build())
                   .withAffiliations(corporationWithContributors.toCorporations())
                   .withRole(new RoleType(Role.OTHER))
                   .withSequence(Integer.parseInt(collaboration.getSeq()))
                   .withCorrespondingAuthor(isCorrespondingAuthor(collaboration, correspondencePerson))
                   .build();
    }

    private Identity createIdentity(AuthorTp authorTp) {
        return new Identity.Builder().withName(getAuthorName(authorTp))
                   .withOrcId(getOrcidUri(authorTp))
                   .withAdditionalIdentifiers(getAdditionalIdentifier(authorTp))
                   .build();
    }

    private String getAuthorName(AuthorTp author) {
        return nonNull(author.getPreferredName()) ? author.getPreferredName().getGivenName()
                                                    + StringUtils.SPACE
                                                    + author.getPreferredName().getSurname()
                   : author.getGivenName() + StringUtils.SPACE + author.getSurname();
    }

    private String getOrcidUri(AuthorTp authorTp) {
        return isNotBlank(authorTp.getOrcid()) ? normalizeOrcid(authorTp.getOrcid()) : null;
    }

    private String normalizeOrcid(String orcid) {
        return orcid.contains(ORCID_DOMAIN_URL) ? orcid : ORCID_DOMAIN_URL + orcid;
    }

    private List<AdditionalIdentifier> getAdditionalIdentifier(AuthorTp authorTp) {
        return Optional.ofNullable(authorTp)
                   .map(AuthorTp::getAuid)
                   .filter(StringUtils::isNotBlank)
                   .map(id -> new AdditionalIdentifier(SCOPUS_AUID, id))
                   .map(List::of)
                   .orElseGet(Collections::emptyList);
    }

    private PersonalnameType getCorrespondencePerson(List<CorrespondenceTp> correspondenceTps) {
        return correspondenceTps.stream()
                   .map(CorrespondenceTp::getPerson)
                   .filter(Objects::nonNull)
                   .findFirst()
                   .orElse(null);
    }

    private boolean isCorrespondingAuthor(CollaborationTp collaboration, PersonalnameType correspondencePerson) {
        return nonNull(correspondencePerson) && collaboration.getIndexedName()
                                                    .equals(correspondencePerson.getIndexedName());
    }

    private boolean isCorrespondingAuthor(AuthorTp author, PersonalnameType correspondencePerson) {
        return nonNull(correspondencePerson) && author.getIndexedName().equals(correspondencePerson.getIndexedName());
    }

    private Collection<URI> extractTopLevelOrganizations(Collection<CorporationWithContributors> corporationsWithContributors) {
        return corporationsWithContributors.stream()
                   .map(CorporationWithContributors::getCristinOrganizations)
                   .flatMap(Collection::stream)
                   .map(CristinOrganization::getTopLevelOrgId)
                   .flatMap(Optional::stream)
                   .toList();
    }

    public record ContributorsOrganizationsWrapper(List<Contributor> contributors, Collection<URI> topLevelOrgs) {

    }
}