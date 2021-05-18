package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.SHOULD_CREATE_CONTRIBUTOR_ID;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.cristin.lambda.constants.MappingConstants;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Role;
import no.unit.nva.model.exceptions.MalformedContributorException;
import no.unit.nva.publication.s3imports.UriWrapper;
import nva.commons.core.JacocoGenerated;

@Data
@Builder(
    builderClassName = "CristinObjectBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinContributor {

    public static final String NAME_DELIMITER = ", ";
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

    public Contributor toNvaContributor() throws MalformedContributorException {
        String fullName = getFamilyName() + NAME_DELIMITER + getGivenName();
        Identity identity = new Identity.Builder()
                                .withName(fullName)
                                .withId(constructId())
                                .build();

        return new Contributor.Builder()
                   .withIdentity(identity)
                   .withCorrespondingAuthor(false)
                   .withRole(Role.CREATOR)
                   .withSequence(contributorOrder)
                   .build();
    }

    private URI constructId() {
        return SHOULD_CREATE_CONTRIBUTOR_ID
                   ? new UriWrapper(MappingConstants.CRISTIN_API)
                         .addChild(Path.of(identifier.toString()))
                         .getUri()
                   : null;
    }
}
