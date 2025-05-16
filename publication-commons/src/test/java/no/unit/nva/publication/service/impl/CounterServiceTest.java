package no.unit.nva.publication.service.impl;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import no.unit.nva.model.Publication;
import no.unit.nva.model.additionalidentifiers.CristinIdentifier;
import no.unit.nva.model.additionalidentifiers.SourceName;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.CounterDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CounterServiceTest extends ResourcesLocalTest {

    private CounterService counterService;
    private ResourceService resourceService;

    @Override
    @BeforeEach
    public void init() {
        super.init();
        resourceService = getResourceServiceBuilder().build();
        counterService = new CristinIdentifierCounterService(super.client, RESOURCES_TABLE_NAME);
    }

    @Test
    void shouldIncreaseCounter() {
        var initialCount = counterService.next();

        assertEquals(CounterDao.fromValue(10_000_000), initialCount);

        var persistedCounter = counterService.next();

        assertEquals(CounterDao.fromValue(10_000_001), persistedCounter);

        var fetchedCounter = counterService.fetch();

        assertEquals(CounterDao.fromValue(10_000_001), fetchedCounter);
    }

    @Test
    void shouldCreatePublicationWithSyntheticCristinIdentifier() throws BadRequestException, NotFoundException {
        var publication = randomPublication().copy().withAdditionalIdentifiers(Set.of()).build();
        var peristedPublication = persistPublication(publication);
        var peristedResource = resourceService.getResourceByIdentifier(peristedPublication.getIdentifier());

        assertTrue(peristedResource.getCristinIdentifier().isPresent());
    }

    @Test
    void shouldNotCreatePublicationWithSyntheticCristinIdentifierWhenCristinIdentifierAlreadyExists()
        throws BadRequestException, NotFoundException {
        var existingCristinIdentifier = randomCristinIdentifier();
        var publication = randomPublicationWithAdditionalIdentifier(existingCristinIdentifier);
        var peristedPublication = persistPublication(publication);
        var peristedResource = resourceService.getResourceByIdentifier(peristedPublication.getIdentifier());

        var additionalIdentifiers = peristedResource.getAdditionalIdentifiers();

        assertThat(additionalIdentifiers, hasSize(1));
        assertEquals(peristedResource.getCristinIdentifier().orElseThrow(), existingCristinIdentifier);
    }

    private static Publication randomPublicationWithAdditionalIdentifier(CristinIdentifier existingCristinIdentifier) {
        return randomPublication().copy().withAdditionalIdentifiers(Set.of(existingCristinIdentifier)).build();
    }

    private static CristinIdentifier randomCristinIdentifier() {
        return new CristinIdentifier(SourceName.fromCristin("ntnu"), "123456");
    }

    private Publication persistPublication(Publication publication) throws BadRequestException {
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
}
