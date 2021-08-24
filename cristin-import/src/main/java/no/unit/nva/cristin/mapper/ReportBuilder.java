package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinMainCategory.isReport;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreeMaster;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreePhd;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isResearchReport;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.pages.Pages;

public class ReportBuilder extends AbstractBookReportBuilder {

    private final CristinObject cristinObject;

    public ReportBuilder(CristinObject cristinObject) {
        super();
        if (!isReport(cristinObject)) {
            throw new IllegalStateException("Is not report");
        }
        this.cristinObject = cristinObject;
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (isResearchReport(cristinObject)) {
            return createReportResearch();
        } else if (isDegreePhd(cristinObject)) {
            return createDegreePhd();
        } else if (isDegreeMaster(cristinObject)) {
            return createDegreeMaster();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    @Override
    protected CristinObject getCristinObject() {
        return this.cristinObject;
    }

    private PublicationInstance<? extends Pages> createReportResearch() {
        return new ReportResearch.Builder().build();
    }

    private PublicationInstance<? extends Pages> createDegreePhd() {
        return new DegreePhd.Builder()
            .withPages(createMonographPages())
            .build();
    }

    private PublicationInstance<? extends Pages> createDegreeMaster() {
        return new DegreeMaster.Builder()
            .withPages(createMonographPages())
            .build();
    }
}

