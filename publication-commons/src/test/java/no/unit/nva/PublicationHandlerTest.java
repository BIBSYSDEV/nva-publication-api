package no.unit.nva;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.model.PublicationDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

public class PublicationHandlerTest {


    @Test
    public void mappingEmptyStringAsNull() throws JsonProcessingException {
        String json =  "{ \"type\" : \"PublicationDate\", \"year\" : \"2019\", \"month\" : \"\", \"day\" : \"\"}";
        ObjectMapper objectMapper = PublicationHandler.createObjectMapper();
        PublicationDate publicationDate = objectMapper.readValue(json, PublicationDate.class);

        assertNull(publicationDate.getMonth());
        assertNull(publicationDate.getYear());
    }

}
