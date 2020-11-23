package no.unit.nva.publication.doi.update.dto;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DoiUpdateDtoTest {

    public static final URI EXAMPLE_DOI = URI.create("https://example.net/doi/prefix/suffix");
    public static final URI EXAMPLE_PUBLICATION_ID = URI.create("https://example.net/env/publication/id");
    public static final Instant EXAMPLE_MODIFIED_DATE = Instant.now();

    @Test
    void builderPopulatesAllFieldsViaConstructor() {
        DoiUpdateDto doiUpdateDto = createDoiUpdateDtoWithDoi();
        assertThat(doiUpdateDto, doesNotHaveNullOrEmptyFields());
    }

    @Test
    void getDoiReturnsOptionalDoiWhenPresent() {
        DoiUpdateDto doiUpdateDto = createDoiUpdateDtoWithDoi();
        Optional<URI> doi = doiUpdateDto.getDoi();
        assertThat(doi.isPresent(), is(equalTo(true)));
        assertThat(doi.get(), is(equalTo(EXAMPLE_DOI)));
    }

    @Test
    void getDoiReturnsOptionalEmptyWhenDoiIsNotPresent() {
        DoiUpdateDto doiUpdateDto = createDoiUpdateDtoWithoutDoi();
        Optional<URI> doi = doiUpdateDto.getDoi();
        assertThat(doi.isEmpty(), is(equalTo(true)));
    }

    private DoiUpdateDto createDoiUpdateDtoWithDoi() {
        return ImmutableDoiUpdateDto.builder()
            .withDoi(EXAMPLE_DOI)
            .withPublicationId(EXAMPLE_PUBLICATION_ID)
            .withModifiedDate(EXAMPLE_MODIFIED_DATE)
            .build();
    }

    private DoiUpdateDto createDoiUpdateDtoWithoutDoi() {
        return DoiUpdateDto.builder()
            .withPublicationId(EXAMPLE_PUBLICATION_ID)
            .withModifiedDate(EXAMPLE_MODIFIED_DATE)
            .build();
    }
}