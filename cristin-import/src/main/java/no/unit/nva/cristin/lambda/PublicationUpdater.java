package no.unit.nva.cristin.lambda;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.MediaContribution;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.AcademicLiteratureReview;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.PopularScienceArticle;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;
import no.unit.nva.model.pages.Pages;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.GodClass"})
public final class PublicationUpdater {

    private PublicationUpdater() {
    }

    public static PublicationRepresentations update(PublicationRepresentations publicationRepresentations) {
        var existingPublication = publicationRepresentations.getExistingPublication();
        existingPublication.setAssociatedArtifacts(updatedAssociatedLinks(publicationRepresentations));
        existingPublication.setEntityDescription(updatedEntityDescription(publicationRepresentations));
        existingPublication.setFundings(updateFundings(publicationRepresentations));
        existingPublication.setProjects(updateProjects(publicationRepresentations));
        existingPublication.setHandle(updateHandle(publicationRepresentations));
        existingPublication.setLink(updateLink(publicationRepresentations));
        return publicationRepresentations;
    }

    private static List<ResearchProject> updateProjects(PublicationRepresentations publicationRepresentations) {
        var existingProjects = publicationRepresentations.getExistingPublication().getProjects();
        var incomingProjects = publicationRepresentations.getIncomingPublication().getProjects();
        return shouldBeUpdated(existingProjects, incomingProjects) ? incomingProjects : existingProjects;
    }

    private static boolean shouldBeUpdated(List<?> oldList, List<?> newList) {
        return nonNull(oldList) && oldList.isEmpty() && !newList.isEmpty();
    }

    private static boolean shouldBeUpdated(Object oldEntry, Object newEntry) {
        return isNull(oldEntry) && nonNull(newEntry);
    }

    private static URI updateLink(PublicationRepresentations publicationRepresentations) {
        var existingLink = publicationRepresentations.getExistingPublication().getLink();
        var incomingLink = publicationRepresentations.getIncomingPublication().getLink();
        return shouldBeUpdated(existingLink, incomingLink) ? incomingLink : existingLink;
    }

    private static URI updateHandle(PublicationRepresentations publicationRepresentations) {
        var existingHandle = publicationRepresentations.getExistingPublication().getHandle();
        var incomingHandle = publicationRepresentations.getIncomingPublication().getHandle();
        return shouldBeUpdated(existingHandle, incomingHandle) ? incomingHandle : existingHandle;
    }

    private static List<Funding> updateFundings(PublicationRepresentations publicationRepresentations) {
        var existingFundings = publicationRepresentations.getExistingPublication().getFundings();
        var incomingFundings = publicationRepresentations.getIncomingPublication().getFundings();
        return shouldBeUpdated(existingFundings, incomingFundings) ? incomingFundings : existingFundings;
    }

    private static EntityDescription updatedEntityDescription(PublicationRepresentations publicationRepresentations) {
        return publicationRepresentations.getExistingPublication().getEntityDescription().copy()
                   .withReference(updateReference(publicationRepresentations))
                   .withTags(updatedTags(publicationRepresentations))
                   .withContributors(updateContributors(publicationRepresentations))
                   .withNpiSubjectHeading(updateNpiSubjectHeading(publicationRepresentations))
                   .build();
    }

    private static String updateNpiSubjectHeading(PublicationRepresentations publicationRepresentations) {
        var existingNpiSubjectHeading = getNpiSubjectHeading(publicationRepresentations.getExistingPublication());
        var incomingNpiSubjectHeading = getNpiSubjectHeading(publicationRepresentations.getIncomingPublication());
        return shouldBeUpdated(existingNpiSubjectHeading, incomingNpiSubjectHeading)
                   ? incomingNpiSubjectHeading
                   : existingNpiSubjectHeading;
    }

    private static String getNpiSubjectHeading(Publication existingPublication) {
        return existingPublication.getEntityDescription().getNpiSubjectHeading();
    }

    private static List<Contributor> updateContributors(PublicationRepresentations publicationRepresentations) {
        var existingContributors = getContributors(publicationRepresentations.getExistingPublication());
        var incomingContributors = getContributors(publicationRepresentations.getIncomingPublication());
        if (isDegree(publicationRepresentations.getExistingPublication())) {
            return updateExistingContributorsIdWhenMissing(incomingContributors, existingContributors);
        } else {
            return listShouldBeOverwritten(existingContributors, incomingContributors)
                       ? incomingContributors
                       : existingContributors;
        }
    }

    private static List<Contributor> updateExistingContributorsIdWhenMissing(List<Contributor> incomingContributors,
                                                                             List<Contributor> existingContributors) {
        incomingContributors.stream()
            .filter(incomingContributor -> nonNull(incomingContributor.getIdentity().getId()))
            .forEach(incomingContributor -> setIdForExistingContributorWhenMissing(incomingContributor,
                                                                                   existingContributors));
        return existingContributors;
    }

