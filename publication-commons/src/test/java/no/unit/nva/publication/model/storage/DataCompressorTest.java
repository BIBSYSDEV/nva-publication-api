package no.unit.nva.publication.model.storage;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import no.unit.nva.publication.model.business.Resource;
import org.junit.jupiter.api.Test;

class DataCompressorTest {

    @Test
    void shouldNotLooseAnyDateWhenCompressingAndUncompressing() {
        var publication = randomPublication();
        var publicationDao = new ResourceDao(Resource.fromPublication(publication));
        var compressedPublicationDao = DataCompressor.compressDaoData(publicationDao);
        var decompressPublicationDao = DataCompressor.decompressDao(compressedPublicationDao, ResourceDao.class);
        var publicationFromDao = decompressPublicationDao.getResource().toPublication();

        assertEquals(publication, publicationFromDao);
    }


    @Test
    void failedJsonSerializationShouldThrowException() {
        var badDao = new ResourceDao();
        assertThrows(IllegalArgumentException.class, () -> DataCompressor.compressDaoData(badDao));
    }
}