package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;

/**
 * Event containing the contents of a file and additional information that are necessary for processing the file
 * content.
 *
 * <p>In its present form the {@link FileContentsEvent} contains also a field with the name "publicationsOwner" which
 * is  specific to the task of importing Cristin records.  In the future, this should be replaced by a more generic
 * format such as a {@link Map} annotated with "&#64;JsonAnySetter".
 *
 * @param <T> the class modeling the data structure of the file content.
 */
public class FileContentsEvent<T> implements JsonSerializable {
    
    @JsonIgnore
    public static final String FILE_URI = "fileUri";
    @JsonIgnore
    public static final String TIMESTAMP = "timestamp";
    public static final String TOPIC = "topic";
    public static final String SUBTOPIC = "subtopic";
    @JsonIgnore
    protected static final String CONTENTS_FIELD = "contents";
    public static final String CRISTIN_ENTRIES_EVENT_FOLDER = "cristinEntries";
    @JsonProperty(FILE_URI)
    private final URI fileUri;
    @JsonProperty(TIMESTAMP)
    private final Instant timestamp;
    @JsonProperty(CONTENTS_FIELD)
    private final T contents;
    @JsonProperty(TOPIC)
    private final String topic;
    @JsonProperty(SUBTOPIC)
    private final String subtopic;
    
    @JacocoGenerated
    @JsonCreator
    public FileContentsEvent(
        @JsonProperty(TOPIC) String topic,
        @JsonProperty(SUBTOPIC) String subtopic,
        @JsonProperty(FILE_URI) URI fileUri,
        @JsonProperty(TIMESTAMP) Instant timestamp,
        @JsonProperty(CONTENTS_FIELD) T contents) {
        this.topic = topic;
        this.subtopic = subtopic;
        this.fileUri = fileUri;
        this.timestamp = timestamp;
        this.contents = contents;
    }
    
    public static <T> FileContentsEvent<T> fromJson(String jsonString, Class<T> contentsClass) {
        JavaType javaType = constructJavaType(contentsClass);
        return attempt(() -> s3ImportsMapper
                                 .<FileContentsEvent<T>>readValue(jsonString, javaType)).orElseThrow();
    }
    
    @JacocoGenerated
    public String getTopic() {
        return topic;
    }
    
    @JacocoGenerated
    public String getSubtopic() {
        return subtopic;
    }
    
    @JacocoGenerated
    public URI getFileUri() {
        return fileUri;
    }
    
    @JacocoGenerated
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @JacocoGenerated
    public T getContents() {
        return contents;
    }
    
    public EventReference toEventReference(S3Driver s3Driver) throws IOException {
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(this);
        var uri = s3Driver.insertEvent(UnixPath.of(CRISTIN_ENTRIES_EVENT_FOLDER, timestamp.toString()), json);
        return new EventReference(getTopic(), getSubtopic(), uri, timestamp);
    }

    public EventReference toCristinNviEventReference() {
        return new EventReference(getTopic(), getSubtopic(), this.fileUri, timestamp);
    }
    
    private static <T> JavaType constructJavaType(Class<T> contentsClass) {
        return s3ImportsMapper.getTypeFactory().constructParametricType(FileContentsEvent.class, contentsClass);
    }
}
