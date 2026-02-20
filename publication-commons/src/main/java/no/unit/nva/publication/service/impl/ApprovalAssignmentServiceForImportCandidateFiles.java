package no.unit.nva.publication.service.impl;

import static java.util.function.Predicate.not;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.publication.model.business.Resource;

public class ApprovalAssignmentServiceForImportCandidateFiles {

    private static final String FETCH_CUSTOMER_EXCEPTION_MESSAGE = "Could not fetch customer with id %s";
    private static final String NO_CUSTOMERS_EXCEPTION_MESSAGE = "No customers for import candidate %s";
    private static final String NO_CONTRIBUTORS_EXCEPTION_MESSAGE = "Import candidate is missing contributors";
    private static final String NO_CONTRIBUTOR_MESSAGE = "No contributor matching customer found to create approval";
    private static final String URI_PATH_SEPARATOR = "/";
    private static final String CRISTIN_ID_SEPARATOR = ".";
    private static final int START_OF_STRING = 0;
    private static final int REMOVE_SLASH = 1;
    private final IdentityServiceClient identityServiceClient;

    public ApprovalAssignmentServiceForImportCandidateFiles(IdentityServiceClient identityServiceClient) {
        this.identityServiceClient = identityServiceClient;
    }

    /**
     * Determines which customer (if any) is responsible for approving ImportCandidates files.
     * The order of preference:
     * <ol>
     *     <li>All customers allow auto-publish of Scopus files: no approval needed.</li>
     *     <li>A customer whose publication workflow explicitly requires file approval
     *         ({@code REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES} or
     *         {@code REGISTRATOR_PUBLISHES_METADATA_ONLY}) and who does not allow auto-publish:
     *         approval needed, assigned to the matching contributor with the highest priority
     *         (corresponding author first, then lowest sequence number).</li>
     *     <li>Any customer that does not allow auto-publish of Scopus files (but whose workflow does not
     *         require explicit file approval): no approval needed, that customer becomes the file owner.</li>
     * </ol>
     *
     * @param resource            The import candidate to analyze.
     * @param associatedCustomers The customer URIs associated with the import candidate.
     * @return {@link AssignmentServiceResult} indicating whether approval is needed and which customer is responsible.
     * @throws ApprovalAssignmentException if a customer URI cannot be resolved, no customers are associated,
     *                                     no contributors are present, or no matching contributor is found.
     */
    public AssignmentServiceResult determineCustomerResponsibleForApproval(Resource resource,
                                                                           Collection<URI> associatedCustomers)
        throws ApprovalAssignmentException {
        validateImportCandidateForCustomersPresence(resource, associatedCustomers);
        var customers = fetchAllAssociatedCustomers(associatedCustomers);

        return anyCustomerRequiresApproval(customers)
                   ? resolveApprovalAssignment(resource, customers)
                   : AssignmentServiceResult.noApprovalNeeded(getCustomerAllowingApproval(customers));
    }

    private static boolean anyCustomerRequiresApproval(Collection<CustomerDto> customers) {
        return customers.stream().anyMatch(not(CustomerDto::autoPublishScopusImportFiles));
    }

    private AssignmentServiceResult resolveApprovalAssignment(Resource resource,
                                                              Collection<CustomerDto> customers)
        throws ApprovalAssignmentException {
        var customerMap = customerByTopLevelInstitutionIdentifierMap(customers);
        var customer = findCustomerRequiringApprovalByScopusConfigAndPublishingWorkflow(resource, customerMap);

        return customer.isPresent()
                   ? AssignmentServiceResult.customerFound(customer.get())
                   : resolveApprovalByCustomerWithScopusConfig(resource, customerMap);
    }

    private AssignmentServiceResult resolveApprovalByCustomerWithScopusConfig(Resource resource,
                                                                              Map<String, CustomerDto> customerMap)
        throws ApprovalAssignmentException {
        return getCustomerRequiringApprovalOfScopusFile(resource, customerMap)
                   .map(pair -> AssignmentServiceResult.noApprovalNeeded(pair.customerDto()))
                   .orElseThrow(() -> new ApprovalAssignmentException(NO_CONTRIBUTOR_MESSAGE));
    }

