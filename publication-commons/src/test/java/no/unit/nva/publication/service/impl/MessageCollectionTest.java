package no.unit.nva.publication.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import no.unit.nva.publication.storage.model.MessageType;
import org.junit.jupiter.api.Test;

class MessageCollectionTest {

    @Test
    public void emptyReturnsAnEmptyMessageCollection() {
        var emptyCollection = MessageCollection.empty(MessageType.SUPPORT);
        assertThat(emptyCollection.getMessages(), is(empty()));
    }
}