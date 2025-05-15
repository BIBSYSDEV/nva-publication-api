package no.unit.nva.publication.permissions.publication.restrict;

import static no.unit.nva.model.PublicationStatus.DELETED;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationDenyStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public class DeletedUploadDenyStrategy extends PublicationStrategyBase implements PublicationDenyStrategy {
    public DeletedUploadDenyStrategy(Resource resource, UserInstance userInstance) {
        super(resource, userInstance);
    }

    @Override
    public boolean deniesAction(PublicationOperation permission) {
        return DELETED.equals(resource.getStatus()) && permission.equals(PublicationOperation.UPLOAD_FILE);
    }
}
