package no.sikt.nva.brage.migration.model.entitydescription;

import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class Range {
    private String begin;
    private String end;

    @JacocoGenerated
    public Range(String begin, String end) {
        this.begin = begin;
        this.end = end;
    }

    @JacocoGenerated
    public String getBegin() {
        return begin;
    }

    @JacocoGenerated
    public void setBegin(String begin) {
        this.begin = begin;
    }

    @JacocoGenerated
    public String getEnd() {
        return end;
    }

    @JacocoGenerated
    public void setEnd(String end) {
        this.end = end;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(begin, end);
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
        Range range = (Range) o;
        return Objects.equals(begin, range.begin)
               && Objects.equals(end, range.end);
    }

}
