package no.unit.nva.publication.events.handlers.expandresources;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.s3.S3Driver;
import org.junit.jupiter.api.Test;

public class PersistedResourceServiceTest {

    @Test
    void shouldThrowOnFailureToPersist() throws IOException {
        var s3Driver = mock(S3Driver.class);
        when(s3Driver.insertFile(any(), any(String.class))).thenThrow(new IOException("test"));

        var serviceUnderTest = new PersistedResourcesService(s3Driver);

        var expandedResource = new ExpandedResource();
        assertThrows(PersistedResourcesException.class, () -> serviceUnderTest.persist(expandedResource));
    }
}
