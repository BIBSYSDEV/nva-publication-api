package no.unit.nva.publication.adapter;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.UUID;

public final class MockLambdaContext implements Context {

    private static final String FUNCTION_NAME = "publication-adapter";
    private static final String FUNCTION_VERSION = "LATEST";
    private static final int MEMORY_LIMIT_MB = 1024;
    private static final int REMAINING_TIME_MS = 30_000;

    private final String requestId = UUID.randomUUID().toString();

    @Override
    public String getAwsRequestId() {
        return requestId;
    }

    @Override
    public String getLogGroupName() {
        return FUNCTION_NAME;
    }

    @Override
    public String getLogStreamName() {
        return FUNCTION_NAME;
    }

    @Override
    public String getFunctionName() {
        return FUNCTION_NAME;
    }

    @Override
    public String getFunctionVersion() {
        return FUNCTION_VERSION;
    }

    @Override
    public String getInvokedFunctionArn() {
        return "arn:local:lambda:" + FUNCTION_NAME;
    }

    @Override
    public CognitoIdentity getIdentity() {
        return null;
    }

    @Override
    public ClientContext getClientContext() {
        return null;
    }

    @Override
    public int getRemainingTimeInMillis() {
        return REMAINING_TIME_MS;
    }

    @Override
    public int getMemoryLimitInMB() {
        return MEMORY_LIMIT_MB;
    }

    @Override
    public LambdaLogger getLogger() {
        return new LambdaLogger() {
            @Override
            public void log(String message) {
                System.out.println(message);
            }

            @Override
            public void log(byte[] message) {
                System.out.write(message, 0, message.length);
            }
        };
    }
}
