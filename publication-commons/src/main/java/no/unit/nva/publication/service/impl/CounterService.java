package no.unit.nva.publication.service.impl;

import no.unit.nva.publication.model.storage.CounterDao;

public interface CounterService {
    CounterDao fetch();

    CounterDao increment();
}
