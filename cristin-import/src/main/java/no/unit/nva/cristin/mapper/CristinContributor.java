package no.unit.nva.cristin.mapper;

import static java.util.Objects.isNull;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.CRISTIN_PERSONS_URI;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.SHOULD_CREATE_CONTRIBUTOR_ID;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.cristin.mapper.nva.exceptions.AffiliationWithoutRoleException;
import no.unit.nva.cristin.mapper.nva.exceptions.ContributorWithoutAffiliationException;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Role;
import no.unit.nva.publication.s3imports.UriWrapper;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

@Data
@Builder(
    builderClassName = "CristinContributorBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinContributor {

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
                                .withId(constructId())
                                .build();

        return new Contributor.Builder()
                   .withIdentity(identity)
                   .withCorrespondingAuthor(false)
                   .withAffiliations(extractAffiliations())
                   .withRole(extractRoles())
                   .withSequence(contributorOrder)
                   .build();
    }

    private String constructFullName() {
        StringBuilder nameBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(getFamilyName())) {
            nameBuilder.append(getFamilyName());
        }
        if (StringUtils.isNotBlank(getGivenName())) {
            nameBuilder.append(NAME_DELIMITER);
            nameBuilder.append(getGivenName());
        }
        return StringUtils.isNotBlank(nameBuilder.toString()) ? nameBuilder.toString() : null;
    }

    private Role extractRoles() {
        CristinContributorRole firstRole =
            affiliations.stream()
                .map(CristinContributorsAffiliation::getRoles)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .findFirst()
                .orElseThrow(() -> new AffiliationWithoutRoleException());
        return firstRole.toNvaRole();
    }

    private List<Organization> extractAffiliations() {
        if (isNull(affiliations)) {
            throw new ContributorWithoutAffiliationException();
        }
        return affiliations.stream()
                   .map(CristinContributorsAffiliation::toNvaOrganization)
                   .collect(Collectors.toList());
    }

    private URI constructId() {
        return SHOULD_CREATE_CONTRIBUTOR_ID
                   ? new UriWrapper(CRISTIN_PERSONS_URI)
                         .addChild(identifier.toString())
                         .getUri()
                   : null;
    }
}
