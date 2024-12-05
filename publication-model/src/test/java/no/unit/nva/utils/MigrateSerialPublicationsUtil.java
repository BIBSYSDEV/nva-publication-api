package no.unit.nva.utils;

import java.net.URI;
import java.util.UUID;
import nva.commons.core.paths.UriWrapper;

@Deprecated
public class MigrateSerialPublicationsUtil {

    public static URI constructExampleIdWithPath(String channelPath) {
        return UriWrapper.fromHost("example.org")
                   .addChild("publication-channels-v2")
                   .addChild(channelPath)
                   .addChild(UUID.randomUUID().toString())
                   .addChild("2020")
                   .getUri();
    }

}
