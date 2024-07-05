package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;

public final class DegreeLicentiateMerger extends PublicationInstanceMerger<DegreeLicentiate> {

    public DegreeLicentiateMerger(DegreeLicentiate degreeLicentiate) {
        super(degreeLicentiate);
    }

    @Override
    public DegreeLicentiate merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof DegreeLicentiate licentiate) {
            return new DegreeLicentiate(getPages(this.publicationInstance.getPages(), licentiate.getPages()),
                                        getDate(this.publicationInstance.getSubmittedDate(),
                                                licentiate.getSubmittedDate()));
        } else {
            return this.publicationInstance;
        }
    }
}
