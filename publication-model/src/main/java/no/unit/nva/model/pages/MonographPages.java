package no.unit.nva.model.pages;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class MonographPages implements Pages {
    private Range introduction;
    private String pages;
    private boolean illustrated;

    public MonographPages() {
    }

    private MonographPages(Builder builder) {
        setIntroduction(builder.introduction);
        setPages(builder.pages);
        setIllustrated(builder.illustrated);
    }

    public Range getIntroduction() {
        return introduction;
    }

    /**
     * Because introductions are not obligatory, this constructor sets null to avoid an empty
     * Range object in the serialized data.
     * @param introduction a range object representing the extent of the introduction or null.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public void setIntroduction(Range introduction) {
        if (isNull(introduction) || isNull(introduction.getBegin())) {
            this.introduction = null;
        } else {
            this.introduction = introduction;
        }
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    public boolean isIllustrated() {
        return illustrated;
    }

    public void setIllustrated(boolean illustrated) {
        this.illustrated = illustrated;
    }

    public static final class Builder {
        private Range introduction;
        private String pages;
        private boolean illustrated;

        public Builder() {
        }

        public Builder withIntroduction(Range introduction) {
            this.introduction = introduction;
            return this;
        }

        public Builder withPages(String pages) {
            this.pages = pages;
            return this;
        }

        public Builder withIllustrated(boolean illustrated) {
            this.illustrated = illustrated;
            return this;
        }

        public MonographPages build() {
            return new MonographPages(this);
        }
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MonographPages)) {
            return false;
        }
        MonographPages monographPages = (MonographPages) o;
        return isIllustrated() == monographPages.isIllustrated()
                && Objects.equals(getIntroduction(), monographPages.getIntroduction())
                && Objects.equals(getPages(), monographPages.getPages());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIntroduction(), getPages(), isIllustrated());
    }
}
