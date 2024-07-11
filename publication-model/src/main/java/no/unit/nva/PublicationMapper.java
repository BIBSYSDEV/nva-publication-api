package no.unit.nva;

import static no.unit.nva.DatamodelConfig.dataModelObjectMapper;
import static no.unit.nva.api.PublicationContext.getContext;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.model.Publication;

public final class PublicationMapper {

    private PublicationMapper() {
    }


    /**
     * Maps a publication and context to specified type.
     *
     * @param publication  publication.
     * @param context      jsonld context.
     * @param <R>          Type to be converted to.
     * @param responseType Class to be converted to.
     * @return publication response
     */
    public static <R extends WithContext> R convertValue(
        Publication publication, JsonNode context, Class<R> responseType) {
        R response = dataModelObjectMapper.convertValue(publication, responseType);
        response.setContext(context);
        return response;
    }

    /**
     * Maps a publication to specified type with default context.
     *
     * @param publication  publication.
     * @param <R>          Type to be converted to.
     * @param responseType Class to be converted to.
     * @return publication response
     */
    public static <R extends WithContext> R convertValue(
        Publication publication, Class<R> responseType) {
        return convertValue(publication, getContext(publication), responseType);
    }

}
