package no.unit.nva.cristin.mapper;

import static java.util.Objects.isNull;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.CRISTIN_PATH;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.PERSON_PATH;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import no.unit.nva.cristin.mapper.nva.exceptions.AffiliationWithoutRoleException;
import no.unit.nva.cristin.mapper.nva.exceptions.ContributorWithoutAffiliationException;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.Identity;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;

@Builder(builderClassName = "CristinContributorBuilder", toBuilder = true, builderMethodName = "builder",
    buildMethodName = "build", setterPrefix = "with")
@Getter
@Setter
@JsonIgnoreProperties({"identified_cristin_person"})
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinContributor implements Comparable<CristinContributor> {

    public static final String NAME_DELIMITER = ", ";
    public static final String MISSING_ROLE_ERROR = "Affiliation without Role";
    @JsonProperty("personlopenr")
    private Integer identifier;
    @JsonProperty("fornavn")
    private String givenName;
    @JsonProperty("etternavn")
    private String familyName;
    @JsonProperty("rekkefolgenr")
    private Integer contributorOrder;
    @JsonProperty("verified_person")
    private VerificationStatus verificationStatus;
    @JsonProperty("VARBEID_PERSON_STED")
    private List<CristinContributorsAffiliation> affiliations;

    @JacocoGenerated
    public CristinContributor() {
    }

    @JacocoGenerated
    public CristinContributorBuilder copy() {
        return this.toBuilder();
    }

    public Contributor toNvaContributor() {

        String fullName = constructFullName();
        Identity identity = new Identity.Builder()
            .withName(fullName)
            .withId(constructId().orElse(null))
            .withVerificationStatus(extractVerificationStatus())
            .build();

        return new Contributor.Builder()
            .withIdentity(identity)
            .withCorrespondingAuthor(false)
            .withAffiliations(extractAffiliations())
            .withRole(extractRoles())
            .withSequence(contributorOrder)
            .build();
    }

    public void setContributorOrder(Integer orderNumber) {
        this.contributorOrder = orderNumber;
    }

    @Override
    public int compareTo(CristinContributor contributor) {
        if (isNull(contributor.getContributorOrder()) && isNull(this.getContributorOrder())) {
            return 0;
        }
        if (isNull(contributor.getContributorOrder())) {
            return -1;
        }
        if (isNull(this.getContributorOrder())) {
            return 1;
        } else {
            return this.contributorOrder.compareTo(contributor.getContributorOrder());
        }
    }

    private ContributorVerificationStatus extractVerificationStatus() {
        return isNull(verificationStatus)
                   ? ContributorVerificationStatus.CANNOT_BE_ESTABLISHED
                   : VerificationStatus.VERIFIED.getValue().equals(verificationStatus.getValue())
                         ? ContributorVerificationStatus.VERIFIED
                         : ContributorVerificationStatus.NOT_VERIFIED;
    }

    private String constructFullName() {
        StringBuilder nameBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(getGivenName())) {
            nameBuilder.append(getGivenName());
        }
        if (StringUtils.isNotBlank(getFamilyName())) {
            nameBuilder.append(StringUtils.SPACE);
            nameBuilder.append(getFamilyName());
        }

        return StringUtils.isNotBlank(nameBuilder.toString().trim())
                   ? nameBuilder.toString().trim()
                   : null;
    }

    private RoleType extractRoles() {
        CristinContributorRole firstRole = affiliations
                                               .stream()
                                               .map(CristinContributorsAffiliation::getRoles)
                                               .filter(Objects::nonNull)
                                               .flatMap(Collection::stream)
                                               .findFirst()
                                               .orElseThrow(AffiliationWithoutRoleException::new);
        return firstRole.toNvaRole();
    }

    private List<Corporation> extractAffiliations() {
        if (isNull(affiliations) || affiliations.isEmpty()) {
            throw new ContributorWithoutAffiliationException();
        }
        return affiliations
                   .stream()
                   .filter(CristinContributorsAffiliation::isKnownAffiliation)
                   .map(CristinContributorsAffiliation::toNvaOrganization)
                   .collect(Collectors.toList());
    }

    private Optional<URI> constructId() {
        return isVerified()
            ? Optional.of(UriWrapper
            .fromUri(NVA_API_DOMAIN)
            .addChild(CRISTIN_PATH)
            .addChild(PERSON_PATH)
            .addChild(identifier.toString())
            .getUri())
            : Optional.empty();
    }

    private boolean isVerified() {
        return ContributorVerificationStatus.VERIFIED.equals(extractVerificationStatus());
    }
}
