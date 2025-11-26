package no.sikt.nva.scopus.utils;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.publication.external.services.cristin.CristinOrganization;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.testutils.RandomDataGenerator;

public class ScopusTestUtils {

    public static CustomerDto randomCustomer(URI cristinId) {
        return new CustomerDto(RandomDataGenerator.randomUri(), UUID.randomUUID(), randomString(), randomString(),
                               randomString(), cristinId,
                               PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY.getValue(), randomBoolean(),
                               randomBoolean(), randomBoolean(), Collections.emptyList(),
                               new CustomerDto.RightsRetentionStrategy(randomString(),
                                                                       RandomDataGenerator.randomUri()),
                               randomBoolean());
    }

    public static CristinOrganization randomCristinOrganization() {
        return new CristinOrganization(randomUri(), randomUri(), randomString(), List.of(), randomString(),
                                       Map.of("no", randomString()));
    }
}
