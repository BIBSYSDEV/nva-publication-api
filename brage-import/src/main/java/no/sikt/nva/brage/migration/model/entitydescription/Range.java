package no.sikt.nva.brage.migration.model.entitydescription;

import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class Range {
    private String begin;
    private String end;

    public Range(String begin, String end) {
        this.begin = begin;
        this.end = end;
    }

    public String getBegin() {
        return begin;
    }

    public void setBegin(String begin) {
        this.begin = begin;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(begin, end);
    }

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
