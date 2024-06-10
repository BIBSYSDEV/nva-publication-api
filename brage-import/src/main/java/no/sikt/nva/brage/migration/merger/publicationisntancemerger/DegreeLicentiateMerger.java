package no.sikt.nva.brage.migration.merger.publicationisntancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;

public class DegreeLicentiateMerger extends PublicationInstanceMerger{

    private DegreeLicentiateMerger() {
    }

    public static DegreeLicentiate merge(DegreeLicentiate degreeLicentiate,
                                         PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof DegreeLicentiate newDegreeBachelor) {
            return new DegreeLicentiate(getPages(degreeLicentiate.getPages(), newDegreeBachelor.getPages()),
                                        getDate(degreeLicentiate.getSubmittedDate(), newDegreeBachelor.getSubmittedDate()));
        } else {
            return degreeLicentiate;
        }
    }
}
