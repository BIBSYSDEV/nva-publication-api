package no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation;

import org.junit.jupiter.api.Test;

import static no.unit.nva.model.testing.PublicationContextBuilder.randomPublishingHouse;
import static no.unit.nva.model.testing.PublicationInstanceBuilder.randomMonographPages;
import static no.unit.nva.model.testing.RandomUtils.randomPublicationDate;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn13;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@Deprecated
class LiteraryArtsManifestationMigrationTest {

    @Test
    void shouldCreateLiteraryArtsMonographWhenIsbnIsString() {
        var literaryArtsMonograph = new LiteraryArtsMonograph(randomPublishingHouse(), randomPublicationDate(),
                randomIsbn13(), randomMonographPages());
        assertThat(literaryArtsMonograph.getIsbnList(), hasSize(1));
    }


    @Test
    void shouldCreateLiteraryArtsAudioVisualWhenIsbnIsString() {
        var literaryArtsAudioVisual = new LiteraryArtsAudioVisual(randomLiteraryArtsAudioVisualSubtype(),
                randomPublishingHouse(), randomPublicationDate(), randomIsbn13(), randomInteger());
        assertThat(literaryArtsAudioVisual.getIsbnList(), hasSize(1));
    }

    private LiteraryArtsAudioVisualSubtype randomLiteraryArtsAudioVisualSubtype() {
        return new LiteraryArtsAudioVisualSubtype(LiteraryArtsAudioVisualSubtypeEnum.PODCAST);
    }
}