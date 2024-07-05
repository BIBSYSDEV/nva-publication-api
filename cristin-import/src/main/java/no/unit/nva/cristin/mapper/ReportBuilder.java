package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreeLicentiate;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreeMaster;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreePhd;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isReportWorkingPaper;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isResearchReport;
import java.util.Set;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;
import no.unit.nva.model.pages.Pages;

public class ReportBuilder extends AbstractBookReportBuilder {

    public ReportBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (isResearchReport(getCristinObject())) {
            return createReportResearch();
        } else if (isDegreePhd(getCristinObject())) {
            return createDegreePhd();
        } else if (isDegreeMaster(getCristinObject())) {
            return createDegreeMaster();
        } else if (isDegreeLicentiate(getCristinObject())) {
            return createDegreeLicentiate();
        } else if (isReportWorkingPaper(getCristinObject())) {
            return new ReportWorkingPaper(createMonographPages());
        } else {
            throw unknownSecondaryCategory();
        }
    }

    @Override
    protected Set<CristinMainCategory> getExpectedType() {
        return Set.of(CristinMainCategory.REPORT);
    }

    private PublicationInstance<? extends Pages> createDegreeLicentiate() {
        return new DegreeLicentiate(createMonographPages(), null);
    }

    private PublicationInstance<? extends Pages> createReportResearch() {
        return new ReportResearch(createMonographPages());
    }

    private PublicationInstance<? extends Pages> createDegreePhd() {
        return new DegreePhd(createMonographPages(), null, Set.of());
    }

    private PublicationInstance<? extends Pages> createDegreeMaster() {
        return new DegreeMaster(createMonographPages(), null);
    }
}

