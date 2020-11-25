package no.unit.nva.publication.doi.update.dto;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DoiUpdateHolderTest {

    public static final String EXAMPLE_TYPE = "doi.updateDoi";
    public static final URI EXAMPLE_DOI = URI.create("https://example.net/doi/prefix/suffix");
    public static final Instant EXAMPLE_NOW = Instant.now();
    public static final URI EXAMPLE_PUBLICATION_ID = URI.create("https://example.net/env/publication/id2");
    public static final DoiUpdateDto EXAMPLE_ITEM = new DoiUpdateDto(
        EXAMPLE_DOI,
        EXAMPLE_PUBLICATION_ID,
        EXAMPLE_NOW);

    @Test
    void constructorPopulatesAllFields() {
        DoiUpdateHolder doiUpdateHolder = createPopulatedDoiUpdateHolder();
        assertThat(doiUpdateHolder, doesNotHaveNullOrEmptyFields());
    }

    @Test
    void hasItemReturnsTrueWhenItemIsSet() {
        DoiUpdateHolder doiUpdateHolder = createPopulatedDoiUpdateHolder();
        assertThat(doiUpdateHolder.hasItem(), is(equalTo(true)));
    }

    @Test
    void hasItemReturnsFalseWhenItemIsNotSet() {
        DoiUpdateHolder doiUpdateHolder = createPopulatedDoiUpdateHolder();
        doiUpdateHolder.item = null;
        assertThat(doiUpdateHolder.hasItem(), is(equalTo(false)));
    }

    @Test
    void getItemReturnsItem() {
        DoiUpdateHolder doiUpdateHolder = createPopulatedDoiUpdateHolder();
        assertThat(doiUpdateHolder.getItem(), notNullValue());
    }


    private DoiUpdateHolder createPopulatedDoiUpdateHolder() {
        return new DoiUpdateHolder(EXAMPLE_TYPE, EXAMPLE_ITEM);
    }
}