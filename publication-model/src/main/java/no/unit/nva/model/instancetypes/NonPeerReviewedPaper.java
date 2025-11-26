package no.unit.nva.model.instancetypes;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JacocoGenerated;

public class NonPeerReviewedPaper implements PublicationInstance<Range> {
    private Range pages;

    protected NonPeerReviewedPaper() {
        super();
    }

    protected NonPeerReviewedPaper(Range pages) {
        super();
        this.pages = pages;
    }

    @Override
    public Range getPages() {
        return this.pages;
    }

    @Override
    public List<URI> extractPublicationContextUris() {
        return Collections.emptyList();
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NonPeerReviewedPaper)) {
            return false;
        }
        NonPeerReviewedPaper that = (NonPeerReviewedPaper) o;
        return Objects.equals(getPages(), that.getPages());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getPages());
    }
}
