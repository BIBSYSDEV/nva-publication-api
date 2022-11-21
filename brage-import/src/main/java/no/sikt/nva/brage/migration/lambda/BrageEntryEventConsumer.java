package no.sikt.nva.brage.migration.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.model.Publication;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrageEntryEventConsumer implements RequestHandler<S3Event, Publication> {

    private static final Logger logger = LoggerFactory.getLogger(BrageEntryEventConsumer.class);

    @Override
    public Publication handleRequest(S3Event input, Context context) {
        logger.info("Hello world");
        return null;
    }
}
