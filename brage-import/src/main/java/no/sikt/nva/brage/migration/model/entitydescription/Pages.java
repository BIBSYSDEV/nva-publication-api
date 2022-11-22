package no.sikt.nva.brage.migration.model.entitydescription;

import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
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

    public String getBragePages() {
        return bragePages;
    }

    public void setBragePages(String bragePages) {
        this.bragePages = bragePages;
    }

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bragePages, range, pages);
    }

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
