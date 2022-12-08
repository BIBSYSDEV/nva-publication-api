package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public final class PublicationDateNva {

    private String year;
    private String month;
    private String day;

    public PublicationDateNva() {
        
    }

    private PublicationDateNva(Builder builder) {
        setYear(builder.year);
        setMonth(builder.month);
        setDay(builder.day);
    }

    @JsonProperty("year")
    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    @JsonProperty("month")
    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    @JsonProperty("day")
    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getYear(), getMonth(), getDay());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PublicationDateNva that = (PublicationDateNva) o;
        return Objects.equals(getYear(), that.getYear())
               && Objects.equals(getMonth(), that.getMonth())
               && Objects.equals(getDay(), that.getDay());
    }

    public static final class Builder {

        private String year;
        private String month;
        private String day;

        public Builder() {
        }

        public Builder withYear(String year) {
            this.year = year;
            return this;
        }

        @JacocoGenerated
        public Builder withMonth(String month) {
            this.month = month;
            return this;
        }

        @JacocoGenerated
        public Builder withDay(String day) {
            this.day = day;
            return this;
        }

        public PublicationDateNva build() {
            return new PublicationDateNva(this);
        }
    }
}
