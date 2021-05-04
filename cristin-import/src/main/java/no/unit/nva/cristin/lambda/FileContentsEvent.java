package no.unit.nva.cristin.lambda;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;

public class FileContentsEvent<T> {

    @JsonIgnore
    protected static final String CONTENTS_FIELD = "contents";
    @JsonIgnore
    protected static final String PUBLICATIONS_OWNER_FIELD = "publicationsOwner";
    @JsonProperty(CONTENTS_FIELD)
    private final T contents;
    @JsonProperty(PUBLICATIONS_OWNER_FIELD)
    private final String publicationsOwner;

    @JacocoGenerated
    @JsonCreator
    public FileContentsEvent(@JsonProperty(CONTENTS_FIELD) T contents,
                             @JsonProperty(PUBLICATIONS_OWNER_FIELD) String publicationsOwner) {
        this.contents = contents;
        this.publicationsOwner = publicationsOwner;
    }

    public FileContentsEvent(T contents, ImportRequest input) {
        this.contents = contents;
        this.publicationsOwner = input.getPublicationsOwner();
    }

    public static <T> FileContentsEvent<T> fromJson(String jsonString, Class<T> contentsClass) {
        JavaType javaType = constructJavaType(contentsClass);
        return attempt(() -> JsonUtils.objectMapperNoEmpty
                                 .<FileContentsEvent<T>>readValue(jsonString, javaType)).orElseThrow();
    }

    public T getContents() {
        return contents;
    }

    public String getPublicationsOwner() {
        return publicationsOwner;
    }

    private static <T> JavaType constructJavaType(Class<T> contentsClass) {
        return JsonUtils.objectMapperNoEmpty.getTypeFactory()
                   .constructParametricType(FileContentsEvent.class, contentsClass);
    }
}
