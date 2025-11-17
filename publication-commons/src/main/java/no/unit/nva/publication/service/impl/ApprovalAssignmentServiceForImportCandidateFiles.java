package no.unit.nva.publication.service.impl;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
     * This method analyzes the data for the ImportCandidate and determines which customer (if any) should approve the
     * candidate files. The order of preference:
     * <ol>
     *     <li>At least one customer has auto-publish enabled: no approval</li>
     *     <li>The customer that has the corresponding author with the lowest contributor-sequence number</li>
     *     <li>The customer that has the contributor with the lowest contributor-sequence number</li>
     * </ol>
     *
     * @param resource The candidate to analyze.
     * @return {@code Optional<CustomerDto>} The customer that should approve.
     * @throws ApprovalAssignmentException if customer uri for import candidate fails to dereference.
     */
    public AssignmentServiceResult determineCustomerResponsibleForApproval(Resource resource,
                                                                           Collection<URI> associatedCustomers)
        throws ApprovalAssignmentException {
        validateImportCandidateForCustomersPresence(resource, associatedCustomers);
        var customers = fetchAllAssociatedCustomers(associatedCustomers);

        var customerDto = findAnyCustomerAllowingAutoApproval(customers);
        if (customerDto.isPresent()) {
            return AssignmentServiceResult.noApprovalNeeded(customerDto.get());
        }

        return AssignmentServiceResult.customerFound(getResponsibleCustomerContributorPair(resource, customers));
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

    private Optional<CustomerDto> findAnyCustomerAllowingAutoApproval(Collection<CustomerDto> customers) {
        return customers.stream().filter(CustomerDto::autoPublishScopusImportFiles).findFirst();
    }

    private CustomerContributorPair getResponsibleCustomerContributorPair(Resource resource,
                                                                          Collection<CustomerDto> customers)
        throws ApprovalAssignmentException {
        var customerMap = customerByTopLevelInstitutionIdentifierMap(customers);

        return getContributors(resource)
                   .stream()
                   .sorted(compareByCorrespondingAuthorAndSequence())
                   .map(contributor -> findMatchingCustomer(contributor, customerMap))
                   .flatMap(Optional::stream)
                   .findFirst()
                   .orElseThrow(() -> new ApprovalAssignmentException(NO_CONTRIBUTOR_MESSAGE));
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
            return new AssignmentServiceResult(AssignmentServiceStatus.NO_APPROVAL_NEEDED, reason, null);
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