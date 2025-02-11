package no.unit.nva.publication.model.business;

import java.util.Optional;
import no.unit.nva.publication.service.impl.ResourceService;

public interface QueryObject<T> {

    Optional<T> fetch(ResourceService resourceService);

    void delete(ResourceService resourceService);
}
