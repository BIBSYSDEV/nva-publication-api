package no.unit.nva.publication.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.URI;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import org.junit.jupiter.api.Test;

class IndexDocumentTest {

//    @Test
//    void shouldReturnIndexDocumentWithIdSetToTHeServiceNamespace(){
//        Publication publication = PublicationGenerator.randomPublication();
//        var indexDocument= IndexDocument.fromPublication(publication);
//        var id=indexDocument.getId();
//        var expectedUri=  URI.create(IndexDocument.ID_NAMESPACE+publication.getIdentifier().toString());
//        assertThat(id, is(equalTo(expectedUri)));
//    }

//    @Test
//    void toJsonStringSerializesRequiredFields() throws InvalidIsbnException {
//        Publication publication =
//            sampleBookInABookSeriesWithAPublisher(randomPublicationChannelsUri(), publishingHouseWithUri());
//        assertThat(publication, doesNotHaveEmptyValuesIgnoringFields(IGNORED_PUBLICATION_FIELDS));
//        IndexDocument actualDocument = IndexDocument.fromPublication(publication);
//        assertNotNull(actualDocument);
//        assertNotNull(actualDocument.hasTitle());
//        assertNotNull(actualDocument.hasPublicationType());
//        assertDoesNotThrow(() -> actualDocument.getId().normalize());
//    }


}