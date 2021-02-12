package no.unit.nva.publication.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Collections;
import no.unit.nva.publication.exception.EmptyValueMapException;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class ResourceServiceUtilsTest {
    
    @Test
    public void parseValueMapThrowsEmptyValueMapExceptionWhenInputIsAnEmptyMap() {
        Executable action =
            () -> ResourceServiceUtils.parseAttributeValuesMap(Collections.emptyMap(), ResourceDao.class);
        assertThrows(EmptyValueMapException.class, action);
    }
}