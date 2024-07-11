package no.unit.nva.model.instancetypes.artistic.architecture;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.NullPages;
import nva.commons.core.JacocoGenerated;

import java.util.List;
import java.util.Objects;

import static no.unit.nva.model.util.SerializationUtils.nullListAsEmpty;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Architecture implements PublicationInstance<NullPages> {

    public static final String SUBTYPE_FIELD = "subtype";
    public static final String DESCRIPTION_FIELD = "description";
    public static final String OUTPUT_FIELD = "architectureOutput";

    @JsonProperty(SUBTYPE_FIELD)
    private final ArchitectureSubtype subtype;
    @JsonProperty(DESCRIPTION_FIELD)
    private final String description;

    // TODO: migrate to output to match other classes
    @JsonProperty(OUTPUT_FIELD)
    private final List<ArchitectureOutput> architectureOutput;

    public Architecture(@JsonProperty(SUBTYPE_FIELD) ArchitectureSubtype subtype,
                        @JsonProperty(DESCRIPTION_FIELD) String description,
                        @JsonProperty(OUTPUT_FIELD) List<ArchitectureOutput> architectureOutput) {
        this.subtype = subtype;
        this.description = description;
        this.architectureOutput = nullListAsEmpty(architectureOutput);
    }


    @JsonGetter
    @Override
    public NullPages getPages() {
        return NullPages.NULL_PAGES;
    }

    public ArchitectureSubtype getSubtype() {
        return subtype;
    }

    public String getDescription() {
        return description;
    }

    public List<ArchitectureOutput> getArchitectureOutput() {
        return architectureOutput;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Architecture)) {
            return false;
        }
        Architecture that = (Architecture) o;
        return Objects.equals(getSubtype(), that.getSubtype())
               && Objects.equals(getDescription(), that.getDescription())
               && Objects.equals(getArchitectureOutput(), that.getArchitectureOutput());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getSubtype(), getDescription(), getArchitectureOutput());
    }
}
