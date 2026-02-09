package no.unit.nva.importcandidate;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.Organization;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
@JsonTypeInfo(use = Id.NAME, property = "type")
public record Affiliation(Corporation targetOrganization, SourceOrganization sourceOrganization) {

    private static final String UIO_LEGACY_IDENTIFIER = "185.0.0.0";
    private static final String UIO_IDENTIFIER = "185.90.0.0";

    public Affiliation {
        if (targetOrganization instanceof Organization organization) {
            var uriWrapper = UriWrapper.fromUri(organization.getId());
            targetOrganization = UIO_LEGACY_IDENTIFIER.equals(uriWrapper.getLastPathElement())
                                     ? replaceLegacyIdentifier(uriWrapper)
                                     : organization;
        }
    }

    private static Organization replaceLegacyIdentifier(UriWrapper uri) {
        return Organization.fromUri(uri.replacePathElementByIndexFromEnd(0, UIO_IDENTIFIER).getUri());
    }
}
