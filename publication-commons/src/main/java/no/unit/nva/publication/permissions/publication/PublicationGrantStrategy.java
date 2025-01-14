package no.unit.nva.publication.permissions.publication;

import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;

public abstract class PublicationGrantStrategy extends PublicationStrategyBase {

    protected PublicationGrantStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    public abstract boolean allowsAction(PublicationOperation permission);

}
