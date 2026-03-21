package no.unit.nva.model;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Collections;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.role.RoleTypeOther;
import no.unit.nva.model.testing.PublicationGenerator;
import org.junit.jupiter.api.Test;

class ContributorTest {

  @Test
  void shouldCreateOther() {
    var description = randomString();
    var contributor = createContributor(description);
    var other = ((RoleTypeOther) contributor.role()).getDescription();
    assertThat(other, is(equalTo(description)));
  }

  @Test
  void shouldRoundtripOther() throws JsonProcessingException {
    var description = randomString();
    var json = JsonUtils.dtoObjectMapper.writeValueAsString(createContributor(description));
    var fromJson = JsonUtils.dtoObjectMapper.readValue(json, Contributor.class);
    var other = ((RoleTypeOther) fromJson.role()).getDescription();
    assertThat(other, is(equalTo(description)));
  }

  @Test
  void shouldRoundtripRoleTypeOtherDirectly() throws JsonProcessingException {
    var description = randomString();
    RoleType roleType = new RoleTypeOther(Role.OTHER, description);
    var json = JsonUtils.dtoObjectMapper.writeValueAsString(roleType);
    var fromJson = JsonUtils.dtoObjectMapper.readValue(json, RoleType.class);
    assertThat(fromJson, is(instanceOf(RoleTypeOther.class)));
    assertThat(((RoleTypeOther) fromJson).getDescription(), is(equalTo(description)));
  }

  @Test
  void shouldRoundtripAsPartOfList() throws JsonProcessingException {
    var description = randomString();
    List<RoleType> roles = List.of(new RoleTypeOther(Role.OTHER, description));
    var json = JsonUtils.dtoObjectMapper.writeValueAsString(roles);
    var fromJson =
        JsonUtils.dtoObjectMapper.readValue(json, new TypeReference<List<RoleType>>() {});
    assertThat(fromJson.getFirst(), is(instanceOf(RoleTypeOther.class)));
    assertThat(((RoleTypeOther) fromJson.getFirst()).getDescription(), is(equalTo(description)));
  }

  @Test
  void shouldPreserveDescriptionWhenTypeIsOtherInRawJson() throws JsonProcessingException {
    var description = randomString();
    var json =
        """
        {"type": "OTHER", "description": "%s"}
        """
            .formatted(description);
    var fromJson = JsonUtils.dtoObjectMapper.readValue(json, RoleType.class);
    assertThat(fromJson, is(instanceOf(RoleTypeOther.class)));
    assertThat(((RoleTypeOther) fromJson).getDescription(), is(equalTo(description)));
  }

  @Test
  void shouldNotBeInstanceOfRoleTypeOtherWhenTypeIsNotOther() throws JsonProcessingException {
    var json =
        """
        {"type": "CREATOR"}
        """;
    var fromJson = JsonUtils.dtoObjectMapper.readValue(json, RoleType.class);
    assertThat(fromJson.getClass(), is(equalTo(RoleType.class)));
  }


  private static Contributor createContributor(String description) {
    return new Contributor(
            createIdentity(),
            List.of(PublicationGenerator.randomOrganization()),
            new RoleTypeOther(Role.OTHER, description),
            1,
            true);
  }

  private static Identity createIdentity() {
    return new Identity.Builder()
            .withVerificationStatus(ContributorVerificationStatus.VERIFIED)
            .withId(randomUri())
            .withAdditionalIdentifiers(Collections.emptyList())
            .withName(randomString())
            .withNameType(NameType.PERSONAL)
            .withOrcId(randomString())
            .build();
  }
}
