package no.unit.nva.publication.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import no.unit.nva.publication.testing.http.RandomPersonServiceResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MigrationTests extends ResourcesLocalTest {

    private static final Clock CLOCK = Clock.systemDefaultZone();
    public static final Map<String, AttributeValue> START_FROM_BEGINNING = null;
    private ResourceService resourceService;
    private URI affiliationUri;

    @BeforeEach
    public void init() {
        super.init();
        affiliationUri = RandomPersonServiceResponse.randomUri();
        var externalServicesHttpClient =
            new FakeHttpClient<>(new RandomPersonServiceResponse(affiliationUri).toString());
        this.resourceService = new ResourceService(client, externalServicesHttpClient, CLOCK);
    }

    @Test
    void shouldWriteBackEntryWithResourceOwnerWHenReadingResourceWithNullResourceOwnerAndNonNullOwner()
        throws TransactionFailedException, NotFoundException {
        var publication = PublicationGenerator.randomPublication();
        publication.setResourceOwner(null);
        var expectedResourceOwner = publication.getOwner();
        var savedPublication = resourceService.insertPreexistingPublication(publication);
        assertThat(savedPublication.getResourceOwner(), is(nullValue()));
        migrateResources();

        var migratedResource = resourceService.getResourceByIdentifier(savedPublication.getIdentifier());
        assertThat(migratedResource.getResourceOwner().getOwner(),is(equalTo(expectedResourceOwner)));
        assertThat(migratedResource.getResourceOwner().getOwnerAffiliation(),is(equalTo(affiliationUri)));

    }

    private void migrateResources() {
        var scanResources = resourceService.scanResources(1000, START_FROM_BEGINNING);
        resourceService.refreshResources(scanResources.getDatabaseEntries());
    }
}
