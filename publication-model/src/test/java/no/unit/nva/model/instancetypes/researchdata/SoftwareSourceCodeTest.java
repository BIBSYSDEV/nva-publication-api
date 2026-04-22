package no.unit.nva.model.instancetypes.researchdata;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.instancetypes.PublicationInstance;
import org.junit.jupiter.api.Test;

class SoftwareSourceCodeTest {

  @Test
  void shouldRoundTripSerializationPreservingValues() throws Exception {
    var original = new SoftwareSourceCode(randomString(), randomUri());

    var json = JsonUtils.dtoObjectMapper.writeValueAsString(original);
    var deserialized = JsonUtils.dtoObjectMapper.readValue(json, PublicationInstance.class);

    assertThat(deserialized, is(equalTo(original)));
  }

  @Test
  void shouldThrowWhenSoftwareVersionIsNull() {
    assertThrows(NullPointerException.class, () -> new SoftwareSourceCode(null, randomUri()));
  }

  @Test
  void shouldSerializeWithExpectedFieldNamesAndTypeDiscriminator() throws Exception {
    var instance = new SoftwareSourceCode("1.2.3", randomUri());

    var json =
        JsonUtils.dtoObjectMapper.readTree(JsonUtils.dtoObjectMapper.writeValueAsString(instance));

    assertThat(json.get("type").asText(), is(equalTo("SoftwareSourceCode")));
    assertThat(json.get(SoftwareSourceCode.SOFTWARE_VERSION_FIELD).asText(), is(equalTo("1.2.3")));
    assertThat(json.has(SoftwareSourceCode.CODE_REPOSITORY_FIELD), is(true));
  }

  @Test
  void shouldRoundTripInsidePublicationWrapper() throws Exception {
    var instance = new SoftwareSourceCode(randomString(), randomUri());

    var json = JsonUtils.dtoObjectMapper.writeValueAsString(instance);
    var deserialized = JsonUtils.dtoObjectMapper.readValue(json, PublicationInstance.class);

    assertThat(deserialized, is(equalTo(instance)));
    assertThat(deserialized.getInstanceType(), is(equalTo("SoftwareSourceCode")));
  }

  @Test
  void shouldImplementStructuralEquality() {
    var version = randomString();
    var repository = randomUri();

    var first = new SoftwareSourceCode(version, repository);
    var second = new SoftwareSourceCode(version, repository);
    var different = new SoftwareSourceCode(randomString(), repository);

    assertThat(first, is(equalTo(second)));
    assertThat(first.hashCode(), is(equalTo(second.hashCode())));
    assertThat(first, is(not(equalTo(different))));
  }
}
