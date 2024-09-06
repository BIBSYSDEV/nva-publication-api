package no.unit.nva;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.ImportDetail;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.ResearchProject;

public interface WithMetadata extends PublicationBase {

    EntityDescription getEntityDescription();

    void setEntityDescription(EntityDescription entityDescription);

    List<ResearchProject> getProjects();

    void setProjects(List<ResearchProject> projects);

    List<URI> getSubjects();

    void setSubjects(List<URI> subjects);

    List<Funding> getFundings();

    void setFundings(List<Funding> fundings);

    String getRightsHolder();

    void setRightsHolder(String rightsHolder);

    List<ImportDetail> getImportDetails();

    void setImportDetails(Collection<ImportDetail> importDetails);
}
