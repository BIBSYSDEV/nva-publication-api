package no.unit.nva.doi;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import java.net.URI;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.journal.JournalReview;
import no.unit.nva.transformer.dto.DataCiteMetadataDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DataCiteMetadataDtoMapperTest {

    public static final String MAIN_TITLE = "MainTitle";
    public static final String SOME_ISSUE = "1";
    public static final String SOME_ARTICLE_NUMBER = "1";
    public static final String SOME_VOLUME = "1";
    public static final String SOME_YEAR = "1991";
    public static final String SOME_MONTH = "12";
    public static final String SOME_DAY = "52";
    public static final int NOT_THE_CREATOR = 2;
    public static final Organization SOME_PUBLISHER = new Builder().withId(URI.create("https://some-org.example.org")).build();
    public static final String SOME_ARP_ID = "someArpId";
    public static final int THE_CREATOR = 1;
    public static final String SOME_OTHER_ARP_ID = "someOtherArpId";
    public static final String THE_CREATOR_NAME = "TheCreatorName";
    public static final String NOT_THE_CREATOR_NAME = "notTheCreatorName";

    @Test
    public void fromPublicationReturnsDataCiteMetadataDto() {
        Publication publication = samplePublication();

        DataCiteMetadataDto dataCiteMetadataDto = DataCiteMetadataDtoMapper.fromPublication(publication);

        assertThat(dataCiteMetadataDto, doesNotHaveEmptyValues());
    }

    @Test
    public void fromPublicationReturnsDataCiteMetadataDtoWithNullFieldsWhenPublicationIsEmpty() {
        Publication publication = Mockito.mock(Publication.class);
        DataCiteMetadataDto dataCiteMetadataDto = DataCiteMetadataDtoMapper.fromPublication(publication);

        assertThat(dataCiteMetadataDto, notNullValue());
    }

    private Publication samplePublication() {
        return new Publication.Builder()
            .withIdentifier(SortableIdentifier.next())
            .withPublisher(SOME_PUBLISHER)
            .withEntityDescription(createEntityDescriptionWithExpectedAndUnexpectedContributors())
            .build();
    }

    private EntityDescription createEntityDescriptionWithExpectedAndUnexpectedContributors() {
        return new EntityDescription.Builder()
            .withReference(createReference())
            .withMainTitle(MAIN_TITLE)
            .withDate(createPublicationDate())
            .withContributors(List.of(createUnexpectedContributor(), createExpectedContributor()))
            .build();
    }

    private Contributor createExpectedContributor() {
        return new Contributor.Builder()
            .withSequence(THE_CREATOR)
            .withAffiliations(List.of(SOME_PUBLISHER))
            .withIdentity(new Identity.Builder()
                .withArpId(SOME_OTHER_ARP_ID)
                .withName(THE_CREATOR_NAME)
                .build())
            .build();
    }

    private Contributor createUnexpectedContributor() {
        return new Contributor.Builder()
            .withSequence(NOT_THE_CREATOR)
            .withAffiliations(List.of(SOME_PUBLISHER))
            .withIdentity(new Identity.Builder()
                .withArpId(SOME_ARP_ID)
                .withName(NOT_THE_CREATOR_NAME)
                .build())
            .build();
    }

    private PublicationDate createPublicationDate() {
        return new PublicationDate.Builder()
            .withYear(SOME_YEAR)
            .withMonth(SOME_MONTH)
            .withDay(SOME_DAY)
            .build();
    }

    private Reference createReference() {
        return new Reference.Builder()
            .withPublicationInstance(createJournalReview()).build();
    }

    private JournalReview createJournalReview() {
        return new JournalReview.Builder()
            .withIssue(SOME_ISSUE)
            .withArticleNumber(SOME_ARTICLE_NUMBER)
            .withVolume(SOME_VOLUME)
            .build();
    }
}
