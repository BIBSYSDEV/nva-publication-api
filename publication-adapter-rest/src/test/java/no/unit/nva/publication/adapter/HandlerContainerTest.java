package no.unit.nva.publication.adapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.Environment;
import org.junit.jupiter.api.Test;

class HandlerContainerTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonUtils.dtoObjectMapper;

    @Test
    void shouldNotFallBackToNoArgConstructorWhenCollaboratorIsMissing() {
        HandlerNeedingCollaborator.noArgConstructorWasUsed = false;
        var container = new HandlerContainer().register(Environment.class, new Environment());

        var failure = assertThrows(IllegalStateException.class,
                                   () -> container.create(HandlerNeedingCollaborator.class));

        assertThat(failure.getMessage(), containsString(HandlerNeedingCollaborator.class.getName()));
        assertThat(HandlerNeedingCollaborator.noArgConstructorWasUsed, is(false));
    }

    @Test
    void shouldReturnSeparateInstancePerCallSinceHandlersHoldPerRequestState() {
        var container = containerWithCollaborator();

        var first = container.create(HandlerNeedingCollaborator.class);
        var second = container.create(HandlerNeedingCollaborator.class);

        assertThat(first, not(sameInstance(second)));
    }

    @Test
    void shouldResolveConstructorOnlyOncePerHandlerClass() {
        var container = containerWithCollaborator();

        container.create(HandlerNeedingCollaborator.class);
        container.create(HandlerNeedingCollaborator.class);

        assertThat(container.resolvedConstructorCount(), is(1));
    }

    @Test
    void shouldUseConstructorWithMostRegisteredParameters() {
        var container = containerWithCollaborator();

        assertThat(container.create(HandlerNeedingCollaborator.class),
                   instanceOf(HandlerNeedingCollaborator.class));
    }

    private static HandlerContainer containerWithCollaborator() {
        return new HandlerContainer()
                   .register(Environment.class, new Environment())
                   .register(Collaborator.class, new Collaborator());
    }

    static final class Collaborator {

    }

    static final class HandlerNeedingCollaborator extends ApiGatewayHandler<Void, String> {

        // Stands in for the real handlers, whose no-arg constructors call
        // ResourceService.defaultService() and reach out to real AWS
        static boolean noArgConstructorWasUsed;

        HandlerNeedingCollaborator() {
            super(Void.class, new Environment());
            noArgConstructorWasUsed = true;
        }

        HandlerNeedingCollaborator(Collaborator collaborator, Environment environment) {
            super(Void.class, environment);
        }

        @Override
        protected void validateRequest(Void input, RequestInfo requestInfo, com.amazonaws.services.lambda.runtime.Context context) {
            // no validation needed
        }

        @Override
        protected String processInput(Void input, RequestInfo requestInfo,
                                      com.amazonaws.services.lambda.runtime.Context context) {
            return OBJECT_MAPPER.createObjectNode().toString();
        }

        @Override
        protected Integer getSuccessStatusCode(Void input, String output) {
            return 200;
        }
    }
}
