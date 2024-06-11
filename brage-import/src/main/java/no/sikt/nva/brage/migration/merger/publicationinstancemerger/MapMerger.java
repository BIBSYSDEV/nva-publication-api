package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.Map;
import no.unit.nva.model.instancetypes.PublicationInstance;

public final class MapMerger extends PublicationInstanceMerger {

    private MapMerger() {
        super();
    }

    public static Map merge(Map map, PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof Map newMap) {
            return new Map(getNonNullValue(map.getDescription(), newMap.getDescription),
                           getPages(map.getPages(), newMap.getPages()));
        } else {
            return map;
        }
    }
}
