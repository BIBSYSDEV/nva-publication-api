package no.sikt.nva.scopus.conversion;

import static no.sikt.nva.scopus.utils.ScopusTestUtils.randomCristinOrganization;
import static no.sikt.nva.scopus.utils.ScopusTestUtils.randomCustomer;
import static no.unit.nva.model.testing.EntityDescriptionBuilder.randomContributorWithAffiliationId;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import no.scopus.generated.CitationTitleTp;
import no.scopus.generated.CitationtypeAtt;
import no.scopus.generated.OpenAccessType;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.YesnoAtt;
import no.sikt.nva.scopus.ScopusConverter;
import no.sikt.nva.scopus.conversion.ContributorExtractor.ContributorsOrganizationsWrapper;
import no.sikt.nva.scopus.conversion.files.ScopusFileConverter;
import no.sikt.nva.scopus.exception.MissingNvaContributorException;
import no.sikt.nva.scopus.utils.ScopusGenerator;
import no.unit.nva.clients.CustomerList;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.Test;

public class ScopusConverterTest {

    @Test
    void shouldReturnImportCandidateWithoutTitleWhenTitleListFromScopusXmlISEmpty() {
        var generator = new ScopusGenerator();
        generator.getDocument().getItem().getItem().getBibrecord().getHead().getCitationTitle().getTitletext().clear();
        var candidate = generateImportCandidate(generator);

        assertThat(candidate.getEntityDescription().getMainTitle(), is(nullValue()));
    }

    @Test
    void shouldReturnImportCandidateWithoutTitleWhenTitleFromXmlIsNull() {
        var generator = new ScopusGenerator();
        setNullTitle(generator);
        var candidate = generateImportCandidate(generator);

        assertThat(candidate.getEntityDescription().getMainTitle(), is(nullValue()));
    }

    @Test
    void shouldReturnImportCandidateWithPublicationDateFromOaAccessEffectiveDateWhenDateIsPresent() {
        var generator = new ScopusGenerator();
        generator.getDocument().getMeta().setOpenAccess(new OpenAccessType());
        generator.getDocument().getMeta().getOpenAccess().setOaAccessEffectiveDate("2024-10-16");

        var candidate = generateImportCandidate(generator);

        var expectedPublicationDate = new PublicationDate.Builder().withYear("2024").withMonth("10").withDay("16").build();

        assertEquals(expectedPublicationDate, candidate.getEntityDescription().getPublicationDate());
    }

    @Test
    void shouldReturnImportCandidateWithPublicationDateFromDateSortWhenOaAccessEffectiveDateIsNotISODate() {
        var generator = new ScopusGenerator();
        generator.getDocument().getMeta().setOpenAccess(new OpenAccessType());
        generator.getDocument().getMeta().getOpenAccess().setOaAccessEffectiveDate("2024");

        var candidate = generateImportCandidate(generator);

        var dateSort = generator.getDocument().getItem().getItem().getProcessInfo().getDateSort();
        var expectedPublicationDate =
            new PublicationDate.Builder().withYear(dateSort.getYear()).withMonth(dateSort.getMonth()).withDay(dateSort.getDay()).build();

        assertEquals(expectedPublicationDate, candidate.getEntityDescription().getPublicationDate());
    }

    @Test
    void shouldReturnImportCandidateFromDateSortWhenOaAccessEffectiveDateIsNotPresent() {
        var generator = new ScopusGenerator();

        var candidate = generateImportCandidate(generator);
        var dateSort = generator.getDocument().getItem().getItem().getProcessInfo().getDateSort();
        var expectedPublicationDate =
            new PublicationDate.Builder().withYear(dateSort.getYear()).withMonth(dateSort.getMonth()).withDay(dateSort.getDay()).build();

        assertEquals(expectedPublicationDate, candidate.getEntityDescription().getPublicationDate());
    }

    @Test
    void shouldThrowMissingNvaContributorExceptionWhenNoContributorsBelongToNvaCustomer() throws ApiGatewayException {
        var generator = new ScopusGenerator();
        var identityServiceClient = mock(IdentityServiceClient.class);
        var contributorExtractor = mock(ContributorExtractor.class);

        var nonNvaOrgId = randomUri();
        when(contributorExtractor.generateContributors(any()))
            .thenReturn(new ContributorsOrganizationsWrapper(
                List.of(randomContributorWithAffiliationId(nonNvaOrgId)),
                List.of(nonNvaOrgId)
            ));

        var differentOrgId = randomUri();
        when(identityServiceClient.getAllCustomers())
            .thenReturn(new CustomerList(List.of(randomCustomer(differentOrgId))));

        var converter = new ScopusConverter(
            generator.getDocument(),
            mock(PublicationChannelConnection.class),
            identityServiceClient,
            mock(ScopusFileConverter.class),
            contributorExtractor
        );

        assertThrows(MissingNvaContributorException.class, converter::generateImportCandidate);
    }

    @Test
    void shouldMapCitationTitleForCitationTypeErWhenOriginalTitleIsMissing() {
        var citationTitle = randomString();
        var scopusDocument = createScopusDocumentWithCitationTypeAndCitationTitle(citationTitle);
        var importCandidate = generateImportCandidate(scopusDocument);

        assertEquals(citationTitle, importCandidate.getEntityDescription().getMainTitle());
    }

    private static ScopusGenerator createScopusDocumentWithCitationTypeAndCitationTitle(String nonOriginalTitle) {
        var scopusGenerator = ScopusGenerator.create(CitationtypeAtt.ER);
        var title = new CitationTitleTp();
        var titletextTp = new TitletextTp();
        titletextTp.setOriginal(YesnoAtt.N);
        titletextTp.getContent().add(nonOriginalTitle);
        title.getTitletext().add(titletextTp);
        scopusGenerator.getDocument().getItem().getItem().getBibrecord().getHead().setCitationTitle(title);
        return scopusGenerator;
    }

    private static ImportCandidate generateImportCandidate(ScopusGenerator generator) {
        var identityServiceClient = mock(IdentityServiceClient.class);
        var cristinConnection = mock(CristinConnection.class);
        var contributorExtractor = mock(ContributorExtractor.class);
        var affiliationId = randomUri();
        when(contributorExtractor.generateContributors(any()))
            .thenReturn(new ContributorsOrganizationsWrapper(List.of(randomContributorWithAffiliationId(affiliationId)), List.of(affiliationId)));
        var converter = new ScopusConverter(generator.getDocument(),
                                            mock(PublicationChannelConnection.class),
                                            identityServiceClient,
                                            mock(ScopusFileConverter.class),
                                            contributorExtractor);
        when(attempt(identityServiceClient::getAllCustomers).orElseThrow())
            .thenReturn(new CustomerList(List.of(randomCustomer(affiliationId))));
        when(cristinConnection.fetchCristinOrganizationByCristinId(any()))
            .thenReturn(randomCristinOrganization());
        return converter.generateImportCandidate();
    }

    private static void setNullTitle(ScopusGenerator generator) {
        generator.getDocument().getItem().getItem().getBibrecord().getHead().getCitationTitle().getTitletext().clear();
        generator.getDocument()
            .getItem()
            .getItem()
            .getBibrecord()
            .getHead()
            .getCitationTitle()
            .getTitletext()
            .add(null);
    }
}
