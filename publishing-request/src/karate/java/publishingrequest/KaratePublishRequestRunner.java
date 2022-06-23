package no.unit.nva.publication.publishingrequest;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.Tag;

@Tag("KarateTest")
public class KaratePublishRequestRunner {

    @Karate.Test
    Karate runKarateFeatures() {
        return Karate.run().relativeTo(getClass());
    }

}
