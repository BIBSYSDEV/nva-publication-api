package no.unit.nva.publication.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import java.util.Base64;
import nva.commons.apigateway.GatewayResponse;

public final class GatewayResponseWriter {

    private static final TypeReference<GatewayResponse<String>> RESPONSE_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public GatewayResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(Context ctx, String gatewayResponseJson) throws Exception {
        GatewayResponse<String> response = objectMapper.readValue(gatewayResponseJson, RESPONSE_TYPE);
        ctx.status(response.getStatusCode());
        response.getHeaders().forEach(ctx::header);
        writeBody(ctx, response);
    }

    private static void writeBody(Context ctx, GatewayResponse<String> response) {
        var body = response.getBody();
        if (body == null || body.isEmpty()) {
            return;
        }
        if (Boolean.TRUE.equals(response.getIsBase64Encoded())) {
            ctx.result(Base64.getDecoder().decode(body));
        } else {
            ctx.result(body);
        }
    }
}
