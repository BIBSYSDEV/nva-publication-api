package no.unit.nva.cristin.mapper.nva;

import java.net.URI;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public final class InformationMaterialBuilder {

    public static final String PUBLISHER = "publisher";
    public static final String PUBLICATION_CHANNELS_V2 = "publication-channels-v2";
    public static final String NIFU_CHANNEL_REGISTER_IDENTIFIER = "42997513-9974-48C2-A9A6-6AE1AC1F908D";

    private InformationMaterialBuilder() {
    }

    public static Report buildPublicationContext(CristinObject cristinObject)
        throws InvalidIssnException, InvalidUnconfirmedSeriesException {
        return new Report.Builder()
                   .withPublisher(new Publisher(hardcodedNifuPublisher(cristinObject)))
                   .build();
    }

    private static URI hardcodedNifuPublisher(CristinObject cristinObject) {
        return UriWrapper.fromHost(new Environment().readEnv("DOMAIN_NAME"))
                   .addChild(PUBLICATION_CHANNELS_V2)
                   .addChild(PUBLISHER)
                   .addChild(NIFU_CHANNEL_REGISTER_IDENTIFIER)
                   .addChild(cristinObject.getPublicationYear().toString())
                   .getUri();
    }
}
