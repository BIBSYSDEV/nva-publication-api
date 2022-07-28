package no.unit.nva.publication.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MigrationTests extends ResourcesLocalTest {
    
    public static final Map<String, AttributeValue> START_FROM_BEGINNING = null;
    private static final Clock CLOCK = Clock.systemDefaultZone();
    private ResourceService resourceService;
    
    @BeforeEach
    public void init() {
        super.init();
        this.resourceService = new ResourceService(client, CLOCK);
    }
    
    @Test
    void shouldWriteBackEntryAsIsWhenMigrating()
        throws NotFoundException {
        var publication = PublicationGenerator.randomPublication();
        var savedPublication = resourceService.insertPreexistingPublication(publication);
        migrateResources();
        
        var migratedResource = resourceService.getResourceByIdentifier(savedPublication.getIdentifier());
        var migratedPublication = migratedResource.toPublication();
        assertThat(migratedPublication, is(equalTo(publication)));
    }
    
    private void migrateResources() {
        var scanResources = resourceService.scanResources(1000, START_FROM_BEGINNING);
        resourceService.refreshResources(scanResources.getDatabaseEntries());
    }
}
