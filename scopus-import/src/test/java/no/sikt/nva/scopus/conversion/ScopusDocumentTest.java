package no.sikt.nva.scopus.conversion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import jakarta.xml.bind.JAXB;
import java.io.StringReader;
import java.net.http.HttpClient;
import java.nio.file.Path;
import no.scopus.generated.DocTp;
import no.sikt.nva.scopus.ScopusConverter;
import no.sikt.nva.scopus.conversion.files.ScopusFileConverter;
import no.sikt.nva.scopus.conversion.files.TikaUtils;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

public class ScopusDocumentTest {

    @Test
    void shouldParseScopusXmlToDocTp() {
        var file = IoUtils.stringFromResources(Path.of("2-s2.0-0018978799.xml"));

        assertDoesNotThrow(() -> JAXB.unmarshal(new StringReader(file), DocTp.class));
    }

//    @Test
//    void some() {
//        var file = IoUtils.stringFromResources(Path.of("2-s2.0-84931466170.xml"));
//        var document = JAXB.unmarshal(new StringReader(file), DocTp.class);
//        var httpClient = HttpClient.newBuilder().build();
//        var authorizedBackendUriRetriever = new AuthorizedBackendUriRetriever(httpClient,
//                                                                              SecretsReader.defaultSecretsManagerClient(), "https://nva-test.auth.eu-west-1.amazoncognito.com", "BackendCognitoClientCredentials");
//        var scopusConverter = new ScopusConverter(document,
//                                                  new PiaConnection(httpClient, new SecretsReader(), new Environment()),
//                                                  new CristinConnection(httpClient),
//                                                  new PublicationChannelConnection(authorizedBackendUriRetriever),
//                                                  new NvaCustomerConnection(authorizedBackendUriRetriever),
//                                                  mock(ScopusFileConverter.class));
//        var candidate = scopusConverter.generateImportCandidate();
//        var contributor =
//            candidate.getEntityDescription().getContributors().stream().filter(c -> c.getIdentity().getName().contains("Aleksander")).findFirst().orElseThrow();
//        var s = "";
//    }
}
