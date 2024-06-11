package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import static java.util.Objects.nonNull;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.researchdata.CompliesWithUris;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import no.unit.nva.model.instancetypes.researchdata.GeographicalDescription;
import no.unit.nva.model.instancetypes.researchdata.ReferencedByUris;

public final class DataSetMerger extends PublicationInstanceMerger {

    private DataSetMerger() {
        super();
    }

    public static DataSet merge(DataSet dataSet, PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof DataSet newDataSet) {
            return new DataSet(dataSet.isUserAgreesToTermsAndConditions(),
                               getGeographicalCoverage(dataSet.getGeographicalCoverage(), newDataSet.getGeographicalCoverage()),
                               new ReferencedByUris(getUriSet(dataSet.getReferencedBy(), newDataSet.getReferencedBy())),
                               mergeCollections(dataSet.getRelated(), newDataSet.getRelated(), LinkedHashSet::new),
                               new CompliesWithUris(getUriSet(dataSet.getCompliesWith(), newDataSet.getCompliesWith())));
        } else {
            return dataSet;
        }
    }

    private static Set<URI> getUriSet(Set<URI> oldReferencedBy, Set<URI> newReferencedBy) {
        Set<URI> result = new HashSet<>();
        if (nonNull(oldReferencedBy)) {
            result.addAll(oldReferencedBy);
        }
        if (nonNull(newReferencedBy)) {
            result.addAll(newReferencedBy);
        }
        return result;
    }

    private static GeographicalDescription getGeographicalCoverage(GeographicalDescription oldGeographicalCoverage,
                                                                   GeographicalDescription newGeographicalCoverage) {
        return Optional.ofNullable(oldGeographicalCoverage).map(GeographicalDescription::getDescription).isPresent()
                   ? oldGeographicalCoverage : newGeographicalCoverage;
    }
}
