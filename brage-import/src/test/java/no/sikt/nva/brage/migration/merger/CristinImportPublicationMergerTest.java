package no.sikt.nva.brage.migration.merger;

import static no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger.PRIORITIZE_ABSTRACT;
import static no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger.PRIORITIZE_ALTERNATIVE_ABSTRACTS;
import static no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger.PRIORITIZE_ALTERNATIVE_TITLES;
import static no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger.PRIORITIZE_CONTRIBUTORS_WITH_CREATOR_ROLE;
import static no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger.PRIORITIZE_FUNDINGS;
import static no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger.PRIORITIZE_MAIN_TITLE;
import static no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger.PRIORITIZE_REFERENCE;
import static no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger.PRIORITIZE_TAGS;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Book.BookBuilder;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.GeographicalContent;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.NullPublisher;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.instancetypes.Map;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.media.MediaInterview;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import org.junit.jupiter.api.Test;

class CristinImportPublicationMergerTest {



    @Test
    void shouldUseBrageBookWhenExistingBookIsEmpty()
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(BookAnthology.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(emptyBook());
        var bragePublication = randomPublication(BookAnthology.class);

        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValuesIgnoringFields(Set.of("additionalIdentifiers")));
    }

    @Test
    void shouldUseExistingBookWhenBrageBookIsEmpty()
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var bragePublication = randomPublication(BookAnthology.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(emptyBook());
        var existingPublication = randomPublication(BookAnthology.class);

        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValuesIgnoringFields(Set.of("additionalIdentifiers")));
    }

    @Test
    void shouldUseBrageJournalWhenExistingJournalIsUnconfirmedJournalMissingAllValues()
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(JournalArticle.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(emptyJournal());
        var bragePublication = randomPublication(JournalArticle.class);

        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldUseExistingJournalWhenExistingJournalIsJournalAndBragePublicationHasUnconfirmedJournal()
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(JournalArticle.class);
        var bragePublication = randomPublication(JournalArticle.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(unconfirmedJournal());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   is(instanceOf(Journal.class)));
    }

    @Test
    void shouldUseExistingJournalWhenExistingJournalIsUnconfirmedJournalAndBragePublicationHasUnconfirmedJournalWithNullValues()
        throws InvalidUnconfirmedSeriesException, InvalidIsbnException, InvalidIssnException {
        var existingPublication = randomPublication(JournalArticle.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(unconfirmedJournal());
        var bragePublication = randomPublication(JournalArticle.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(emptyUnconfirmedJournal());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   is(instanceOf(UnconfirmedJournal.class)));
        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldReturnExistingJournalWhenNewPublicationContextIsNotJournal()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(JournalArticle.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(emptyJournal());
        var bragePublication = randomPublication(JournalArticle.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(unconfirmedJournal());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   is(instanceOf(UnconfirmedJournal.class)));
        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldReturnExistingJournalWhenNewPublicationContextIsNotSupportedJournalType()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(JournalArticle.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(unconfirmedJournal());
        var bragePublication = randomPublication(JournalArticle.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(new BookBuilder().build());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   is(instanceOf(UnconfirmedJournal.class)));
        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldUseExistingPublicationContextWhenIncomingReportIsEmpty()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(ReportResearch.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(unconfirmedJournal());
        var bragePublication = randomPublication(JournalArticle.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(emptyReport());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldUseExistingPublicationContextWhenIncomingEventIsEmpty()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(Lecture.class);
        var bragePublication = randomPublication(Lecture.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(emptyEvent());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldUseExistingPublicationContextWhenIncomingAnthologyIsEmpty()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(ChapterArticle.class);
        var bragePublication = randomPublication(ChapterArticle.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(new Anthology());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldUseExistingPublicationContextWhenIncomingMediaContributionIsEmpty()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(MediaInterview.class);
        var bragePublication = randomPublication(MediaInterview.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(new Anthology());
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference().getPublicationContext(),
                   doesNotHaveEmptyValues());
    }

    @Test
    void shouldUseExistingPublicationContextWhenIncomingResearchDataIsEmpty()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(DataSet.class);
        var bragePublication = randomPublication(DataSet.class);
        bragePublication.getEntityDescription().getReference()
            .setPublicationContext(new ResearchData(new NullPublisher()));
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        var researchData = (ResearchData) updatedPublication.getEntityDescription()
                                              .getReference().getPublicationContext();
        assertThat(researchData.getPublisher(), is(instanceOf(Publisher.class)));
    }

    @Test
    void shouldUseExistingPublicationContextWhenIncomingGeographicalContentIsEmpty()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(Map.class);
        var bragePublication = randomPublication(Map.class);
        bragePublication.getEntityDescription().getReference()
            .setPublicationContext(new GeographicalContent(new NullPublisher()));
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        var researchData = (GeographicalContent) updatedPublication.getEntityDescription()
                                                     .getReference().getPublicationContext();
        assertThat(researchData.getPublisher(), is(instanceOf(Publisher.class)));
    }

    @Test
    void shouldUseIncomingPublicationsFundingsWhenExistingPublicationMissesProjects()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(Map.class);
        existingPublication.setFundings(List.of());
        var bragePublication = randomPublication(Map.class);
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getProjects(), is(not(emptyIterable())));
    }

    @Test
    void shouldPrioritizePublisherIfRecordHasPublisherPrioritized()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        var existingPublication = randomPublication(DegreeBachelor.class);
        var shouldBeOverWrittenDuringMerging = new Degree.Builder().withPublisher(new Publisher(randomUri())).build();
        existingPublication.getEntityDescription().getReference()
            .setPublicationContext(shouldBeOverWrittenDuringMerging);
        var prioritizedPublisher = new Publisher(randomUri());
        var bragePublication = randomPublication(DegreeBachelor.class);
        bragePublication.getEntityDescription().getReference()
            .setPublicationContext(new Degree.Builder().withPublisher(prioritizedPublisher).build());
        var record = new Record();
        record.setId(bragePublication.getHandle());
        record.setPrioritizedProperties(Set.of("publisher"));
        var updatedPublication = mergePublications(existingPublication, bragePublication, record);

        var actualPublicationContext =
            (Degree)   updatedPublication.getEntityDescription().getReference().getPublicationContext();
        var actualPublisher = (Publisher) actualPublicationContext.getPublisher();
        assertThat(actualPublisher.getId(), is(equalTo(prioritizedPublisher.getId())));
    }

    @Test
    void shouldPrioritizeAuthorsFromBrageIfRecordHasContributorsWithRoleCreatorPrioritized()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(DegreeBachelor.class);
        var contributorThatShouldBeOverWrittenDuringMerging = new Contributor.Builder()
                                                                  .withIdentity(new Identity.Builder()
                                                                                    .withName(randomString())
                                                                                    .build())
                                                                  .withAffiliations(List.of())
                                                                  .withSequence(1)
                                                                  .withRole(new RoleType(Role.CREATOR))
                                                                  .build();
        var contributorThatShouldBeKept = new Contributor.Builder()
                                              .withIdentity(new Identity.Builder().withName(randomString()).build())
                                              .withAffiliations(List.of())
                                              .withSequence(2)
                                              .withRole(new RoleType(Role.ADVISOR))
                                              .build();
        existingPublication.getEntityDescription().setContributors(
            List.of(contributorThatShouldBeOverWrittenDuringMerging, contributorThatShouldBeKept));
        var bragePublication = randomPublication(DegreeBachelor.class);
        var contributorThatShouldBePrioritized = new Contributor.Builder()
                                                     .withIdentity(new Identity.Builder()
                                                                       .withName(randomString())
                                                                       .build())
                                                     .withAffiliations(List.of())
                                                     .withSequence(1)
                                                     .withRole(new RoleType(Role.CREATOR))
                                                     .build();
        var contributorThatShouldBeIgnored = new Contributor.Builder()
                                                 .withIdentity(new Identity.Builder().withName(randomString()).build())
                                                 .withAffiliations(List.of())
                                                 .withSequence(2)
                                                 .withRole(new RoleType(Role.ADVISOR))
                                                 .build();
        bragePublication.getEntityDescription()
            .setContributors(List.of(contributorThatShouldBePrioritized, contributorThatShouldBeIgnored));
        var record = new Record();
        record.setId(bragePublication.getHandle());
        record.setPrioritizedProperties(Set.of(PRIORITIZE_CONTRIBUTORS_WITH_CREATOR_ROLE));
        var updatedPublication = mergePublications(existingPublication, bragePublication, record);
        var contributors = updatedPublication.getEntityDescription().getContributors();
        assertThat(contributors, hasItem(contributorThatShouldBePrioritized));
        assertThat(contributors, hasItem(contributorThatShouldBeKept));
        assertThat(contributors, not(hasItem(contributorThatShouldBeIgnored)));
        assertThat(contributors, not(hasItem(contributorThatShouldBeOverWrittenDuringMerging)));
    }

    @Test
    void shouldKeepFileFromNewPublicationWhenExistingPublicationHasAssociatedLinkOnly()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var associatedLink = randomAssociatedLink();
        var newPublishedFile = randomPublishedFile();

        var existingPublication = randomPublication(Map.class);
        existingPublication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(associatedLink)));
        var bragePublication = randomPublication(Map.class);
        bragePublication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(newPublishedFile)));

        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getAssociatedArtifacts(),
                   containsInAnyOrder(associatedLink, newPublishedFile));
    }

    @Test
    void shouldKeepFileFromNewPublicationWhenExistingPublicationHasAdministrativeAgreementOnly()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var administrativeAgreement = randomAdministrativeAgreement();
        var newPublishedFile = randomPublishedFile();

        var existingPublication = randomPublication(Map.class);
        existingPublication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(administrativeAgreement)));
        var bragePublication = randomPublication(Map.class);
        bragePublication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(newPublishedFile)));

        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getAssociatedArtifacts(),
                   containsInAnyOrder(administrativeAgreement, newPublishedFile));
    }

    private File randomPublishedFile() {
        return File.builder()
                   .withName(randomString())
                   .withIdentifier(UUID.randomUUID())
                   .withLicense(randomUri())
                   .buildPublishedFile();
    }

    private File randomAdministrativeAgreement() {
        return File.builder()
                   .withName(randomString())
                   .withIdentifier(UUID.randomUUID())
                   .withLicense(randomUri())
                   .withAdministrativeAgreement(true)
                   .buildUnpublishableFile();
    }

    private static AssociatedLink randomAssociatedLink() {
        var associatedLink = new AssociatedLink(randomUri(), null, null);
        return associatedLink;
    }

    @Test
    void shouldMergeWhenPublicationContextAndPublicationInstanceIsMissingOnIncomingPublication()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(Map.class);
        var bragePublication = randomPublication(Map.class);
        bragePublication.getEntityDescription().getReference().setPublicationContext(null);
        bragePublication.getEntityDescription().getReference().setPublicationInstance(null);
        var updatedPublication = mergePublications(existingPublication, bragePublication);

        assertThat(updatedPublication.getEntityDescription().getReference(),
                   is(equalTo(existingPublication.getEntityDescription().getReference())));
    }

    @Test
    void shouldPrioritizeMainTitleIfRecordHasMainTitleSetAsPrioritized()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(DegreeBachelor.class);
        var titleThatShouldBeOverWrittenDuringMerging = randomString();
        existingPublication.getEntityDescription().setMainTitle(titleThatShouldBeOverWrittenDuringMerging);

        var bragePublication = randomPublication(DegreeBachelor.class);
        var mainTitleThatShouldBeKept = randomString();
        bragePublication.getEntityDescription().setMainTitle(mainTitleThatShouldBeKept);

        var record = new Record();
        record.setId(bragePublication.getHandle());
        record.setPrioritizedProperties(Set.of(PRIORITIZE_MAIN_TITLE));

        var updatedPublication = mergePublications(existingPublication, bragePublication, record);

        assertThat(updatedPublication.getEntityDescription().getMainTitle(), is(equalTo(mainTitleThatShouldBeKept)));
    }

    @Test
    void shouldPioritizeAlternativeTitleIfRecordHasAlternativeTitleSetAsPrioritized()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(DegreeBachelor.class);
        var alternativeTitleThatShouldBeOverWrittenDuringMerging = java.util.Map.of(randomString(), randomString());
        existingPublication.getEntityDescription()
            .setAlternativeTitles(alternativeTitleThatShouldBeOverWrittenDuringMerging);

        var bragePublication = randomPublication(DegreeBachelor.class);
        var alternativeTitleThatShouldBeKept = java.util.Map.of(randomString(), randomString());
        bragePublication.getEntityDescription().setAlternativeTitles(alternativeTitleThatShouldBeKept);

        var record = new Record();
        record.setId(bragePublication.getHandle());
        record.setPrioritizedProperties(Set.of(PRIORITIZE_ALTERNATIVE_TITLES));

        var updatedPublication = mergePublications(existingPublication, bragePublication, record);

        assertThat(updatedPublication.getEntityDescription().getAlternativeTitles(),
                   is(equalTo(alternativeTitleThatShouldBeKept)));
    }

    @Test
    void shouldPrioritizeAbstractIfRecordHasAbstractAsPrioritized()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(DegreeBachelor.class);
        var abstractThatShouldBeOverWrittenDuringMerging = randomString();
        existingPublication.getEntityDescription().setAbstract(abstractThatShouldBeOverWrittenDuringMerging);

        var bragePublication = randomPublication(DegreeBachelor.class);
        var abstractThatShouldBeKept = randomString();
        bragePublication.getEntityDescription().setAbstract(abstractThatShouldBeKept);

        var record = new Record();
        record.setId(bragePublication.getHandle());
        record.setPrioritizedProperties(Set.of(PRIORITIZE_ABSTRACT));

        var updatedPublication = mergePublications(existingPublication, bragePublication, record);

        assertThat(updatedPublication.getEntityDescription().getAbstract(), is(equalTo(abstractThatShouldBeKept)));
    }

    @Test
    void shouldPrioritizeAlternativeAbstractsIfRecordHasAlternativeAbstractAsPrioritized()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(DegreeBachelor.class);
        var abstractThatShouldBeOverWrittenDuringMerging = java.util.Map.of(randomString(), randomString());
        existingPublication.getEntityDescription()
            .setAlternativeAbstracts(abstractThatShouldBeOverWrittenDuringMerging);

        var bragePublication = randomPublication(DegreeBachelor.class);
        var abstractThatShouldBeKept = java.util.Map.of(randomString(), randomString());
        bragePublication.getEntityDescription().setAlternativeAbstracts(abstractThatShouldBeKept);

        var record = new Record();
        record.setId(bragePublication.getHandle());
        record.setPrioritizedProperties(Set.of(PRIORITIZE_ALTERNATIVE_ABSTRACTS));

        var updatedPublication = mergePublications(existingPublication, bragePublication, record);

        assertThat(updatedPublication.getEntityDescription().getAlternativeAbstracts(),
                   is(equalTo(abstractThatShouldBeKept)));
    }

    @Test
    void shouldPrioritizeReferenceFromBrageIfRecordHasReferenceAsPrioritized()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(DegreeBachelor.class);

        var bragePublication = randomPublication(DegreeBachelor.class);
        var referenceThatShouldBeKept = bragePublication.getEntityDescription().getReference();

        var record = new Record();
        record.setId(bragePublication.getHandle());
        record.setPrioritizedProperties(Set.of(PRIORITIZE_REFERENCE));

        var updatedPublication = mergePublications(existingPublication, bragePublication, record);

        assertThat(updatedPublication.getEntityDescription().getReference(), is(equalTo(referenceThatShouldBeKept)));
    }

    @Test
    void shouldMergeTagsWhenRecordHasTagsPrioritized()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(DegreeBachelor.class);
        var tag = randomString();
        var duplicatedTag = randomString();
        var tags = new ArrayList<String>();
        tags.add(tag);
        tags.add(duplicatedTag);
        tags.add(null);
        existingPublication.getEntityDescription().setTags(tags);

        var bragePublication = randomPublication(DegreeBachelor.class);
        var newTag = randomString();
        bragePublication.getEntityDescription().setTags(List.of(newTag, duplicatedTag.toUpperCase(Locale.ROOT)));

        var record = new Record();
        record.setId(bragePublication.getHandle());
        record.setPrioritizedProperties(Set.of(PRIORITIZE_TAGS));

        var updatedPublication = mergePublications(existingPublication, bragePublication, record);

        var acutalTags = updatedPublication.getEntityDescription().getTags();
        assertThat(acutalTags, is(containsInAnyOrder(tag,
                                                                                              duplicatedTag,
                                                                                              newTag)));
    }

    @Test
    void shouldMergeFundingsIfRecordHasFundingsPrioritized()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var existingPublication = randomPublication(DegreeBachelor.class);
        var funding =  new FundingBuilder().withId(randomUri())
                           .withSource(randomUri())
                           .withIdentifier(randomString())
                           .build();
        existingPublication.setFundings(List.of(funding));

        var bragePublication = randomPublication(DegreeBachelor.class);
        var newFunding =  new FundingBuilder().withId(randomUri()).build();

        bragePublication.setFundings(List.of(newFunding));

        var record = new Record();
        record.setId(bragePublication.getHandle());
        record.setPrioritizedProperties(Set.of(PRIORITIZE_FUNDINGS));

        var updatedPublicaiton = mergePublications(existingPublication, bragePublication, record);

        assertThat(updatedPublicaiton.getFundings(), is(containsInAnyOrder(funding, newFunding)));
    }

    private PublicationContext emptyUnconfirmedJournal() throws InvalidIssnException {
        return new UnconfirmedJournal(null, null, null);
    }

    private static Publication mergePublications(Publication existingPublication, Publication bragePublication)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        var record = new Record();
        record.setId(bragePublication.getHandle());
        return mergePublications(existingPublication, bragePublication, record);
    }

    private static Publication mergePublications(Publication existingPublication, Publication bragePublication,
                                                 Record record)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var representation = new PublicationRepresentation(record, bragePublication);
        return new CristinImportPublicationMerger(existingPublication,
                                                  representation).mergePublications();

    }

    private static Book emptyBook() throws InvalidUnconfirmedSeriesException {
        return new Book(null, null, null, null, null, null);
    }

    private static UnconfirmedJournal emptyJournal() throws InvalidIssnException {
        return new UnconfirmedJournal(null, null, null);
    }

    private static UnconfirmedJournal unconfirmedJournal() throws InvalidIssnException {
        return new UnconfirmedJournal(randomString(), randomIssn(), randomIssn());
    }

    private static Report emptyReport() throws InvalidIssnException, InvalidUnconfirmedSeriesException {
        return new Report.Builder().build();
    }

    private static Event emptyEvent() {
        return new Event.Builder().build();
    }

}