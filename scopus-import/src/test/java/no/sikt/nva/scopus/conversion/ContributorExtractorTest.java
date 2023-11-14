package no.sikt.nva.scopus.conversion;

import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import no.sikt.nva.scopus.utils.CristinGenerator;
import no.sikt.nva.scopus.utils.ScopusGenerator;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContributorExtractorTest {

    public static final URI CRISTIN_ID = randomUri();
    private PiaConnection piaConnection;
    public CristinConnection cristinConnection;

    @BeforeEach
    void init() {
        piaConnection = mock(PiaConnection.class);
        cristinConnection = mock(CristinConnection.class);
    }

    @Test
    void shouldCreateContributorWithoutAffiliationWhenAuthorTpIsNorwegianContributorWithoutAffiliation() {
        var generator = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1);
        var correspondenceTps = generator.getDocument().getItem().getItem().getBibrecord().getHead().getCorrespondence();
        var authorGroupTps = generator.getDocument().getItem().getItem().getBibrecord().getHead().getAuthorGroup();
        var contributorExtractor = new ContributorExtractor(correspondenceTps, authorGroupTps, piaConnection, cristinConnection);
        var personCristinId = randomUri();
        when(piaConnection.getCristinPersonIdentifier(any())).thenReturn(Optional.of(personCristinId));
        when(cristinConnection.getCristinPersonByCristinId(personCristinId)).thenReturn(Optional.of(CristinGenerator.generateCristinPersonWithoutAffiliations(personCristinId, randomString(), randomString())));

        var cristinOrgId = randomUri();
        when(piaConnection.getCristinOrganizationIdentifier(any())).thenReturn(Optional.of(cristinOrgId));
        when(piaConnection.getCristinOrganizationIdentifier(UriWrapper.fromUri(cristinOrgId).getLastPathElement())).thenReturn(Optional.of(randomUri()));

        var contributors = contributorExtractor.generateContributors().stream().collect(SingletonCollector.collect()) ;

        assertThat(contributors.getAffiliations(), is(emptyIterable()));
    }

    @Test
    void shouldCreateContributorWithoutAffiliationWhenAuthorTpIsNorwegianContributorWithOtherNorwegianInstitutionAsAffiliation() {
        var generator = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1);
        var correspondenceTps = generator.getDocument().getItem().getItem().getBibrecord().getHead().getCorrespondence();
        var authorGroupTps = generator.getDocument().getItem().getItem().getBibrecord().getHead().getAuthorGroup();
        var contributorExtractor = new ContributorExtractor(correspondenceTps, authorGroupTps, piaConnection, cristinConnection);
        var personCristinId = randomUri();
        when(piaConnection.getCristinPersonIdentifier(any())).thenReturn(Optional.of(personCristinId));
        when(cristinConnection.getCristinPersonByCristinId(personCristinId)).thenReturn(Optional.of(CristinGenerator.generateCristinPersonWithoutAffiliations(personCristinId, randomString(), randomString())));

        var cristinOrgId = randomUri();
        when(piaConnection.getCristinOrganizationIdentifier(any())).thenReturn(Optional.of(cristinOrgId));
        when(cristinConnection.getCristinOrganizationByCristinId(cristinOrgId)).thenReturn(CristinGenerator.generateOtherCristinOrganization(cristinOrgId));

        var contributors = contributorExtractor.generateContributors().stream().collect(SingletonCollector.collect()) ;

        assertThat(contributors.getAffiliations(), is(emptyIterable()));
    }

    @Test
    void shouldCreateContributorWithAffiliationWhenAuthorTpIsNorwegianContributorWithNorwegianInstitutionAsAffiliation() {
        var generator = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1);
        var correspondenceTps = generator.getDocument().getItem().getItem().getBibrecord().getHead().getCorrespondence();
        var authorGroupTps = generator.getDocument().getItem().getItem().getBibrecord().getHead().getAuthorGroup();
        var contributorExtractor = new ContributorExtractor(correspondenceTps, authorGroupTps, piaConnection, cristinConnection);
        var personCristinId = randomUri();
        when(piaConnection.getCristinPersonIdentifier(any())).thenReturn(Optional.of(personCristinId));
        when(cristinConnection.getCristinPersonByCristinId(personCristinId)).thenReturn(Optional.of(CristinGenerator.generateCristinPersonWithoutAffiliations(personCristinId, randomString(), randomString())));

        var cristinOrgId = randomUri();
        when(piaConnection.getCristinOrganizationIdentifier(any())).thenReturn(Optional.of(cristinOrgId));
        when(cristinConnection.getCristinOrganizationByCristinId(any())).thenReturn(CristinGenerator.generateCristinOrganizationWithCountry("NO"));

        var contributors = contributorExtractor.generateContributors().stream().collect(SingletonCollector.collect()) ;

        assertThat(contributors.getAffiliations(), hasSize(1));
    }

    @Test
    void shouldCreateContributorWithoutAffiliationWhenAuthorTpIsNorwegianContributorWithoutAffiliationFromCristin() {
        var document = new ScopusGenerator().createWithNumberOfContributorsFromCollaborationTp(1);
        var correspondenceTps = document.getItem().getItem().getBibrecord().getHead().getCorrespondence();
        var authorGroupTps = document.getItem().getItem().getBibrecord().getHead().getAuthorGroup();
        var contributorExtractor = new ContributorExtractor(correspondenceTps, authorGroupTps, piaConnection, cristinConnection);
        var personCristinId = randomUri();
        when(piaConnection.getCristinPersonIdentifier(any())).thenReturn(Optional.of(personCristinId));
        when(cristinConnection.getCristinPersonByCristinId(personCristinId)).thenReturn(Optional.of(CristinGenerator.generateCristinPersonWithoutAffiliations(personCristinId, randomString(), randomString())));

        var cristinOrgId = randomUri();
        when(piaConnection.getCristinOrganizationIdentifier(any())).thenReturn(Optional.of(cristinOrgId));
        when(cristinConnection.getCristinOrganizationByCristinId(cristinOrgId)).thenReturn(null);

        var contributors = contributorExtractor.generateContributors().stream().collect(SingletonCollector.collect()) ;

        assertThat(contributors.getAffiliations(), is(emptyIterable()));
    }

    @Test
    void shouldCreateContributorWithAffiliationFromCristinWhenCreatingContributorFromCollaborationTp() {
        var document = new ScopusGenerator().createWithNumberOfContributorsFromCollaborationTp(1);
        var correspondenceTps = document.getItem().getItem().getBibrecord().getHead().getCorrespondence();
        var authorGroupTps = document.getItem().getItem().getBibrecord().getHead().getAuthorGroup();
        var contributorExtractor = new ContributorExtractor(correspondenceTps, authorGroupTps, piaConnection, cristinConnection);
        when(piaConnection.getCristinOrganizationIdentifier(any())).thenReturn(Optional.of(randomUri()));
        when(cristinConnection.getCristinOrganizationByCristinId(any())).thenReturn(CristinGenerator.generateCristinOrganization(CRISTIN_ID));

        var contributors = contributorExtractor.generateContributors().stream().collect(SingletonCollector.collect()) ;

        assertThat(contributors.getAffiliations().get(0).getId(), is(equalTo(CRISTIN_ID)));
    }
}
