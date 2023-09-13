package no.unit.nva.publication.model.storage;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import no.unit.nva.publication.model.business.Resource;
import org.junit.jupiter.api.Test;

class DataCompressorTest {

    @Test
    void shouldNotLooseAnyDateWhenCompressingAndUncompressing() throws IOException {
        var publication = randomPublication();
        var pubalitionDao = new ResourceDao(Resource.fromPublication(publication));
        var compressedPublicationDao = DataCompressor.compressDao(pubalitionDao);
        var decompressPublicationDao = DataCompressor.decompressDao(compressedPublicationDao, ResourceDao.class);
        var publicationFromDao = decompressPublicationDao.getResource().toPublication();

        assertEquals(publication, publicationFromDao);
    }


}