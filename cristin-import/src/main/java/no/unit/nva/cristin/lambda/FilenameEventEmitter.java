package no.unit.nva.cristin.lambda;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.InputStream;
import java.io.OutputStream;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;

public class FilenameEventEmitter implements RequestStreamHandler {

    public static final String ILLEGAL_ARGUMENT_MESSAGE = "Illegal argument:";

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        String inputString = IoUtils.streamToString(input);
        attempt(() -> parseInput(inputString)).orElseThrow(fail -> handleNotParsableInputError(inputString));
    }

    private IllegalArgumentException handleNotParsableInputError(String inputString) {
        return new IllegalArgumentException(ILLEGAL_ARGUMENT_MESSAGE + inputString);
    }

    private ImportRequest parseInput(String inputString) throws com.fasterxml.jackson.core.JsonProcessingException {
        return JsonUtils.objectMapperWithEmpty.readValue(inputString, ImportRequest.class);
    }
}
