package no.unit.nva.publication.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ExportDataSet {
    public static final String DATA_SET_PUBLICATIONS = "publications";
    public static final String DATA_SET_CONTRIBUTORS = "contributors";
    public static final String DATA_SET_FUNDINGS = "fundings";

    private final Map<String, String> dataSetMap = new HashMap<>();

    public ExportDataSet(String publications, String contributors, String fundings) {
        if (publications != null) {
            this.dataSetMap.put(DATA_SET_PUBLICATIONS, publications);
        }
        if (contributors != null) {
            this.dataSetMap.put(DATA_SET_CONTRIBUTORS, contributors);
        }
        if (fundings != null) {
            this.dataSetMap.put(DATA_SET_FUNDINGS, fundings);
        }
    }

    public Map<String, String> getDataSets() {
        return Collections.unmodifiableMap(this.dataSetMap);
    }
}
