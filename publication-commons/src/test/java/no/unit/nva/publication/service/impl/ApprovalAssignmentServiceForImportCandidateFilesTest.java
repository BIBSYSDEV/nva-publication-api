package no.unit.nva.publication.service.impl;

import static no.unit.nva.model.testing.EntityDescriptionBuilder.randomReference;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.service.impl.ApprovalAssignmentServiceForImportCandidateFiles.AssignmentServiceStatus.APPROVAL_NEEDED;
import static no.unit.nva.publication.service.impl.ApprovalAssignmentServiceForImportCandidateFiles.AssignmentServiceStatus.NO_APPROVAL_NEEDED;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.importcandidate.ImportContributor;
import no.unit.nva.importcandidate.ImportEntityDescription;
import no.unit.nva.importcandidate.ImportOrganization;
import no.unit.nva.importcandidate.ImportStatusFactory;
import no.unit.nva.importcandidate.ScopusAffiliation;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.service.impl.ApprovalAssignmentServiceForImportCandidateFiles.ApprovalAssignmentException;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApprovalAssignmentServiceForImportCandidateFilesTest {

    private ApprovalAssignmentServiceForImportCandidateFiles service;
    private IdentityServiceClient identityServiceClient;

    @BeforeEach
    void setUp() {
        identityServiceClient = mock(IdentityServiceClient.class);
        service = new ApprovalAssignmentServiceForImportCandidateFiles(identityServiceClient);
    }

    @Test
    void shouldThrowExceptionWhenAssociatedCustomersAreEmpty() {
        var importCandidate = createImportCandidateWithoutCustomers();

        var exception = assertThrows(ApprovalAssignmentException.class,
                                     () -> service.determineCustomerResponsibleForApproval(importCandidate));

        assertEquals("No customers for import candidate " + importCandidate.getIdentifier(), exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUnableToFetchCustomer() throws NotFoundException {
        var customerId = randomUri();
        when(identityServiceClient.getCustomerById(customerId)).thenThrow(RuntimeException.class);
        var importCandidate = createImportCandidate(customerId);

        var exception = assertThrows(ApprovalAssignmentException.class,
                                     () -> service.determineCustomerResponsibleForApproval(importCandidate));

        assertEquals("Could not fetch customer with id " + customerId, exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNoContributorForCustomerExist() throws Exception {
        var customer = new CustomerSetup();
        mockCustomer(customer);

        var importCandidate = createImportCandidate(
            List.of(customer.customerId),
            createImportContributor(createCristinId(), true, 1));

        assertThrows(ApprovalAssignmentException.class,
                     () -> service.determineCustomerResponsibleForApproval(importCandidate));
    }

    @Test
    void shouldThrowExceptionWhenNoContributors() throws Exception {
        var customer = new CustomerSetup();
        mockCustomer(customer);

        var importCandidate = createImportCandidate(List.of(customer.customerId));

        assertThrows(ApprovalAssignmentException.class,
                     () -> service.determineCustomerResponsibleForApproval(importCandidate));
    }

    @Test
    void shouldReturnNoApprovalNeededWhenCustomerAllowsAutoPublishing() throws Exception {
        var customerId = randomUri();
        mockCustomer(customerId, randomUri(), true);
        var importCandidate = createImportCandidate(customerId);

        var result = service.determineCustomerResponsibleForApproval(importCandidate);

        assertEquals(NO_APPROVAL_NEEDED, result.getStatus());
        assertTrue(result.getReason().contains("allows auto publishing"));
    }

    @Test
    void shouldReturnCustomerFoundWhenCustomerIsFound() throws Exception {
        var customer = new CustomerSetup();
        mockCustomer(customer);

        var importCandidate = createImportCandidate(
            List.of(customer.customerId),
            createImportContributor(customer.cristinId, true, 1));

        var result = service.determineCustomerResponsibleForApproval(importCandidate).getStatus();

        assertEquals(APPROVAL_NEEDED, result);
    }

    @Test
    void shouldReturnCorrespondenceContributorCustomerWhenMultipleContributorsExist() throws Exception {
        var correspondenceCustomer = new CustomerSetup();
        var nonCorrespondenceCustomer = new CustomerSetup();

        mockCustomer(correspondenceCustomer);
        mockCustomer(nonCorrespondenceCustomer);

        var importCandidate = createImportCandidate(
            List.of(nonCorrespondenceCustomer.customerId, correspondenceCustomer.customerId),
            createImportContributor(nonCorrespondenceCustomer.cristinId, false, 1),
            createImportContributor(correspondenceCustomer.cristinId, true, 2));

        var customerDto = service.determineCustomerResponsibleForApproval(importCandidate).getCustomer();

        assertEquals(correspondenceCustomer.customerId, customerDto.id());
    }

    @Test
    void shouldReturnLowestSequenceContributorCustomerWhenNoCorrespondenceContributor() throws Exception {
        var lowestSequenceCustomer = new CustomerSetup();
        var otherCustomer = new CustomerSetup();

        mockCustomer(lowestSequenceCustomer);
        mockCustomer(otherCustomer);

        var importCandidate = createImportCandidate(
            List.of(otherCustomer.customerId, lowestSequenceCustomer.customerId),
            createImportContributor(otherCustomer.cristinId, false, 2),
            createImportContributor(lowestSequenceCustomer.cristinId, false, 1));

        var customerDto = service.determineCustomerResponsibleForApproval(importCandidate).getCustomer();

        assertEquals(lowestSequenceCustomer.customerId, customerDto.id());
    }

    @Test
    void shouldReturnFirstContributorCustomerWhenNoCorrespondenceContributorAndNoSequenceNumbers() throws Exception {
        var firstCustomer = new CustomerSetup();
        var secondCustomer = new CustomerSetup();

        mockCustomer(firstCustomer);
        mockCustomer(secondCustomer);

        var importCandidate = createImportCandidate(
            List.of(firstCustomer.customerId, secondCustomer.customerId),
            createNonCorrespondenceContributorWithoutSequence(firstCustomer.cristinId, false),
            createNonCorrespondenceContributorWithoutSequence(secondCustomer.cristinId, false));

        var customerDto = service.determineCustomerResponsibleForApproval(importCandidate);

        assertEquals(firstCustomer.customerId, customerDto.getCustomer().id());
    }

    @Test
    void shouldPrioritizeCorrespondenceContributorCustomerEvenWithoutSequenceNumber() throws Exception {
        var correspondenceCustomer = new CustomerSetup();
        var otherCustomer = new CustomerSetup();

        mockCustomer(correspondenceCustomer);
        mockCustomer(otherCustomer);

        var importCandidate = createImportCandidate(
            List.of(otherCustomer.customerId, correspondenceCustomer.customerId),
            createImportContributor(otherCustomer.cristinId, false, 1),
            createNonCorrespondenceContributorWithoutSequence(correspondenceCustomer.cristinId, true));

        var customerDto = service.determineCustomerResponsibleForApproval(importCandidate).getCustomer();

        assertEquals(correspondenceCustomer.customerId, customerDto.id());
    }

    @Test
    void shouldReturnLowestSequenceCorrespondenceContributorCustomerWhenMultipleCorrespondenceContributorsExist()
        throws Exception {
        var lowestSequenceCorrespondenceCustomer = new CustomerSetup();
        var higherSequenceCorrespondenceCustomer = new CustomerSetup();
        var nonCorrespondenceCustomer = new CustomerSetup();

        mockCustomer(lowestSequenceCorrespondenceCustomer);
        mockCustomer(higherSequenceCorrespondenceCustomer);
        mockCustomer(nonCorrespondenceCustomer);

        var importCandidate = createImportCandidate(
            List.of(
                lowestSequenceCorrespondenceCustomer.customerId,
                higherSequenceCorrespondenceCustomer.customerId,
                nonCorrespondenceCustomer.customerId
            ),
            createImportContributor(nonCorrespondenceCustomer.cristinId, false, 1),
            createImportContributor(higherSequenceCorrespondenceCustomer.cristinId, true, 3),
            createImportContributor(lowestSequenceCorrespondenceCustomer.cristinId, true, 2)
        );

        var customerDto = service.determineCustomerResponsibleForApproval(importCandidate).getCustomer();

        assertEquals(lowestSequenceCorrespondenceCustomer.customerId, customerDto.id());
    }

    private static ImportContributor createNonCorrespondenceContributorWithoutSequence(URI cristinId,
                                                                                 boolean correspondingAuthor) {
        return new ImportContributor(new Identity.Builder().build(),
                                     List.of(new ImportOrganization(Organization.fromUri(cristinId),
                                                                    ScopusAffiliation.emptyAffiliation())),
                                     new RoleType(
                                         Role.CREATOR), randomInteger(), correspondingAuthor);
    }

    private static CustomerDto createCustomerDto(URI customerId, URI cristinId, boolean allowsPublishing) {
        return new CustomerDto(customerId, UUID.randomUUID(), randomString(), randomString(), randomString(), cristinId,
                               PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY.getValue(), randomBoolean(),
                               randomBoolean(), randomBoolean(), Collections.emptyList(),
                               new CustomerDto.RightsRetentionStrategy(randomString(), RandomDataGenerator.randomUri()),
                               allowsPublishing);
    }

    private static ImportContributor createImportContributor(URI cristinId, boolean isCorrespondence, int sequence) {
        return new ImportContributor(new Identity.Builder().build(),
                                     List.of(new ImportOrganization(Organization.fromUri(cristinId),
                                                                    ScopusAffiliation.emptyAffiliation())),
                                     new RoleType(
                                         Role.CREATOR), sequence, isCorrespondence);
    }

    private static URI createCristinId() {
        return UriWrapper.fromUri(randomUri())
                   .addChild("cristin")
                   .addChild("organization")
                   .addChild("%s.%s.%s".formatted(randomString(), randomString(), randomString()))
                   .getUri();
    }

    private void mockCustomer(CustomerSetup setup) throws NotFoundException {
        mockCustomer(setup.customerId, setup.cristinId, false);
    }

    private void mockCustomer(URI customerId, URI cristinId, boolean allowsPublishing) throws NotFoundException {
        when(identityServiceClient.getCustomerById(customerId)).thenReturn(
            createCustomerDto(customerId, cristinId, allowsPublishing));
    }

    private ImportCandidate createImportCandidate(URI customerId) {
        return new ImportCandidate.Builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .withAssociatedCustomers(List.of(customerId))
                   .withEntityDescription(randomImportEntityDescription())
                   .build();
    }

    private ImportCandidate createImportCandidateWithoutCustomers() {
        return new ImportCandidate.Builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .build();
    }

    private ImportEntityDescription randomImportEntityDescription() {
        return new ImportEntityDescription(randomString(), RandomDataGenerator.randomUri(),
                                           new PublicationDate.Builder().withYear("2020").build(),
                                           List.of(), randomString(), Map.of(), List.of(), randomString(),
                                           randomReference(JournalArticle.class));
    }

    private ImportEntityDescription randomImportEntityDescriptionWithContributors(Collection<ImportContributor> contributors) {
        return new ImportEntityDescription(randomString(), RandomDataGenerator.randomUri(),
                                           new PublicationDate.Builder().withYear("2020").build(),
                                           contributors, randomString(), Map.of(), List.of(), randomString(),
                                           randomReference(JournalArticle.class));
    }

    private ImportCandidate createImportCandidate(List<URI> customers, ImportContributor... contributors) {
        var entityDescription = randomImportEntityDescriptionWithContributors(Arrays.asList(contributors));
        return new ImportCandidate.Builder().withImportStatus(ImportStatusFactory.createNotImported())
                   .withAssociatedCustomers(customers)
                   .withEntityDescription(entityDescription)
                   .build();
    }

    private static class CustomerSetup {

        final URI customerId = randomUri();
        final URI cristinId = createCristinId();
    }
}