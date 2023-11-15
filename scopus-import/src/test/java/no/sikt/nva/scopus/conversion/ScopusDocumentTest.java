package no.sikt.nva.scopus.conversion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import jakarta.xml.bind.JAXB;
import java.io.StringReader;
import java.nio.file.Path;
import no.scopus.generated.DocTp;
import no.sikt.nva.scopus.ScopusConverter;
import no.sikt.nva.scopus.conversion.files.ScopusFileConverter;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

public class ScopusDocumentTest {

    @Test
    void shouldParseScopusXmlToDocTp() {
        var file = IoUtils.stringFromResources(Path.of("2-s2.0-0018978799.xml"));

        assertDoesNotThrow(() -> JAXB.unmarshal(new StringReader(file), DocTp.class));
    }

    @Test
    void some() {
        var file = IoUtils.stringFromResources(Path.of("2-s2.0-85175337306.xml"));

        var doc = JAXB.unmarshal(new StringReader(file), DocTp.class);
        var converter = new ScopusConverter(doc, mock(PiaConnection.class), mock(CristinConnection.class), mock(
            PublicationChannelConnection.class), mock(ScopusFileConverter.class));
        converter.generateImportCandidate();
    }
}
