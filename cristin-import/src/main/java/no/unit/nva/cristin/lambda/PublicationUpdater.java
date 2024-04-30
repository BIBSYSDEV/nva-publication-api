package no.unit.nva.cristin.lambda;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.cristin.mapper.CristinSecondaryCategory;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.AcademicLiteratureReview;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.PopularScienceArticle;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;
import no.unit.nva.model.pages.Pages;
import nva.commons.core.JacocoGenerated;

public final class PublicationUpdater {

    private PublicationUpdater() {
    }

    public static PublicationRepresentations update(PublicationRepresentations publicationRepresentations) {
        var existingPublication = publicationRepresentations.getExistingPublication();
        existingPublication.setAssociatedArtifacts(updatedAssociatedLinks(publicationRepresentations));
        existingPublication.setEntityDescription(updatedEntityDescription(publicationRepresentations));
        return publicationRepresentations;
    }

    private static EntityDescription updatedEntityDescription(PublicationRepresentations publicationRepresentations) {
        return publicationRepresentations.getExistingPublication().getEntityDescription().copy()
                   .withReference(updateReference(publicationRepresentations))
                   .withTags(updatedTags(publicationRepresentations))
                   .build();
    }

    private static List<String> updatedTags(PublicationRepresentations publicationRepresentations) {
        var existingTags = publicationRepresentations.getExistingPublication().getEntityDescription().getTags();
        var incomingTags = publicationRepresentations.getIncomingPublication().getEntityDescription().getTags();
        var list = new ArrayList<>(existingTags);
        list.addAll(incomingTags);
        return list.stream().distinct().toList();
    }

    private static Reference updateReference(PublicationRepresentations publicationRepresentations) {
        var existinReference = publicationRepresentations.getExistingPublication().getEntityDescription().getReference();
        existinReference.setPublicationContext(updatePublicationContext(publicationRepresentations));
        existinReference.setPublicationInstance(updatePublicationInstance(publicationRepresentations));
        return existinReference;

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
        if (existingPublicationContext instanceof Event existingEvent && incomingPublicationContext instanceof Event incomingEvent) {
            var existingPlace = existingEvent.getPlace();
            var incomingPlace = incomingEvent.getPlace();
            if (isNull(existingPlace) && nonNull(incomingPlace)) {
                return new Event.Builder()
                           .withLabel(existingEvent.getLabel())
                           .withAgent(existingEvent.getAgent())
                           .withTime(existingEvent.getTime())
                           .withProduct(existingEvent.getProduct().orElse(null))
                           .withSubEvent(existingEvent.getSubEvent().orElse(null))
                           .withPlace(incomingPlace)
                           .build();
            } else {
                return existingEvent;
            }
        } else {
            return existingPublicationContext;
        }
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
}
