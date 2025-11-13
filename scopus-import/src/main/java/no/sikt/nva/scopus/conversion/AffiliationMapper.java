package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import no.scopus.generated.AddressPartTp;
import no.scopus.generated.AffiliationTp;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.OrganizationTp;
import no.scopus.generated.PostalCodeTp;
import no.sikt.nva.scopus.ScopusConverter;
import no.unit.nva.importcandidate.Address;
import no.unit.nva.importcandidate.AffiliationIdentifier;
import no.unit.nva.importcandidate.Country;
import no.unit.nva.importcandidate.ScopusAffiliation;

public final class AffiliationMapper {

    private AffiliationMapper() {
    }

    public static ScopusAffiliation mapToAffiliation(AffiliationTp affiliation) {
        return nonNull(affiliation) ? getAffiliation(affiliation) : null;
    }

    public static ScopusAffiliation mapToAffiliation(CollaborationTp collaboration) {
        return nonNull(collaboration) ? getAffiliation(collaboration) : null;
    }

    private static ScopusAffiliation getAffiliation(CollaborationTp collaboration) {
        return new ScopusAffiliation(null, getOrganizationNames(collaboration), null, null, null);
    }

    private static List<String> getOrganizationNames(CollaborationTp collaboration) {
        var names = new ArrayList<>(getTextNames(collaboration));
        names.add(collaboration.getIndexedName());
        return names.stream().filter(Objects::nonNull).toList();
    }

    private static List<String> getTextNames(CollaborationTp collaboration) {
        return collaboration.getText().getContent().stream().map(ScopusConverter::extractContentString).toList();
    }

    private static ScopusAffiliation getAffiliation(AffiliationTp scopusAffiliation) {
        return new ScopusAffiliation(new AffiliationIdentifier(scopusAffiliation.getAfid(), scopusAffiliation.getDptid()),
                                     getOrganizationNames(scopusAffiliation), scopusAffiliation.getSourceText(),
                                     createCountry(scopusAffiliation), createLocation(scopusAffiliation));
    }

    private static Country createCountry(AffiliationTp scopusAffiliation) {
        return new Country(scopusAffiliation.getCountry(), scopusAffiliation.getCountryAttribute());
    }

    private static Address createLocation(AffiliationTp scopusAffiliation) {
        return new Address(scopusAffiliation.getCity(), getAddressPart(scopusAffiliation),
                           scopusAffiliation.getCityGroup(), getPostalCode(scopusAffiliation));
    }

    private static String getAddressPart(AffiliationTp scopusAffiliation) {
        return Optional.ofNullable(scopusAffiliation)
                   .map(AffiliationTp::getAddressPart)
                   .map(AddressPartTp::getContent)
                   .map(ScopusConverter::extractContentString)
                   .orElse(null);
    }

    private static String getPostalCode(AffiliationTp scopusAffiliation) {
        var postalCodes = Optional.ofNullable(scopusAffiliation.getPostalCode()).orElse(Collections.emptyList());
        var postalCodeList = getPostalCodeList(postalCodes);
        return postalCodeList.isEmpty() ? null : postalCodeList.getFirst();
    }

    private static List<String> getPostalCodeList(Collection<PostalCodeTp> postalCodes) {
        return postalCodes.stream()
                   .filter(Objects::nonNull)
                   .map(PostalCodeTp::getContent)
                   .map(ScopusConverter::extractContentString)
                   .toList();
    }

    private static List<String> getOrganizationNames(AffiliationTp scopusAffiliation) {
        var organization = Optional.ofNullable(scopusAffiliation.getOrganization()).orElse(Collections.emptyList());
        return organization.stream()
                   .map(OrganizationTp::getContent)
                   .map(ScopusConverter::extractContentString)
                   .toList();
    }
}