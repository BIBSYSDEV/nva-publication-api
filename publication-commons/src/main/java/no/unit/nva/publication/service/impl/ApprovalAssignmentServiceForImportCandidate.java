package no.unit.nva.publication.service.impl;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import nva.commons.core.paths.UriWrapper;

public class ApprovalAssignmentServiceForImportCandidate {

    private static final String EXCEPTION_MESSAGE = "Could not fetch customer with id %s";
    private static final String CRISTIN_ID_SEPARATOR = "\\.";
    private final IdentityServiceClient identityServiceClient;

    public ApprovalAssignmentServiceForImportCandidate(IdentityServiceClient identityServiceClient) {
        this.identityServiceClient = identityServiceClient;
    }

    public Optional<CustomerDto> determineCustomerResponsibleForApproval(ImportCandidate importCandidate)
        throws ApprovalAssignmentException {
        var customers = fetchAllAssociatedCustomers(importCandidate);

        if (oneOfCustomersAllowsPublishingWithoutApproval(customers)) {
            return Optional.empty();
        }

        return getResponsibleCustomer(customers, importCandidate);
    }

    private Optional<CustomerDto> getResponsibleCustomer(Collection<CustomerDto> customers,
                                                         ImportCandidate importCandidate) {
        return getCustomerForCorrespondenceContributor(customers, importCandidate)
                   .or(() -> getCustomerForContributorWithLowestSequenceNumber(importCandidate, customers));
    }

    private Optional<CustomerDto> getCustomerForCorrespondenceContributor(Collection<CustomerDto> customers, ImportCandidate importCandidate) {
        return getCorrespondenceContributor(importCandidate)
                   .flatMap(contributor -> getCustomerForContributor(contributor, customers));
    }

    private Optional<CustomerDto> getCustomerForContributor(Contributor contributor, Collection<CustomerDto> customers) {
        return contributor.getAffiliations().stream()
                   .filter(Organization.class::isInstance)
                   .map(Organization.class::cast)
                   .map(Organization::getId)
                   .filter(Objects::nonNull)
                   .flatMap(cristinId -> getCustomerByCristinId(customers, cristinId))
                   .findFirst();
    }

    private Optional<CustomerDto> getCustomerForContributorWithLowestSequenceNumber(ImportCandidate importCandidate,
                                                                                    Collection<CustomerDto> customers) {
        return importCandidate.getEntityDescription()
                   .getContributors().stream()
                   .sorted(Comparator.comparing(Contributor::getSequence,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                   .map(contributor -> getCustomerForContributor(contributor, customers))
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .findFirst();
    }

    private Stream<CustomerDto> getCustomerByCristinId(Collection<CustomerDto> customers, URI cristinId) {
        return customers.stream()
                   .filter(customerDto -> cristinIdMatchesCustomer(cristinId, customerDto))
                   .findFirst().stream();
    }

    private boolean cristinIdMatchesCustomer(URI organizationId, CustomerDto customerDto) {
        return extractOrganizationNumber(organizationId).equals(extractOrganizationNumber(customerDto.cristinId()));
    }

    private static String extractOrganizationNumber(URI organizationId) {
        return UriWrapper.fromUri(organizationId).getLastPathElement().split(CRISTIN_ID_SEPARATOR)[0];
    }

    private Optional<Contributor> getCorrespondenceContributor(ImportCandidate importCandidate) {
        return importCandidate.getEntityDescription()
                   .getContributors().stream()
                   .filter(Contributor::isCorrespondingAuthor)
                   .findFirst();
    }

    private static boolean oneOfCustomersAllowsPublishingWithoutApproval(ArrayList<CustomerDto> customers) {
        return customers.stream().anyMatch(CustomerDto::autoPublishScopusImportFiles);
    }

    private ArrayList<CustomerDto> fetchAllAssociatedCustomers(ImportCandidate importCandidate)
        throws ApprovalAssignmentException {
        var customers = new ArrayList<CustomerDto>();

        for (URI customerUri : importCandidate.getAssociatedCustomers()) {
            customers.add(getCustomerById(customerUri));
        }
        return customers;
    }

    private CustomerDto getCustomerById(URI customerId) throws ApprovalAssignmentException {
        return attempt(() -> identityServiceClient.getCustomerById(customerId)).orElseThrow(
            failure -> new ApprovalAssignmentException(EXCEPTION_MESSAGE.formatted(customerId)));
    }

    public static class ApprovalAssignmentException extends Exception {

        public ApprovalAssignmentException(String message) {
            super(message);
        }
    }
}