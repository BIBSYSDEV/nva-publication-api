package no.unit.nva.publication.testing.http;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import java.net.URI;

public class RandomPersonServiceResponse {
    
    private URI affiliationUri;
    
    public RandomPersonServiceResponse() {
        this(randomUri());
    }
    
    public RandomPersonServiceResponse(URI affiliationUri) {
        this.affiliationUri = affiliationUri;
    }
    
    public static URI randomUri() {
        return URI.create("https://example.org/" + randomInteger());
    }
    
    @Override
    public String toString() {
        return String.format("[{\"orgunitids\": [\"%s\"]}]", affiliationUri);
    }
}
