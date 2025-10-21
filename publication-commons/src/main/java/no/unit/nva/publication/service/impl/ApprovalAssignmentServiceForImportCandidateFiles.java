package no.unit.nva.publication.service.impl;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;

public class ApprovalAssignmentServiceForImportCandidateFiles {

    private static final String FETCH_CUSTOMER_EXCEPTION_MESSAGE = "Could not fetch customer with id %s";
    private static final String NO_CUSTOMERS_EXCEPTION_MESSAGE = "No customers for import candidate %s";
    private static final String NO_CONTRIBUTOR_MESSAGE = "No contributor to create approval";
    private static final String URI_PATH_SEPARATOR = "/";
    private static final String CRISTIN_ID_SEPARATOR = ".";
    private static final int START_OF_STRING = 0;
    private static final int REMOVE_SLASH = 1;
    private final IdentityServiceClient identityServiceClient;

    public ApprovalAssignmentServiceForImportCandidateFiles(IdentityServiceClient identityServiceClient) {
        this.identityServiceClient = identityServiceClient;
    }

    /**
     * This method analyzes the data for the ImportCandidate and determines which customer (if any) should approve the
     * candidate files. The order of preference:
     * <ol>
     *     <li>At least one customer has auto-publish enabled: no approval</li>
     *     <li>The customer that has the corresponding author with the lowest contributor-sequence number</li>
     *     <li>The customer that has the contributor with the lowest contributor-sequence number</li>
     * </ol>
     *
     * @param importCandidate The candidate to analyze.
     * @return {@code Optional<CustomerDto>} The customer that should approve.
     * @throws ApprovalAssignmentException if customer uri for import candidate fails to dereference.
     */
    public AssignmentServiceResult determineCustomerResponsibleForApproval(ImportCandidate importCandidate)
        throws ApprovalAssignmentException {
        validateImportCandidateForCustomersPresence(importCandidate);
        var customers = fetchAllAssociatedCustomers(importCandidate);

        if (oneOfCustomersAllowsPublishingWithoutApproval(customers)) {
            return AssignmentServiceResult.noApprovalNeeded();
        }

        return AssignmentServiceResult.customerFound(getResponsibleCustomer(importCandidate, customers));
    }

    private static Map<String, CustomerDto> customerByTopLevelInstitutionIdentifierMap(List<CustomerDto> customers) {
        return customers.stream()
                   .distinct()
                   .collect(Collectors.toMap(
                       ApprovalAssignmentServiceForImportCandidateFiles::extractTopLevelCristinInstitutionIdentifier,
                       customerDto -> customerDto));
    }

    private static void validateImportCandidateForCustomersPresence(ImportCandidate importCandidate)
        throws ApprovalAssignmentException {
        if (importCandidate.getAssociatedCustomers().isEmpty()) {
            var message = NO_CUSTOMERS_EXCEPTION_MESSAGE.formatted(importCandidate.getIdentifier());
            throw new ApprovalAssignmentException(message);
        }
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

    private static boolean oneOfCustomersAllowsPublishingWithoutApproval(Collection<CustomerDto> customers) {
        return customers.stream().anyMatch(CustomerDto::autoPublishScopusImportFiles);
    }

    private CustomerDto getResponsibleCustomer(ImportCandidate importCandidate, List<CustomerDto> customers)
        throws ApprovalAssignmentException {
        var customerMap = customerByTopLevelInstitutionIdentifierMap(customers);

        return importCandidate.getEntityDescription()
                   .getContributors()
                   .stream()
                   .sorted(compareByCorrespondingAuthorAndSequence())
                   .map(contributor -> findMatchingCustomer(contributor, customerMap))
                   .flatMap(Optional::stream)
                   .findFirst()
                   .orElseThrow(() -> new ApprovalAssignmentException(NO_CONTRIBUTOR_MESSAGE));
    }

    private Optional<CustomerDto> findMatchingCustomer(Contributor contributor, Map<String, CustomerDto> customerMap) {
        return contributor.getAffiliations()
                   .stream()
                   .filter(Organization.class::isInstance)
                   .map(Organization.class::cast)
                   .map(Organization::getId)
                   .filter(Objects::nonNull)
                   .map(ApprovalAssignmentServiceForImportCandidateFiles::extractTopLevelOrganizationPart)
                   .map(customerMap::get)
                   .filter(Objects::nonNull)
                   .findFirst();
    }

    private List<CustomerDto> fetchAllAssociatedCustomers(ImportCandidate importCandidate)
        throws ApprovalAssignmentException {
        var customers = new ArrayList<CustomerDto>();
        for (URI customerUri : importCandidate.getAssociatedCustomers()) {
            var customer = getCustomerById(customerUri);
            if (customer.autoPublishScopusImportFiles()) {
                return List.of(customer);
            }
            customers.add(customer);
        }
        return customers;
    }

    private CustomerDto getCustomerById(URI customerId) throws ApprovalAssignmentException {
        return attempt(() -> identityServiceClient.getCustomerById(customerId)).orElseThrow(
            failure -> new ApprovalAssignmentException(FETCH_CUSTOMER_EXCEPTION_MESSAGE.formatted(customerId)));
    }

    public enum AssignmentServiceStatus {
        NO_APPROVAL_NEEDED, CUSTOMER_FOUND
    }

    public static class ApprovalAssignmentException extends Exception {

        public ApprovalAssignmentException(String message) {
            super(message);
        }
    }

    public static final class AssignmentServiceResult {

        private final AssignmentServiceStatus status;
        private final CustomerDto customerDto;

        private AssignmentServiceResult(AssignmentServiceStatus status, CustomerDto customerDto) {
            this.status = status;
            this.customerDto = customerDto;
        }

        public static AssignmentServiceResult noApprovalNeeded() {
            return new AssignmentServiceResult(AssignmentServiceStatus.NO_APPROVAL_NEEDED, null);
        }

        public static AssignmentServiceResult customerFound(CustomerDto customer) {
            Objects.requireNonNull(customer, "Customer required when status is CUSTOMER_FOUND");
            return new AssignmentServiceResult(AssignmentServiceStatus.CUSTOMER_FOUND, customer);
        }

        public Optional<CustomerDto> getCustomerDto() {
            return Optional.ofNullable(customerDto);
        }

        public AssignmentServiceStatus getStatus() {
            return status;
        }
    }
}