package no.unit.nva.cristin.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.time.duration.DefinedDuration;
import no.unit.nva.model.time.duration.NullDuration;
import no.unit.nva.model.time.duration.UndefinedDuration;
import no.unit.nva.publication.model.utils.CustomerService;
import no.unit.nva.publication.utils.CristinUnitsUtil;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

public class ArtisticMapperTest {

    @Test
    void shouldExtractDurationAsDefinedDurationWhenTimeUnitIsPresent() {
        var cristinObject = CristinDataGenerator.randomArtisticProduction(CristinSecondaryCategory.FILM_PRODUCTION);
        var mapper = getMapper(cristinObject);
        var publicationInstance =
            (MovingPicture) mapper.generatePublication().getEntityDescription().getReference().getPublicationInstance();
        assertThat(publicationInstance.getDuration(), is(instanceOf(DefinedDuration.class)));
    }

    @Test
    void shouldExtractDurationAsUndefinedDurationWhenMissingTimeUnit() {
        var cristinObject = CristinDataGenerator.randomArtisticProduction(CristinSecondaryCategory.FILM_PRODUCTION);
        cristinObject.getCristinArtisticProduction().setArtisticProductionTimeUnit(null);
        var mapper = getMapper(cristinObject);
        var publicationInstance =
            (MovingPicture) mapper.generatePublication().getEntityDescription().getReference().getPublicationInstance();
        assertThat(publicationInstance.getDuration(), is(instanceOf(UndefinedDuration.class)));
    }

    @Test
    void shouldExtractDurationAsNullDurationWhenMissingDuration() {
        var cristinObject = CristinDataGenerator.randomArtisticProduction(CristinSecondaryCategory.FILM_PRODUCTION);
        cristinObject.getCristinArtisticProduction().setDuration(null);
        var mapper = getMapper(cristinObject);
        var publicationInstance =
            (MovingPicture) mapper.generatePublication().getEntityDescription().getReference().getPublicationInstance();
        assertThat(publicationInstance.getDuration(), is(instanceOf(NullDuration.class)));
    }

    private static CristinMapper getMapper(CristinObject cristinObject) {
        return new CristinMapper(cristinObject, mock(CristinUnitsUtil.class), mock(S3Client.class),
                                 mock(UriRetriever.class), mock(CustomerService.class));
    }
}
