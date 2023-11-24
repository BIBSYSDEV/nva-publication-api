package no.unit.nva.publication;

import static java.net.HttpURLConnection.HTTP_GONE;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.DefaultProblem;

public class GoneProblem extends AbstractThrowableProblem implements JsonSerializable {

    private final PublicationDetail publicationDetail;

    public GoneProblem(Publication publication) {
        this.publicationDetail = PublicationDetail.fromPublication(publication);
    }

    public PublicationDetail getPublicationDetail() {
        return publicationDetail;
    }
}
