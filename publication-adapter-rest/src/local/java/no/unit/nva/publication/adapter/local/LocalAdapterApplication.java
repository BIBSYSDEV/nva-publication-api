package no.unit.nva.publication.adapter.local;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.adapter.AdapterApplication;

/**
 * Entry point for local runs. Identical to {@link AdapterApplication} except that it trusts the
 * X-Adapter-Authorizer header, which lets you exercise handlers without obtaining a token.
 *
 * <p>This class and {@link TestHeaderAuthorizerProvider} live in the `local` source set, so they are
 * not on the classpath of the distribution this module builds. Deploying the bypass would have to be
 * a deliberate build change rather than a configuration mistake.
 */
public final class LocalAdapterApplication {

    private LocalAdapterApplication() {
    }

    public static void main(String[] args) {
        AdapterApplication.startWith(
            new TestHeaderAuthorizerProvider(JsonUtils.dtoObjectMapper));
    }
}
