package no.unit.nva.publication.service.impl;

import static no.unit.nva.model.testing.EntityDescriptionBuilder.randomEntityDescription;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.service.impl.ApprovalAssignmentServiceForImportCandidateFiles.AssignmentServiceStatus.APPROVAL_NEEDED;
import static no.unit.nva.publication.service.impl.ApprovalAssignmentServiceForImportCandidateFiles.AssignmentServiceStatus.NO_APPROVAL_NEEDED;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
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
        var resource = createResourceWithoutCustomers();

        var exception = assertThrows(ApprovalAssignmentException.class,
                                     () -> service.determineCustomerResponsibleForApproval(resource, List.of()));

        assertEquals("No customers for import candidate " + resource.getIdentifier(), exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUnableToFetchCustomer() throws NotFoundException {
        var customerId = randomUri();
        when(identityServiceClient.getCustomerById(customerId)).thenThrow(RuntimeException.class);
        var resource = createResource();

        var exception = assertThrows(ApprovalAssignmentException.class,
                                     () -> service.determineCustomerResponsibleForApproval(resource,
                                                                                           List.of(customerId)));

        assertEquals("Could not fetch customer with id " + customerId, exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNoContributorForCustomerExist() throws Exception {
        var customer = new CustomerSetup();
        mockCustomer(customer);

        var resource = createResource(
            createContributorContributor(createCristinId(), true, 1));

        assertThrows(ApprovalAssignmentException.class,
                     () -> service.determineCustomerResponsibleForApproval(resource,
                                                                           List.of(customer.customerId)));
    }

    @Test
    void shouldThrowExceptionWhenNoContributors() throws Exception {
        var customer = new CustomerSetup();
        mockCustomer(customer);

        var resource = resourceWithoutContributors();

        assertThrows(ApprovalAssignmentException.class,
                     () -> service.determineCustomerResponsibleForApproval(resource,
                                                                           List.of(customer.customerId)));
    }

    @Test
    void shouldReturnNoApprovalNeededWhenCustomerAllowsAutoPublishing() throws Exception {
        var customerId = randomUri();
        mockCustomer(customerId, randomUri(), true);
        var resource = createResource();

        var result = service.determineCustomerResponsibleForApproval(resource, List.of(customerId));

        assertEquals(NO_APPROVAL_NEEDED, result.getStatus());
        assertTrue(result.getReason().contains("allows auto publishing"));
    }

    @Test
    void shouldReturnCustomerFoundWhenCustomerIsFound() throws Exception {
        var customer = new CustomerSetup();
        mockCustomer(customer);

        var resource = createResource(
            createContributorContributor(customer.cristinId, true, 1));

        var associatedCustomers = List.of(customer.customerId);
        var result = service.determineCustomerResponsibleForApproval(resource, associatedCustomers).getStatus();

        assertEquals(APPROVAL_NEEDED, result);
    }

    @Test
    void shouldReturnCorrespondenceContributorCustomerWhenMultipleContributorsExist() throws Exception {
        var correspondenceCustomer = new CustomerSetup();
        var nonCorrespondenceCustomer = new CustomerSetup();

        mockCustomer(correspondenceCustomer);
        mockCustomer(nonCorrespondenceCustomer);

        var resource = createResource(
            createContributorContributor(nonCorrespondenceCustomer.cristinId, false, 1),
            createContributorContributor(correspondenceCustomer.cristinId, true, 2));

        var associatedCustomers = List.of(nonCorrespondenceCustomer.customerId, correspondenceCustomer.customerId);
        var customerDto = service.determineCustomerResponsibleForApproval(resource,
                                                                          associatedCustomers).getCustomer();

        assertEquals(correspondenceCustomer.customerId, customerDto.id());
    }

    @Test
    void shouldReturnLowestSequenceContributorCustomerWhenNoCorrespondenceContributor() throws Exception {
        var lowestSequenceCustomer = new CustomerSetup();
        var otherCustomer = new CustomerSetup();

        mockCustomer(lowestSequenceCustomer);
        mockCustomer(otherCustomer);

        var associatedCustomers = List.of(otherCustomer.customerId, lowestSequenceCustomer.customerId);
        var resource = createResource(
            createContributorContributor(otherCustomer.cristinId, false, 2),
            createContributorContributor(lowestSequenceCustomer.cristinId, false, 1));

        var customerDto = service.determineCustomerResponsibleForApproval(resource, associatedCustomers).getCustomer();

        assertEquals(lowestSequenceCustomer.customerId, customerDto.id());
    }

    @Test
    void shouldReturnFirstContributorCustomerWhenNoCorrespondenceContributorAndNoSequenceNumbers() throws Exception {
        var firstCustomer = new CustomerSetup();
        var secondCustomer = new CustomerSetup();

        mockCustomer(firstCustomer);
        mockCustomer(secondCustomer);

        var associatedCustomers = List.of(firstCustomer.customerId, secondCustomer.customerId);
        var resource = createResource(
            createNonCorrespondenceContributorWithoutSequence(firstCustomer.cristinId, false),
            createNonCorrespondenceContributorWithoutSequence(secondCustomer.cristinId, false));

        var customerDto = service.determineCustomerResponsibleForApproval(resource, associatedCustomers);

        assertEquals(firstCustomer.customerId, customerDto.getCustomer().id());
    }

    @Test
    void shouldPrioritizeCorrespondenceContributorCustomerEvenWithoutSequenceNumber() throws Exception {
        var correspondenceCustomer = new CustomerSetup();
        var otherCustomer = new CustomerSetup();

        mockCustomer(correspondenceCustomer);
        mockCustomer(otherCustomer);

        var resource = createResource(
            createContributorContributor(otherCustomer.cristinId, false, 1),
            createNonCorrespondenceContributorWithoutSequence(correspondenceCustomer.cristinId, true));

        var associatedCustomers = List.of(otherCustomer.customerId, correspondenceCustomer.customerId);
        var customerDto = service.determineCustomerResponsibleForApproval(resource,
                                                                          associatedCustomers).getCustomer();

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

        var associatedCustomers = associatedCustomers(lowestSequenceCorrespondenceCustomer, 
                                                      higherSequenceCorrespondenceCustomer, 
                                                      nonCorrespondenceCustomer);
        var resource = createResource(
            createContributorContributor(nonCorrespondenceCustomer.cristinId, false, 1),
            createContributorContributor(higherSequenceCorrespondenceCustomer.cristinId, true, 3),
            createContributorContributor(lowestSequenceCorrespondenceCustomer.cristinId, true, 2)
        );

        var customerDto = service.determineCustomerResponsibleForApproval(resource, associatedCustomers).getCustomer();

        assertEquals(lowestSequenceCorrespondenceCustomer.customerId, customerDto.id());
    }
    
    private static List<URI> associatedCustomers(CustomerSetup lowestSequenceCorrespondenceCustomer,
                                                 CustomerSetup higherSequenceCorrespondenceCustomer,
                                                 CustomerSetup nonCorrespondenceCustomer) {
        return List.of(
            lowestSequenceCorrespondenceCustomer.customerId,
            higherSequenceCorrespondenceCustomer.customerId,
            nonCorrespondenceCustomer.customerId
        );
    }

    private static Contributor createNonCorrespondenceContributorWithoutSequence(URI cristinId,
                                                                                 boolean correspondingAuthor) {
        return new Contributor(new Identity.Builder().build(),
                                     List.of(Organization.fromUri(cristinId)),
                                     new RoleType(Role.CREATOR), null, correspondingAuthor);
    }

    private static CustomerDto createCustomerDto(URI customerId, URI cristinId, boolean allowsPublishing) {
        return new CustomerDto(customerId, UUID.randomUUID(), randomString(), randomString(), randomString(), cristinId,
                               PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY.getValue(), randomBoolean(),
                               randomBoolean(), randomBoolean(), Collections.emptyList(),
                               new CustomerDto.RightsRetentionStrategy(randomString(), RandomDataGenerator.randomUri()),
                               allowsPublishing);
    }

    private static Contributor createContributorContributor(URI cristinId, boolean isCorrespondence, int sequence) {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder().build())
                   .withAffiliations(List.of(Organization.fromUri(cristinId)))
                   .withRole(new RoleType(Role.CREATOR))
                   .withSequence(sequence)
                   .withCorrespondingAuthor(isCorrespondence)
                   .build();
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

    private Resource createResource() {
        return Resource.builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withEntityDescription(randomEntityDescription(JournalArticle.class))
                   .build();
    }

    private Resource createResourceWithoutCustomers() {
        return Resource.builder()
                   .withIdentifier(SortableIdentifier.next())
                   .build();
    }

    private Resource createResource(Contributor... contributors) {
        var entityDescription = randomEntityDescription(JournalArticle.class)
                                    .copy()
                                    .withContributors(Arrays.asList(contributors))
                                    .build();
        return Resource.builder()
                   .withEntityDescription(entityDescription)
                   .build();
    }

    private Resource resourceWithoutContributors() {
        var resource = createResource();
        resource.getEntityDescription().setContributors(Collections.emptyList());
        return resource;
    }

    private static class CustomerSetup {

        final URI customerId = randomUri();
        final URI cristinId = createCristinId();
    }
}