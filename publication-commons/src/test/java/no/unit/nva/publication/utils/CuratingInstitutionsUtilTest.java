package no.unit.nva.publication.utils;

import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.utils.CuratingInstitutionsUtil;
import no.unit.nva.publication.model.utils.CustomerList;
import no.unit.nva.publication.model.utils.CustomerService;
import no.unit.nva.publication.model.utils.CustomerSummary;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

class CuratingInstitutionsUtilTest {

    protected static final URI TOP_LEVEL_ORG = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");

    @Test
    void shouldReturnCuratingInstitutionForContributorAffiliatedWithCustomer() {
        var organization = randomUri();
        var entityDescription = PublicationGenerator.fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES)
                                    .getEntityDescription()
                                    .copy()
                                    .withContributors(List.of(contributorWithOrganization(organization)))
                                    .build();
        var util = mock(CristinUnitsUtil.class);
        var customerService = mock(CustomerService.class);

        entityDescription.getContributors().forEach(contributor -> mockTopLevelOrg(contributor, TOP_LEVEL_ORG, util));
        when(customerService.fetchCustomers()).thenReturn(new CustomerList(List.of(new CustomerSummary(randomUri(),
                                                                                                       TOP_LEVEL_ORG))));

        var list =
            new CuratingInstitutionsUtil(mock(UriRetriever.class), customerService)
                .getCuratingInstitutionsCached(entityDescription, util);

        assertEquals(TOP_LEVEL_ORG, list.stream().findFirst().orElseThrow().id());
    }

    @Test
    void shouldFetchCustomerListOnlyOnceWhenInstantiatingCuratingInstitutionUtil() {
        var customerService = mock(CustomerService.class);
        when(customerService.fetchCustomers())
            .thenReturn(new CustomerList(List.of(new CustomerSummary(randomUri(), TOP_LEVEL_ORG))));


        new CuratingInstitutionsUtil(mock(UriRetriever.class), customerService);
        new CuratingInstitutionsUtil(mock(UriRetriever.class), customerService);

        verify(customerService, atMostOnce()).fetchCustomers();
    }

    @Test
    void shouldCreatingCuratingInstitutionsForContributorAtSubunit() {
        var orgId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0");
        var topLevelId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");
        var customerService = mock(CustomerService.class);
        var uriRetriever = mock(UriRetriever.class);
        when(uriRetriever.getRawContent(eq(orgId), any())).thenReturn(
            Optional.of(IoUtils.stringFromResources(Path.of("cristin-orgs/20754.6.0.0.json"))));
        when(customerService.fetchCustomers()).thenReturn(new CustomerList(List.of(new CustomerSummary(
            RandomDataGenerator.randomUri(),
            topLevelId))));
        var curatingInstitutionsUtil = new CuratingInstitutionsUtil(uriRetriever, customerService);

        var publication = randomPublication();
        publication.getEntityDescription().setContributors(List.of(contributorWithOrganization(orgId)));

        var curatingInstitutions = curatingInstitutionsUtil.getCuratingInstitutionsOnline(publication);

        assertThat(curatingInstitutions.stream().findFirst().orElseThrow().id(), is(equalTo(topLevelId)));
    }

    private Contributor contributorWithOrganization(URI organization) {
        var identity = new Identity();
        return new Contributor(identity, List.of(Organization.fromUri(organization)), null, 0, true);
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
