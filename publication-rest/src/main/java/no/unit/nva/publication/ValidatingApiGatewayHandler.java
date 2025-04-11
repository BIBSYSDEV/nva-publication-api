package no.unit.nva.publication;

import com.amazonaws.services.lambda.runtime.Context;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.UnprocessableContentException;
import nva.commons.core.Environment;

public abstract class ValidatingApiGatewayHandler<I, O> extends ApiGatewayHandler<I, O> {
    private final Validator validator;

    public ValidatingApiGatewayHandler(Class<I> iclass, Environment environment) {
        super(iclass, environment);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Override
    protected O processInput(I input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        if (Objects.nonNull(input)) {
            validateInput(input);
        }
        return processValidatedInput(input, requestInfo, context);
    }

    private void validateInput(I input) throws UnprocessableContentException {
        var result = validator.validate(input);
        if (!result.isEmpty()) {
            throw new UnprocessableContentException(toMessage(result));
        }
    }

    private String toMessage(Set<ConstraintViolation<I>> violations) {
        return violations.stream().map(this::summarizeConstraintViolation).collect(Collectors.joining(", "));
    }

    private String summarizeConstraintViolation(ConstraintViolation<I> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    protected abstract O processValidatedInput(I input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException;
}