    private static Map<String, CustomerDto> customerByTopLevelInstitutionIdentifierMap(Collection<CustomerDto> customers) {
        return customers.stream()
                   .distinct()
                   .collect(Collectors.toMap(
                       ApprovalAssignmentServiceForImportCandidateFiles::extractTopLevelCristinInstitutionIdentifier,
                       customerDto -> customerDto));
    }

    private static void validateImportCandidateForCustomersPresence(Resource resource, Collection<URI> associatedCustomers)
        throws ApprovalAssignmentException {
        if (associatedCustomers.isEmpty()) {
            var message = NO_CUSTOMERS_EXCEPTION_MESSAGE.formatted(resource.getIdentifier());
            throw new ApprovalAssignmentException(message);
        }
        if (getContributors(resource).isEmpty()) {
            throw new ApprovalAssignmentException(NO_CONTRIBUTORS_EXCEPTION_MESSAGE);
        }
    }

    private static Collection<Contributor> getContributors(Resource resource) {
        return Optional.ofNullable(resource)
                   .map(Resource::getEntityDescription)
                   .map(EntityDescription::getContributors)
                   .orElse(Collections.emptyList());
    }

    private static String extractTopLevelCristinInstitutionIdentifier(CustomerDto customerDto) {
        return extractTopLevelOrganizationPart(customerDto.cristinId());
    }

    private static String extractTopLevelOrganizationPart(URI organizationId) {
        if (organizationId.getPath().isBlank()) {
            throw new IllegalArgumentException(String.format("Not a valid URI for Organization: %s", organizationId));
        }
        var string = organizationId.toString();
        var first = string.substring(string.lastIndexOf(URI_PATH_SEPARATOR) + REMOVE_SLASH);
        if (!first.contains(CRISTIN_ID_SEPARATOR)) {
            throw new IllegalArgumentException(String.format("Not a valid URI for Organization: %s", organizationId));
        }
        return first.substring(START_OF_STRING, first.indexOf(CRISTIN_ID_SEPARATOR));
    }

