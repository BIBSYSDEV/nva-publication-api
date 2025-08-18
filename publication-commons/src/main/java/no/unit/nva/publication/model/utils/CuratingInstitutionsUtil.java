package no.unit.nva.publication.model.utils;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.utils.RdfUtils.getTopLevelOrgUri;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.utils.CristinUnitsUtil;

public class CuratingInstitutionsUtil {

    private static CustomerList customerList;
    private final RawContentRetriever uriRetriever;
    private final CustomerService customerService;

    public CuratingInstitutionsUtil(RawContentRetriever uriRetriever, CustomerService customerService) {
        this.customerService = customerService;
        this.uriRetriever = uriRetriever;
    }

    public Set<CuratingInstitution> getCuratingInstitutionsOnline(Publication publication) {
        return getAffiliatedContributors(publication.getEntityDescription()).flatMap(this::toCuratingInstitutionOnline)
                   .filter(this::isCustomer)
                   .collect(Collectors.groupingBy(SimpleEntry::getKey,
                                                  Collectors.mapping(SimpleEntry::getValue, Collectors.toSet())))
                   .entrySet()
                   .stream()
                   .map(entry -> new CuratingInstitution(entry.getKey(), entry.getValue()))
                   .collect(Collectors.toSet());
    }

    public Set<CuratingInstitution> getCuratingInstitutionsCached(EntityDescription entityDescription,
                                                                  CristinUnitsUtil cristinUnitsUtil) {
        return getAffiliatedContributors(entityDescription).flatMap(
                contributor -> toCuratingInstitution(contributor, cristinUnitsUtil))
                   .filter(this::isCustomer)
                   .collect(Collectors.groupingBy(SimpleEntry::getKey,
                                                  Collectors.mapping(SimpleEntry::getValue, Collectors.toSet())))
                   .entrySet()
                   .stream()
                   .map(entry -> new CuratingInstitution(entry.getKey(), entry.getValue()))
                   .collect(Collectors.toSet());
    }

    private static Stream<SimpleEntry<URI, URI>> toCuratingInstitution(Contributor contributor,
                                                                       CristinUnitsUtil cristinUnitsUtil) {
        return getOrganizationIds(contributor).map(cristinUnitsUtil::getTopLevel)
                   .filter(Objects::nonNull)
                   .map(id -> new SimpleEntry<>(id, Optional.of(contributor)
                                                        .map(Contributor::getIdentity)
                                                        .map(Identity::getId)
                                                        .orElse(null)));
    }

    private static Stream<URI> getOrganizationIds(Contributor contributor) {
        return contributor.getAffiliations()
                   .stream()
                   .filter(Organization.class::isInstance)
                   .map(Organization.class::cast)
                   .map(Organization::getId)
                   .filter(Objects::nonNull);
    }

    private static Stream<Contributor> getAffiliatedContributors(EntityDescription entityDescription) {
        return Optional.ofNullable(entityDescription)
                   .map(EntityDescription::getContributors)
                   .orElse(Collections.emptyList())
                   .stream()
                   .filter(CuratingInstitutionsUtil::isAffiliatedContributor);
    }

    private static boolean isAffiliatedContributor(Contributor contributor) {
        return contributor.getAffiliations().stream().anyMatch(Organization.class::isInstance);
    }

    private CustomerList getCustomerList() {
        customerList = nonNull(customerList) ? customerList : customerService.fetchCustomers();
        return customerList;
    }

    private boolean isCustomer(SimpleEntry<URI, URI> entry) {
        return getCustomerList().customers()
                   .stream()
                   .map(CustomerSummary::cristinId)
                   .anyMatch(cristinId -> Objects.equals(cristinId, entry.getKey()));
    }

    private Stream<SimpleEntry<URI, URI>> toCuratingInstitutionOnline(Contributor contributor) {
        return getOrganizationIds(contributor).map(orgId -> getTopLevelOrgUri(uriRetriever, orgId))
                   .filter(Objects::nonNull)
                   .map(topLevelOrgId -> new SimpleEntry<>(topLevelOrgId, contributor.getIdentity().getId()));
    }
}
