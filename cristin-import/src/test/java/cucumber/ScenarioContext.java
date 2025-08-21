package cucumber;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinMapper;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.CristinTitle;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.utils.CustomerService;
import no.unit.nva.publication.utils.CristinUnitsUtilImpl;
import nva.commons.core.attempt.Try;
import nva.commons.core.ioutils.IoUtils;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class ScenarioContext {

    private final CristinUnitsUtilImpl cristinUnitsUtil;
    private CristinObject cristinEntry;
    private Try<Publication> mappingAttempt;
    private Publication nvaEntry;

    public ScenarioContext() {
        var s3Client = mock(S3Client.class);
        when(s3Client.utilities()).thenReturn(S3Client.create().utilities());
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenAnswer(
            (Answer<ResponseBytes<GetObjectResponse>>) invocationOnMock -> getUnitsResponseBytes());
        this.cristinUnitsUtil = new CristinUnitsUtilImpl(s3Client, "s3://some-bucket/some-key");
    }

    private static ResponseBytes getUnitsResponseBytes() {
        var result = IoUtils.stringFromResources(Path.of("cristinUnits/units-norway.json"));
        var httpResponse = mock(ResponseBytes.class);
        when(httpResponse.asUtf8String()).thenReturn(result);
        return httpResponse;
    }

    public Try<Publication> getMappingAttempt() {
        return mappingAttempt;
    }

    public void newCristinEntry(Supplier<CristinObject> cristinObjectSupplier) {
        this.cristinEntry = cristinObjectSupplier.get();
    }

    public void newCristinEntry() {
        this.cristinEntry = CristinDataGenerator.randomObject();
    }

    public CristinObject getCristinEntry() {
        return this.cristinEntry;
    }

    public void convertToNvaEntry() {
        mappingAttempt =
            attempt(() -> new CristinMapper(cristinEntry, cristinUnitsUtil, mock(S3Client.class),
                                            mock(UriRetriever.class), mock(CustomerService.class))
                              .generatePublication());
    }

    public Publication getNvaEntry() {
        if (isNull(this.nvaEntry)) {
            this.nvaEntry = this.mappingAttempt.orElseThrow();
        }
        return this.nvaEntry;
    }

    public void addEmptyCristinTitle() {
        cristinEntry = cristinEntry.copy().withCristinTitles(new ArrayList<>()).build();
    }

    public void addCristinTitle() {
        cristinEntry.getCristinTitles().add(CristinTitle.builder().build());
    }

    public CristinTitle getLatestCristinTitle() {
        List<CristinTitle> titles = cristinEntry.getCristinTitles();
        return titles.get(titles.size() - 1);
    }

    public boolean mappingIsSuccessful() {
        return this.mappingAttempt.isSuccess();
    }
}