    private static Comparator<Contributor> compareByCorrespondingAuthorAndSequence() {
        return Comparator.comparing(Contributor::isCorrespondingAuthor, Comparator.reverseOrder())
                   .thenComparing(Contributor::getSequence, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private CustomerDto getCustomerAllowingApproval(Collection<CustomerDto> customers) {
        return customers.stream().filter(CustomerDto::autoPublishScopusImportFiles).findFirst().orElseThrow();
    }

    private Optional<CustomerContributorPair> getCustomerRequiringApprovalOfScopusFile(Resource resource,
                                                                           Map<String, CustomerDto> customerMap) {
        return getContributors(resource)
                   .stream()
                   .sorted(compareByCorrespondingAuthorAndSequence())
                   .map(contributor -> findMatchingCustomer(contributor, customerMap))
                   .flatMap(Optional::stream)
                   .filter(ApprovalAssignmentServiceForImportCandidateFiles::customerRequiresApprovalOfFilesFromScopus)
                   .findFirst();
    }

    private Optional<CustomerContributorPair> findCustomerRequiringApprovalByScopusConfigAndPublishingWorkflow(Resource resource, Map<String, CustomerDto> customerMap) {
        return getContributors(resource)
                   .stream()
                   .sorted(compareByCorrespondingAuthorAndSequence())
                   .map(contributor -> findMatchingCustomer(contributor, customerMap))
                   .flatMap(Optional::stream)
                   .filter(ApprovalAssignmentServiceForImportCandidateFiles::customerRequiresApprovalOfFilesFromScopus)
                   .filter(ApprovalAssignmentServiceForImportCandidateFiles::customerWorkflowRequiresApprovalOfFiles)
                   .findFirst();
    }

    private static boolean customerWorkflowRequiresApprovalOfFiles(CustomerContributorPair customerContributorPair) {
        var workflow = customerContributorPair.customerDto().publicationWorkflow();
        return REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES.getValue().equals(workflow)
               || REGISTRATOR_PUBLISHES_METADATA_ONLY.getValue().equals(workflow);
    }

    private static boolean customerRequiresApprovalOfFilesFromScopus(CustomerContributorPair customerContributorPair) {
        return !customerContributorPair.customerDto().autoPublishScopusImportFiles();
    }

    private Optional<CustomerContributorPair> findMatchingCustomer(Contributor contributor,
                                                                   Map<String, CustomerDto> customerMap) {
        return contributor.getAffiliations().stream()
                   .filter(Organization.class::isInstance)
                   .map(Organization.class::cast)
                   .map(Organization::getId)
                   .filter(Objects::nonNull)
                   .map(ApprovalAssignmentServiceForImportCandidateFiles::extractTopLevelOrganizationPart)
                   .map(customerMap::get)
                   .filter(Objects::nonNull)
                   .findFirst()
                   .map(customer -> new CustomerContributorPair(customer, contributor));
    }

    private Collection<CustomerDto> fetchAllAssociatedCustomers(Collection<URI> associatedCustomers)
        throws ApprovalAssignmentException {
        var customers = new ArrayList<CustomerDto>();
        for (URI customerUri : associatedCustomers) {
            var customer = getCustomerById(customerUri);
            customers.add(customer);
        }
        return customers;
    }

    private CustomerDto getCustomerById(URI customerId) throws ApprovalAssignmentException {
        return attempt(() -> identityServiceClient.getCustomerById(customerId)).orElseThrow(
            failure -> new ApprovalAssignmentException(FETCH_CUSTOMER_EXCEPTION_MESSAGE.formatted(customerId)));
    }

    public enum AssignmentServiceStatus {
        NO_APPROVAL_NEEDED, APPROVAL_NEEDED
    }

    public static class ApprovalAssignmentException extends Exception {

        public ApprovalAssignmentException(String message) {
            super(message);
        }
    }

    public static final class AssignmentServiceResult {

        private static final String FOUND_REASON_TEMPLATE =
            "Customer %s requires approval based on contributor %s because of corresponding author %s and sequence %s";
        private final AssignmentServiceStatus status;
        private final String reason;
        private final CustomerDto customer;

        private AssignmentServiceResult(AssignmentServiceStatus status, String reason, CustomerDto customerDto) {
            this.status = status;
            this.reason = reason;
            this.customer = customerDto;
        }

        public static AssignmentServiceResult noApprovalNeeded(CustomerDto customerDto) {
            var reason = "Customer %s allows auto publishing".formatted(customerDto);
            return new AssignmentServiceResult(AssignmentServiceStatus.NO_APPROVAL_NEEDED, reason, customerDto);
        }

        public static AssignmentServiceResult customerFound(CustomerContributorPair customerContributorPair) {
            Objects.requireNonNull(customerContributorPair, "Customer required when status is CUSTOMER_FOUND");
            var reason = createReason(customerContributorPair);
            return new AssignmentServiceResult(AssignmentServiceStatus.APPROVAL_NEEDED, reason,
                                               customerContributorPair.customerDto());
        }

        private static String createReason(CustomerContributorPair customerContributorPair) {
            return FOUND_REASON_TEMPLATE.formatted(customerContributorPair.customerDto().cristinId(),
                                                   getContributorId(customerContributorPair.contributor()),
                                                   isCorrespondingAuthor(customerContributorPair.contributor()),
                                                   getSequence(customerContributorPair.contributor()));
        }

        private static URI getContributorId(Contributor contributor) {
            return Optional.ofNullable(contributor).map(Contributor::getIdentity).map(Identity::getId).orElse(null);
        }

        private static boolean isCorrespondingAuthor(Contributor contributor) {
            return Optional.ofNullable(contributor).map(Contributor::isCorrespondingAuthor).orElse(false);
        }

        private static Integer getSequence(Contributor contributor) {
            return Optional.ofNullable(contributor).map(Contributor::getSequence).orElse(null);
        }

        public CustomerDto getCustomer() {
            return customer;
        }

        public AssignmentServiceStatus getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }
    }

    public record CustomerContributorPair(CustomerDto customerDto, Contributor contributor) {}
}