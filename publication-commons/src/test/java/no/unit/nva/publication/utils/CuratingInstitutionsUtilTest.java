package no.unit.nva.publication.utils;

import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.List;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.utils.CuratingInstitutionsUtil;
import no.unit.nva.publication.model.utils.CustomerList;
import no.unit.nva.publication.model.utils.CustomerService;
import no.unit.nva.publication.model.utils.CustomerSummary;
import org.junit.jupiter.api.Test;

class CuratingInstitutionsUtilTest {

    @Test
    void shouldReturnCuratingInstitutionForContributorAffiliatedWithCustomer() {
        var organization = randomUri();
        var entityDescription = PublicationGenerator.fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES)
                                    .getEntityDescription()
                                    .copy()
                                    .withContributors(List.of(contributorWithOrganization(organization)))
                                    .build();
        var topLevelOrg = randomUri();
        var util = mock(CristinUnitsUtil.class);
        var customerService = mock(CustomerService.class);

        entityDescription.getContributors().forEach(contributor -> mockTopLevelOrg(contributor, topLevelOrg, util));
        when(customerService.fetchCustomers()).thenReturn(new CustomerList(List.of(new CustomerSummary(randomUri(),
                                                                                                       topLevelOrg))));

        var list =
            new CuratingInstitutionsUtil(mock(UriRetriever.class), customerService).getCuratingInstitutionsCached(entityDescription,
                                                                                                   util);

        assertEquals(topLevelOrg, list.stream().findFirst().orElseThrow().id());
    }

    private Contributor contributorWithOrganization(URI organization) {
        return new Contributor(null, List.of(Organization.fromUri(organization)), null, 0, true);
    }

    private void mockTopLevelOrg(Contributor contributor, URI topLevelOrg, CristinUnitsUtil util) {
        contributor.getAffiliations()
            .stream()
            .filter(Organization.class::isInstance)
            .map(Organization.class::cast)
            .map(Organization::getId)
            .forEach(id -> when(util.getTopLevel(id)).thenReturn(topLevelOrg));
    }
}