    private static void setIdForExistingContributorWhenMissing(Contributor incomingContributor, List<Contributor> existingContributors) {
        var contributorMissingId = existingContributors.stream()
                    .filter(existingContributor -> haveSameName(incomingContributor, existingContributor))
                    .filter(PublicationUpdater::hasNoId)
                    .findFirst();
        contributorMissingId.ifPresent(existingContributor -> existingContributor.getIdentity().setId(incomingContributor.getIdentity().getId()));
    }

    private static boolean hasNoId(Contributor existingContributor) {
        return isNull(existingContributor.getIdentity().getId());
    }

    private static boolean haveSameName(Contributor incomingContributor, Contributor existingContributor) {
        return existingContributor.getIdentity().getName().equals(incomingContributor.getIdentity().getName());
    }

    private static boolean isDegree(Publication publication) {
        return publication.getEntityDescription().getReference().getPublicationContext() instanceof Degree;
    }

    private static List<Contributor> getContributors(Publication publication) {
        return publication.getEntityDescription().getContributors();
    }

    private static List<String> updatedTags(PublicationRepresentations publicationRepresentations) {
        var existingTags = publicationRepresentations.getExistingPublication().getEntityDescription().getTags();
        var incomingTags = publicationRepresentations.getIncomingPublication().getEntityDescription().getTags();
        var list = new ArrayList<>(existingTags);
        list.addAll(incomingTags);
        return list.stream().distinct().toList();
    }

    private static Reference updateReference(PublicationRepresentations publicationRepresentations) {
        var existinReference = publicationRepresentations
                                   .getExistingPublication()
                                   .getEntityDescription()
                                   .getReference();
        existinReference.setPublicationContext(updatePublicationContext(publicationRepresentations));
        existinReference.setPublicationInstance(updatePublicationInstance(publicationRepresentations));
        existinReference.setDoi(updateDoi(publicationRepresentations));
        return existinReference;

    }

    private static URI updateDoi(PublicationRepresentations publicationRepresentations) {
        var existingDoi = getDoi(publicationRepresentations.getExistingPublication());
        var incomingDoi = getDoi(publicationRepresentations.getIncomingPublication());
        return shouldBeUpdated(existingDoi, incomingDoi) ? incomingDoi : existingDoi;
    }

    private static URI getDoi(Publication publication) {
        return publication.getEntityDescription().getReference().getDoi();
    }

    private static PublicationInstance<? extends Pages> updatePublicationInstance(
        PublicationRepresentations publicationRepresentations) {
        var existingPublicationInstance = getPublicationInstance(publicationRepresentations.getExistingPublication());
        var incomingPublicationInstance = getPublicationInstance(publicationRepresentations.getIncomingPublication());

        if (existingPublicationInstance instanceof JournalArticle existingJournalArticle
            && incomingPublicationInstance instanceof JournalArticle incomingJournalArticle
            && existingPublicationInstance.getInstanceType().equals(incomingPublicationInstance.getInstanceType())) {
            return updateJournalArticle(existingJournalArticle, incomingJournalArticle);
        } else {
            return existingPublicationInstance;
        }
    }

    @JacocoGenerated
    private static PublicationInstance<? extends Pages> updateJournalArticle(JournalArticle existingJournalArticle,
                                                                             JournalArticle incomingJournalArticle) {
        return switch (existingJournalArticle) {
            case ProfessionalArticle professionalArticle ->
                getProfessionalArticle(existingJournalArticle, incomingJournalArticle);
            case PopularScienceArticle popularScienceArticle ->
                getPopularScienceArticle(existingJournalArticle, incomingJournalArticle);
            case AcademicArticle academicArticle ->
                getAcademicArticle(existingJournalArticle, incomingJournalArticle);
            case AcademicLiteratureReview academicLiteratureReview ->
                getAcademicLiteratureReview(existingJournalArticle, incomingJournalArticle);
            case null, default -> existingJournalArticle;
        };
    }

    @JacocoGenerated
    private static AcademicLiteratureReview getAcademicLiteratureReview(JournalArticle existingJournalArticle,
                                                                        JournalArticle incomingJournalArticle) {
        return new AcademicLiteratureReview(existingJournalArticle.getPages(),
                                            existingJournalArticle.getVolume(),
                                            existingJournalArticle.getIssue(),
                                            incomingJournalArticle.getArticleNumber());
    }

    @JacocoGenerated
    private static AcademicArticle getAcademicArticle(JournalArticle existingJournalArticle,
                                                      JournalArticle incomingJournalArticle) {
        return new AcademicArticle(existingJournalArticle.getPages(), existingJournalArticle.getVolume(),
                                   existingJournalArticle.getIssue(),
                                   incomingJournalArticle.getArticleNumber());
    }

