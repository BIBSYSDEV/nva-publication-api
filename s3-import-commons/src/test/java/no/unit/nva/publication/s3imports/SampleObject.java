package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.javafaker.Faker;
import java.util.Objects;
import java.util.Random;
import no.unit.nva.commons.json.JsonSerializable;

public class SampleObject implements JsonSerializable {
    
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final Faker FAKER = Faker.instance(RANDOM);
    @JsonProperty("id")
    private final int id;
    @JsonProperty("field1")
    private final String field1;
    @JsonProperty("field2")
    private final String field2;
    
    @JsonCreator
    public SampleObject(@JsonProperty("id") int id,
                        @JsonProperty("field1") String field1,
                        @JsonProperty("field2") String field2) {
        this.id = id;
        this.field1 = field1;
        this.field2 = field2;
    }
    
    public static SampleObject random() {
        return new SampleObject(RANDOM.nextInt(), randomString(), randomString());
    }
    
    public static SampleObject fromJson(String json) {
        return attempt(() -> s3ImportsMapper.readValue(json, SampleObject.class)).orElseThrow();
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId(), getField1(), getField2());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SampleObject)) {
            return false;
        }
        SampleObject that = (SampleObject) o;
        return getId() == that.getId()
               && Objects.equals(getField1(), that.getField1())
               && Objects.equals(getField2(), that.getField2());
    }
    
    public String toString() {
        return toJsonString();
    }
    
    public int getId() {
        return id;
    }
    
    public String getField1() {
        return field1;
    }
    
    public String getField2() {
        return field2;
    }
    
    private static String randomString() {
        return FAKER.lorem().sentence(2);
    }
}
