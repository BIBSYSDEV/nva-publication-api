package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.PublicationGenerator.publicationWithoutIdentifier;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.storage.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResourceServiceTest extends ResourcesDynamoDbLocalTest {

    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init();
        resourceService = new ResourceService(client);
    }

    @Test
    public void createResourceCreatesResource()  {
        Resource resource = Resource.fromPublication(publicationWithoutIdentifier());
        resourceService.createResource(resource);
        Resource savedResource = resourceService.getResource(resource);
        assertThat(savedResource,is(equalTo(resource)));
        assertThat(savedResource,is(not(sameInstance(resource))));
    }
}