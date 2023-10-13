package no.unit.nva.cristin.mapper;

import java.util.Set;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProduction;

public class ExhibitionProductionBuilder extends AbstractPublicationInstanceBuilder {

    public ExhibitionProductionBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    public ExhibitionProduction build() {
        return super.getCristinObject().getCristinExhibition().toExhibitionProduction();
    }

    @Override
    protected Set<CristinMainCategory> getExpectedType() {
        return Set.of(CristinMainCategory.EXHIBITION);
    }
}
