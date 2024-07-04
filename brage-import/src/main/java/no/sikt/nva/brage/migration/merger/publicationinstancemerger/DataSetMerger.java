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

public final class DataSetMerger extends PublicationInstanceMerger<DataSet> {

    public DataSetMerger(DataSet dataSet) {
        super(dataSet);
    }

    @Override
    public DataSet merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof DataSet dataSet) {
            return new DataSet(this.publicationInstance.isUserAgreesToTermsAndConditions(),
                               getGeographicalCoverage(this.publicationInstance.getGeographicalCoverage(),
                                                       dataSet.getGeographicalCoverage()),
                               new ReferencedByUris(getUriSet(this.publicationInstance.getReferencedBy(),
                                                              dataSet.getReferencedBy())),
                               mergeCollections(this.publicationInstance.getRelated(),
                                                dataSet.getRelated(), LinkedHashSet::new),
                               new CompliesWithUris(getUriSet(this.publicationInstance.getCompliesWith(),
                                                              dataSet.getCompliesWith())));
        } else {
            return this.publicationInstance;
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
