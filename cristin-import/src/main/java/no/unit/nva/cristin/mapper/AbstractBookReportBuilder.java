package no.unit.nva.cristin.mapper;

import java.util.Optional;
import no.unit.nva.model.pages.MonographPages;

public abstract class AbstractBookReportBuilder extends AbstractPublicationInstanceBuilder {

    public AbstractBookReportBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    protected MonographPages createMonographPages() {
        return new MonographPages.Builder()
                   .withPages(extractNumberOfPages())
                   .build();
    }
    
    private String extractNumberOfPages() {
        return Optional.ofNullable(getCristinObject())
                   .map(CristinObject::getBookOrReportMetadata)
                   .map(CristinBookOrReportMetadata::getNumberOfPages)
                   .orElse(null);
    }
}
