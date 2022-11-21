package no.sikt.nva.brage.migration.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.model.Publication;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

public class BrageEntryEventConsumer implements RequestHandler<S3Event, Publication> {

    @Override
    public Publication handleRequest(S3Event input, Context context) {
        return null;
    }
}
