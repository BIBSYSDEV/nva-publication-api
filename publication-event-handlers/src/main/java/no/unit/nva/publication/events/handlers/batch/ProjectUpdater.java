package no.unit.nva.publication.events.handlers.batch;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.publication.model.business.Resource;

final class ProjectUpdater {

  private static final String CRISTIN = "cristin";
  private static final String PROJECT = "project";
  private final ApiUriProvider uriProvider;

  private ProjectUpdater(ApiUriProvider uriProvider) {
    this.uriProvider = uriProvider;
  }

  static ProjectUpdater create(ApiUriProvider uriProvider) {
    return new ProjectUpdater(uriProvider);
  }

  boolean hasProject(Resource resource, String projectIdentifier) {
    return containsProjectWithId(resource.getProjects(), buildProjectUri(projectIdentifier));
  }

  Resource updateProject(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var oldProjectId = buildProjectUri(request.oldValue());
    var newProjectId = buildProjectUri(request.newValue());
    var projectsAfterUpdate =
        withProjectReplaced(resource.getProjects(), oldProjectId, newProjectId);

    resource.setProjects(projectsAfterUpdate);
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

  private URI buildProjectUri(String projectIdentifier) {
    return uriProvider.uriFrom(CRISTIN, PROJECT, projectIdentifier);
  }
}
