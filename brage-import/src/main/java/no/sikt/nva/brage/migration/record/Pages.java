package no.sikt.nva.brage.migration.record;

import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class Pages {

    private String bragePages;
    private Range range;

    @SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
    private String pages;

    public Pages() {

    }

    public Pages(String bragePages, Range range, String pages) {
        this.bragePages = bragePages;
        this.range = range;
        this.pages = pages;
    }

    @JacocoGenerated
    public String getBragePages() {
        return bragePages;
    }

    @JacocoGenerated
    public void setBragePages(String bragePages) {
        this.bragePages = bragePages;
    }

    @JacocoGenerated
    public Range getRange() {
        return range;
    }

    @JacocoGenerated
    public void setRange(Range range) {
        this.range = range;
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(bragePages, range, pages);
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
        Pages pages = (Pages) o;
        return Objects.equals(bragePages, pages.bragePages)
               && Objects.equals(range, pages.range)
               && Objects.equals(this.pages, pages.pages);
    }
}
