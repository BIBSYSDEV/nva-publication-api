package no.unit.nva.publication.events.handlers.batch.updates;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.publication.events.handlers.batch.ManualUpdateType;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.paths.UriWrapper;

public final class ProjectUpdate extends ResourceUpdate {

  private static final String CRISTIN_PATH = "cristin";
  private static final String PROJECT_PATH = "project";

  public ProjectUpdate(ResourceService resourceService) {
    super(resourceService);
  }

  @Override
  public ManualUpdateType type() {
    return ManualUpdateType.PROJECT;
  }

  @Override
  public boolean matches(Resource resource, ManuallyUpdatePublicationsRequest request) {
    return containsProjectWithId(resource.getProjects(), projectUri(request.oldValue()));
  }

  @Override
  protected Resource update(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var oldProjectId = projectUri(request.oldValue());
    var newProjectId = projectUri(request.newValue());

    resource.setProjects(withProjectReplaced(resource.getProjects(), oldProjectId, newProjectId));
    return resource;
  }

  private List<ResearchProject> withProjectReplaced(
      Collection<ResearchProject> projects, URI oldProjectId, URI newProjectId) {
    if (oldProjectId.equals(newProjectId)) {
      return List.copyOf(projects);
    }
    return containsProjectWithId(projects, newProjectId)
        ? withoutProjectWithId(projects, oldProjectId)
        : withFirstProjectIdReplaced(projects, oldProjectId, newProjectId);
  }

  private boolean containsProjectWithId(Collection<ResearchProject> projects, URI projectId) {
    return projects.stream().anyMatch(project -> projectId.equals(project.getId()));
  }

  private List<ResearchProject> withoutProjectWithId(
      Collection<ResearchProject> projects, URI projectId) {
    return projects.stream().filter(project -> !projectId.equals(project.getId())).toList();
  }

  private List<ResearchProject> withFirstProjectIdReplaced(
      Collection<ResearchProject> projects, URI oldProjectId, URI newProjectId) {
    var replacedProjects = new ArrayList<ResearchProject>();
    for (var project : projects) {
      if (!oldProjectId.equals(project.getId())) {
        replacedProjects.add(project);
      } else if (!containsProjectWithId(replacedProjects, newProjectId)) {
        replacedProjects.add(projectWithId(project, newProjectId));
      }
    }
    return List.copyOf(replacedProjects);
  }

  private ResearchProject projectWithId(ResearchProject project, URI projectId) {
    return new ResearchProject.Builder()
        .withId(projectId)
        .withName(project.getName())
        .withApprovals(project.getApprovals())
        .build();
  }

  private URI projectUri(String projectIdentifier) {
    return UriWrapper.fromHost(API_HOST)
        .addChild(CRISTIN_PATH, PROJECT_PATH, projectIdentifier)
        .getUri();
  }
}
