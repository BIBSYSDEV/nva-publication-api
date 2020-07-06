package no.unit.nva.publication.query;

import java.util.ArrayList;
import java.util.List;
import no.unit.nva.publication.model.PublicationSummary;

public class PublishedPublicationsResponse  extends ArrayList<PublicationSummary>  {
    public PublishedPublicationsResponse(List<PublicationSummary> publicationsResponse) {
        super(publicationsResponse);
    }
}
