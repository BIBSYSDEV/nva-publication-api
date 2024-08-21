package no.unit.nva.model.associatedartifacts.file;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.Instant;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.ImportUploadDetails.Source;

public class UploadDetailsDeserializer extends StdDeserializer<UploadDetails> {

    public static final String SCOPUS_IMPORT_USERNAME = "central-import";

    public UploadDetailsDeserializer() {
        this(null);
    }

    public UploadDetailsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public UploadDetails deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        Instant uploadedDate = null;
        if (node.has("uploadedDate")) {
            uploadedDate = Instant.parse(node.get("uploadedDate").asText());
        }
        if (node.has("uploadedBy")) {
            var uploadedBy = node.get("uploadedBy").asText();
            var usernameOrArchive = uploadedBy.split("@")[0];
            if (attempt(() -> Integer.parseInt(usernameOrArchive)).isSuccess()) {
                return new UserUploadDetails(new Username(uploadedBy), uploadedDate);
            } else {
                if (uploadedBy.contains(SCOPUS_IMPORT_USERNAME)) {
                    return new ImportUploadDetails(Source.SCOPUS, null, uploadedDate);
                } else {
                    return new ImportUploadDetails(Source.BRAGE, usernameOrArchive, uploadedDate);
                }
            }
        }
        if (node.has("archive")) {
            var archive = node.get("archive").asText();
            return new ImportUploadDetails(Source.BRAGE, archive, uploadedDate);
        } else {
            return null;
        }
    }
}