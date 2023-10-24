package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isMuseum;
import java.util.Set;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProduction;

public class ExhibitionProductionBuilder extends AbstractPublicationInstanceBuilder {

    public ExhibitionProductionBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    public ExhibitionProduction build() {
        if (isMuseum(getCristinObject())) {
            return super.getCristinObject().getCristinExhibition().toExhibitionProduction();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    @Override
    protected Set<CristinMainCategory> getExpectedType() {
        return Set.of(CristinMainCategory.EXHIBITION);
    }
}
