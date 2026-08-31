package no.unit.nva.model.contexttypes;

import static no.unit.nva.model.testing.EntityDescriptionBuilder.randomIdentity;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Agent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ResearchDataTest {

  public static Stream<Agent> agentProvider() {
    return Stream.of(
        new Publisher(randomUri()),
        new UnconfirmedPublisher(randomString()),
        new NullPublisher(),
        randomIdentity());
  }

  @ParameterizedTest
  @MethodSource("agentProvider")
  void shouldAllowAnyAgentAsPublisher(Agent agent) {
    assertDoesNotThrow(() -> new ResearchData(agent));
  }

  @ParameterizedTest
  @MethodSource("agentProvider")
  void shouldSerializeAnyAgentAsPublisher(Agent agent) throws JsonProcessingException {
    var researchData = new ResearchData(agent);
    var jsonString = JsonUtils.dtoObjectMapper.writeValueAsString(researchData);
    var actual = JsonUtils.dtoObjectMapper.readValue(jsonString, ResearchData.class);
    assertEquals(researchData, actual);
  }
}
