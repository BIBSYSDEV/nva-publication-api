package no.unit.nva.publication.external.services;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * A simple model of a Bare response, to avoid using resource files.
 */
public class PersonApiResponseBodyMock {
    
    List<PersonApiEntry> personApiEntries;
    
    public static PersonApiResponseBodyMock createResponse(String feideId, URI... orgUnitIds) {
        var result = new PersonApiResponseBodyMock();
        createEntry(feideId, result, orgUnitIds);
        return result;
    }
    
    @JsonValue
    //Bare returns a Json Array as a search result and not a JSON object.
    public String toString() {
        return attempt(() -> dtoObjectMapper.writeValueAsString(personApiEntries)).orElseThrow();
    }
    
    private static void createEntry(String feideId, PersonApiResponseBodyMock result, URI[] orgUnitIds) {
        if (nonNull(orgUnitIds) && orgUnitIds.length > 0) {
            var entry = PersonApiEntry.create(feideId, orgUnitIds);
            result.personApiEntries = List.of(entry);
        } else {
            result.personApiEntries = Collections.emptyList();
        }
    }
}
