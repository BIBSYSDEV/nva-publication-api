package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.Map;
import no.unit.nva.model.instancetypes.PublicationInstance;

public final class MapMerger extends PublicationInstanceMerger<Map> {

    public MapMerger(Map map) {
        super(map);
    }

    @Override
    public Map merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof Map map) {
            return new Map(
                getNonNullValue(this.publicationInstance.getDescription(), map.getDescription()),
                getPages(this.publicationInstance.getPages(), map.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