    @JacocoGenerated
    private static PopularScienceArticle getPopularScienceArticle(JournalArticle existingJournalArticle,
                                                                  JournalArticle incomingJournalArticle) {
        return new PopularScienceArticle(existingJournalArticle.getPages(), existingJournalArticle.getVolume(),
                                         existingJournalArticle.getIssue(),
                                         incomingJournalArticle.getArticleNumber());
    }

    @JacocoGenerated
    private static ProfessionalArticle getProfessionalArticle(JournalArticle existingJournalArticle,
                                                              JournalArticle incomingJournalArticle) {
        return new ProfessionalArticle(existingJournalArticle.getPages(), existingJournalArticle.getVolume(),
                                       existingJournalArticle.getIssue(),
                                       incomingJournalArticle.getArticleNumber());
    }

    private static PublicationInstance<? extends Pages> getPublicationInstance(Publication publication) {
        return publication
                   .getEntityDescription()
                   .getReference()
                   .getPublicationInstance();
    }

    private static PublicationContext updatePublicationContext(PublicationRepresentations publicationRepresentations) {
        var existingPublicationContext = getPublicationContext(publicationRepresentations.getExistingPublication());
        var incomingPublicationContext = getPublicationContext(publicationRepresentations.getIncomingPublication());
        return updatePublicationContext(existingPublicationContext, incomingPublicationContext);
    }

    private static PublicationContext updatePublicationContext(PublicationContext existingPublicationContext,
                                                            PublicationContext incomingPublicationContext) {
        if (existingPublicationContext instanceof Event existingEvent
            && incomingPublicationContext instanceof Event incomingEvent) {
            return updateEvent(existingEvent, incomingEvent);
        }
        if (existingPublicationContext instanceof MediaContribution existingMediaContribution
            && incomingPublicationContext instanceof MediaContribution incomingMediaContribution) {
            return new MediaContribution.Builder()
                       .withFormat(existingMediaContribution.getFormat())
                       .withMedium(existingMediaContribution.getMedium())
                       .withDisseminationChannel(updateDisseminationChannel(existingMediaContribution,
                                                                            incomingMediaContribution))
                       .build();
        } else {
            return existingPublicationContext;
        }
    }

    private static String updateDisseminationChannel(MediaContribution existingMediaContribution,
                                                  MediaContribution incomingMediaContribution) {
        return nonNull(existingMediaContribution.getDisseminationChannel())
                   ? existingMediaContribution.getDisseminationChannel()
                   : incomingMediaContribution.getDisseminationChannel();
    }

    private static Event updateEvent(Event existingEvent, Event incomingEvent) {
        var existingPlace = getPlaceCountry(existingEvent);
        var incomingPlace = getPlaceCountry(incomingEvent);
        if (shouldBeUpdated(existingPlace, incomingPlace)
            || shouldBeUpdated(getLabel(existingPlace), getLabel(incomingPlace))
            || shouldBeUpdated(existingEvent.getTime(), incomingEvent.getTime())) {
            return new Event.Builder()
                       .withName(
                           nonNull(existingEvent.getName()) ? existingEvent.getName() : incomingEvent.getName())
                       .withAgent(
                           nonNull(existingEvent.getAgent()) ? existingEvent.getAgent() : incomingEvent.getAgent())
                       .withTime(nonNull(existingEvent.getTime()) ? existingEvent.getTime() : incomingEvent.getTime())
                       .withProduct(existingEvent.getProduct().orElse(incomingEvent.getProduct().orElse(null)))
                       .withSubEvent(existingEvent.getSubEvent().orElse(incomingEvent.getSubEvent().orElse(null)))
                       .withPlace(incomingPlace)
                       .build();
        } else {
            return existingEvent;
        }
    }

    private static String getLabel(UnconfirmedPlace existingPlace) {
        return Optional.ofNullable(existingPlace).map(UnconfirmedPlace::name).orElse(null);
    }

    private static UnconfirmedPlace getPlaceCountry(Event event) {
        return Optional.ofNullable(event.getPlace())
                   .map(UnconfirmedPlace.class::cast)
                   .orElse(null);
    }

    private static PublicationContext getPublicationContext(Publication publication) {
        return publication
                   .getEntityDescription()
                   .getReference()
                   .getPublicationContext();
    }

    private static AssociatedArtifactList updatedAssociatedLinks(
        PublicationRepresentations publicationRepresentations) {
        var incomingAssociatedLinks = publicationRepresentations.getIncomingPublication().getAssociatedArtifacts();
        var existingAssociatedLinks = publicationRepresentations.getExistingPublication().getAssociatedArtifacts();
        var list = new ArrayList<>(existingAssociatedLinks);
        list.addAll(incomingAssociatedLinks);
        return new AssociatedArtifactList(list.stream().distinct().toList());
    }

    private static <T> boolean listShouldBeOverwritten(List<T> oldList, List<T> newList) {
        return !(oldList.size() == newList.size()
               && new HashSet<>(oldList).containsAll(newList)
               && new HashSet<>(newList).containsAll(oldList));
    }
}
