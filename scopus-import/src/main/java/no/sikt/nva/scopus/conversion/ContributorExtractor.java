package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.ORCID_DOMAIN_URL;
import static no.sikt.nva.scopus.conversion.CristinContributorExtractor.generateContributorFromCristinPerson;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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
import no.sikt.nva.scopus.conversion.model.AuthorGroupWithCristinOrganization;
import no.sikt.nva.scopus.conversion.model.AuthorIdentifiers;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.model.Identity;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.importcandidate.ImportContributor;
import nva.commons.core.StringUtils;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class ContributorExtractor {

    public static final String FIRST_NAME_CRISTIN_FIELD_NAME = "FirstName";
    public static final String LAST_NAME_CRISTIN_FIELD_NAME = "LastName";
    public static final String SCOPUS_AUID = "scopus-auid";
    public static final String FULL_NAME_PATTERN = "%s %s";

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

        var allContributors = corporationsWithAuthors.stream()
                               .flatMap(corp -> processAuthorGroup(corp, cristinPersons, correspondencePerson).stream())
                               .toList();

        var contributors = deduplicateContributors(allContributors);

        var topLevelOrgs = extractTopLevelOrganizations(corporationsWithAuthors);

        return new ContributorsOrganizationsWrapper(contributors, topLevelOrgs);
    }

    private List<ImportContributor> deduplicateContributors(List<ImportContributor> contributors) {
        var contributorMap = new LinkedHashMap<String, ImportContributor>();

        for (ImportContributor contributor : contributors) {
            var key = getContributorKey(contributor).orElse(null);

            if (contributorMap.containsKey(key)) {
                var existing = contributorMap.get(key);
                if (nonNull(existing) && hasNoId(existing)) {
                    var mergedContributor = mergeContributors(existing, contributor);
                    contributorMap.put(key, mergedContributor);
                }
            } else {
                contributorMap.put(key, contributor);
            }
        }

        return new ArrayList<>(contributorMap.values());
    }

    private static boolean hasNoId(ImportContributor contributor) {
        return Optional.of(contributor).map(ImportContributor::identity).map(Identity::getId).isEmpty();
    }

    private Optional<String> getContributorKey(ImportContributor contributor) {
        return getScopusAuid(contributor).or(() -> getOrcId(contributor));
    }

    private Optional<String> getOrcId(ImportContributor contributor) {
        return Optional.ofNullable(contributor).map(ImportContributor::identity).map(Identity::getOrcId);
    }

    private Optional<String> getScopusAuid(ImportContributor contributor) {
        return Optional.ofNullable(contributor)
                   .map(ImportContributor::identity)
                   .map(Identity::getAdditionalIdentifiers)
                   .stream()
                   .flatMap(List::stream)
                   .filter(additionalIdentifier -> SCOPUS_AUID.equals(additionalIdentifier.sourceName()))
                   .map(AdditionalIdentifier::value)
                   .findFirst();
    }

    private ImportContributor mergeContributors(ImportContributor existing, ImportContributor newContributor) {
        var mergedAffiliations = new ArrayList<>(existing.affiliations());
        mergedAffiliations.addAll(newContributor.affiliations());
        var mergedIdentity = mergeIdentities(existing.identity(), newContributor.identity());
        return new ImportContributor(mergedIdentity,
                                     mergedAffiliations.stream().distinct().toList(),
                                     existing.role(),
                                     existing.sequence(),
                                     existing.correspondingAuthor());
    }

    private Identity mergeIdentities(Identity identity, Identity duplicatedIdentity) {
        return new Identity.Builder()
                   .withName(identity.getName())
                   .withAdditionalIdentifiers(identity.getAdditionalIdentifiers())
                   .withOrcId(Optional.ofNullable(identity.getOrcId()).orElse(duplicatedIdentity.getOrcId()))
                   .withVerificationStatus(identity.getVerificationStatus())
                   .withAdditionalIdentifiers(identity.getAdditionalIdentifiers())
                   .withNameType(identity.getNameType())
                   .build();
    }

    private List<ImportContributor> processAuthorGroup(AuthorGroupWithCristinOrganization authorGroupWithCristinOrganization,
                                                 Map<AuthorIdentifiers, CristinPerson> cristinPersons,
                                                 PersonalnameType correspondencePerson) {
        return authorGroupWithCristinOrganization.getScopusAuthors()
                   .getAuthorOrCollaboration()
                   .stream()
                   .map(authorOrCollaboration -> createContributor(authorOrCollaboration,
                                                                   authorGroupWithCristinOrganization,
                                                                   cristinPersons, correspondencePerson))
                   .toList();
    }

    private ImportContributor createContributor(Object authorOrCollaboration,
                                          AuthorGroupWithCristinOrganization authorGroupWithCristinOrganization,
                                          Map<AuthorIdentifiers, CristinPerson> cristinPersons,
                                          PersonalnameType correspondencePerson) {
        return switch (authorOrCollaboration) {
            case AuthorTp author ->
                createFromAuthor(author, authorGroupWithCristinOrganization, cristinPersons, correspondencePerson);
            case CollaborationTp collaboration ->
                createFromCollaboration(collaboration, authorGroupWithCristinOrganization, correspondencePerson);
            default -> throw new IllegalArgumentException("Unknown type: " + authorOrCollaboration.getClass());
        };
    }

    private ImportContributor createFromAuthor(AuthorTp author,
                                          AuthorGroupWithCristinOrganization authorGroupWithCristinOrganization,
                                         Map<AuthorIdentifiers, CristinPerson> cristinPersons,
                                         PersonalnameType correspondencePerson) {
        var authorIdentifiers = new AuthorIdentifiers(author.getAuid(), author.getOrcid());

        return Optional.ofNullable(cristinPersons.get(authorIdentifiers))
                   .map(cristinPerson -> generateContributorFromCristinPerson(cristinPerson, author,
                                                                              correspondencePerson,
                                                                              authorGroupWithCristinOrganization))
                   .orElseGet(() -> buildFromScopusAuthor(author, authorGroupWithCristinOrganization, correspondencePerson));
    }

    private ImportContributor buildFromScopusAuthor(AuthorTp author,
                                               AuthorGroupWithCristinOrganization authorGroupWithCristinOrganization,
                                              PersonalnameType correspondencePerson) {
        return new ImportContributor(createIdentity(author),
                                     authorGroupWithCristinOrganization.toCorporations(),
                                     new RoleType(Role.CREATOR),
                                     Integer.parseInt(author.getSeq()),
                                     isCorrespondingAuthor(author, correspondencePerson));
    }

    private ImportContributor createFromCollaboration(CollaborationTp collaboration,
                                                AuthorGroupWithCristinOrganization authorGroupWithCristinOrganization,
                                                PersonalnameType correspondencePerson) {
        return new ImportContributor(new Identity.Builder().withName(collaboration.getIndexedName()).build(),
                                     authorGroupWithCristinOrganization.toCorporations(),
                                     new RoleType(Role.OTHER),
                                     Integer.parseInt(collaboration.getSeq()),
                                     isCorrespondingAuthor(collaboration, correspondencePerson));
    }

    private Identity createIdentity(AuthorTp authorTp) {
        return new Identity.Builder().withName(getAuthorName(authorTp))
                   .withOrcId(getOrcidUri(authorTp).orElse(null))
                   .withAdditionalIdentifiers(getAdditionalIdentifier(authorTp))
                   .build();
    }

    private String getAuthorName(AuthorTp author) {
        return nonNull(author.getPreferredName())
                   ? FULL_NAME_PATTERN.formatted(author.getPreferredName().getGivenName(), author.getPreferredName().getSurname())
                   : FULL_NAME_PATTERN.formatted(author.getGivenName(), author.getSurname());
    }

    private Optional<String> getOrcidUri(AuthorTp authorTp) {
        return Optional.ofNullable(authorTp)
                   .map(AuthorTp::getOrcid)
                   .filter(StringUtils::isNotBlank)
                   .map(this::normalizeOrcid);
    }

    private String normalizeOrcid(String orcid) {
        return orcid.contains(ORCID_DOMAIN_URL)
                   ? orcid
                   : ORCID_DOMAIN_URL + orcid;
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
        return nonNull(correspondencePerson)
               && Optional.ofNullable(author.getIndexedName()).isPresent()
               && author.getIndexedName().equals(correspondencePerson.getIndexedName());
    }

    private Collection<URI> extractTopLevelOrganizations(Collection<AuthorGroupWithCristinOrganization> corporationsWithContributors) {
        return corporationsWithContributors.stream()
                   .map(AuthorGroupWithCristinOrganization::getCristinOrganizations)
                   .flatMap(Collection::stream)
                   .map(CristinOrganization::getTopLevelOrgId)
                   .flatMap(Optional::stream)
                   .toList();
    }

    public record ContributorsOrganizationsWrapper(List<ImportContributor> contributors, Collection<URI> topLevelOrgs) {

    }
}