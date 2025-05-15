package no.unit.nva;

import no.unit.nva.model.instancetypes.degree.ArtisticDegreePhd;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.OtherStudentWork;

public class PublicationUtil {

    public static final Class<?>[] PROTECTED_DEGREE_INSTANCE_TYPES = {
        DegreeLicentiate.class,
        DegreeBachelor.class,
        DegreeMaster.class,
        DegreePhd.class,
        ArtisticDegreePhd.class,
        OtherStudentWork.class,
    };
}
