package cucumber;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.cristin.mapper.artisticproduction.ArtisticEvent;
import no.unit.nva.cristin.mapper.artisticproduction.ArtisticProductionTimeUnit;
import no.unit.nva.cristin.mapper.artisticproduction.CristinArtisticProduction;
import no.unit.nva.cristin.mapper.artisticproduction.CristinProduct;
import no.unit.nva.cristin.mapper.artisticproduction.Performance;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.film.realization.OtherRelease;
import no.unit.nva.model.instancetypes.artistic.music.AudioVisualPublication;
import no.unit.nva.model.instancetypes.artistic.music.Concert;
import no.unit.nva.model.instancetypes.artistic.music.InvalidIsmnException;
import no.unit.nva.model.instancetypes.artistic.music.InvalidIsrcException;
import no.unit.nva.model.instancetypes.artistic.music.Ismn;
import no.unit.nva.model.instancetypes.artistic.music.Isrc;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.artistic.music.MusicScore;
import no.unit.nva.model.instancetypes.artistic.music.MusicalWork;
import no.unit.nva.model.instancetypes.artistic.music.OtherPerformance;
import no.unit.nva.model.time.Time;
import no.unit.nva.model.time.Instant;
import nva.commons.core.ioutils.IoUtils;

import java.nio.file.Path;

import static no.unit.nva.cristin.mapper.artisticproduction.ArtisticProductionTimeUnit.MINUTE;
import static no.unit.nva.model.instancetypes.artistic.film.MovingPictureSubtypeEnum.SHORT;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class ArtisticFeatures {

    private final ScenarioContext scenarioContext;

    public ArtisticFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @And("the Cristin result with both type_kunstneriskproduksjon and type_produkt present")
    public void validCristinResultWithBothTypeKunstneriskproduksjonAndTypeProduktPresent() {
        var artisticProduction = readArtisticProductionFromResource();
        var product = readProductFromResource();
        scenarioContext.getCristinEntry().setCristinArtisticProduction(artisticProduction);
        scenarioContext.getCristinEntry().setCristinProduct(product);
    }

    @Then("the NVA resources contains the data scraped from type_kunstneriskproduksjon")
    public void theNvaResourcesContainsTheDataScrapedFromTypeKunstneriskproduksjon() {
        var nvaResource = scenarioContext.getNvaEntry();
        var publicationInstance = nvaResource.getEntityDescription().getReference().getPublicationInstance();
        assertThat(publicationInstance, is(instanceOf(MovingPicture.class)));
        var movingPicture = (MovingPicture) publicationInstance;
        assertThat(movingPicture.getSubtype().getType(), is(equalTo(SHORT)));
        assertThat(movingPicture.getOutputs(), hasSize(1));
        var output = movingPicture.getOutputs().get(0);
        assertThat(output, is(instanceOf(OtherRelease.class)));
        var otherRelease = (OtherRelease) output;
        var publisher = (UnconfirmedPublisher) otherRelease.getPublisher();
        assertThat(publisher.getName(), is(equalTo("Landbruksfilm")));
    }


    @And("the cristin result has a {string} present with duration equal to {string} minutes")
    public void theCristinResultHasAPresentWithDurationEqualToMinutes(String metadatafield, String minutes) {
        var timeUnit = new ArtisticProductionTimeUnit(MINUTE);
        if ("type_kunstneriskproduksjon".equals(metadatafield)) {
            var artisticProduction = readArtisticProductionFromResource();
            artisticProduction.setArtisticProductionTimeUnit(timeUnit);
            artisticProduction.setDuration(minutes);
            scenarioContext.getCristinEntry().setCristinArtisticProduction(artisticProduction);
            scenarioContext.getCristinEntry().setCristinProduct(null);
        }
        if ("type_produkt".equals(metadatafield)) {
            var product = readProductFromResource();
            product.setTimeUnit(timeUnit);
            product.setDuration(minutes);
            scenarioContext.getCristinEntry().setCristinProduct(product);
            scenarioContext.getCristinEntry().setCristinArtisticProduction(null);
        }

    }

    @Then("the Cristin Result contains a MovingPictureSubtypeEnum equal to {string}")
    public void theCristinResultContainsAMovingPictureSubtypeEnumEqualTo(String movingPictureSubtypeEnum) {
        var publicationInstance =
            scenarioContext.getNvaEntry().getEntityDescription().getReference().getPublicationInstance();
        assertThat(publicationInstance, is(instanceOf(MovingPicture.class)));
        var movingPicture = (MovingPicture) publicationInstance;
        assertThat(movingPicture.getSubtype().getType().getType(),
            is(equalTo(movingPictureSubtypeEnum)));
    }

    @And("the cristin result lack the duration in both metadata fields")
    public void theCristinResultLackTheDurationInBothMetadataFields() {
        scenarioContext.getCristinEntry().getCristinArtisticProduction().setArtisticProductionTimeUnit(null);
        scenarioContext.getCristinEntry().getCristinProduct().setTimeUnit(null);
    }

    @And("the performance type is equal to {string}")
    public void thePerformanceTypeIsEqualtTo(String performanceType) {
        if ("null".equals(performanceType)) {
            scenarioContext.getCristinEntry()
                .getCristinArtisticProduction()
                .setPerformance(null);
        } else {
            scenarioContext.getCristinEntry()
                .getCristinArtisticProduction()
                .setPerformance(Performance.builder().withPerformanceType(performanceType).build());

        }

    }

    @And("the performance is a premiere")
    public void thePerformanceIsAPremiere() {
        scenarioContext.getCristinEntry().getCristinArtisticProduction().setPremiere("J");
    }

    @And("the performance has a duration of {string} minutes")
    public void thePerformanceHasADurationOfMinutes(String duration) {
        scenarioContext.getCristinEntry().getCristinArtisticProduction().setDuration(duration);
        scenarioContext.getCristinEntry().getCristinArtisticProduction()
            .setArtisticProductionTimeUnit(ArtisticProductionTimeUnit
                .builder()
                .withTimeUnitCode("MINUTT")
                .build());
    }

    @And("the performance has an original composer {string}")
    public void thePerformanceHasAnOriginalComposer(String composer) {
        scenarioContext.getCristinEntry().getCristinArtisticProduction().setOriginalComposer(composer);
    }

    @Then("the Nva Resource has a Concert announcements")
    public void theNvaResourceHasAConcertAnnouncements() {
        var musicalWorkPerformance = extractMusicPerformance();
        assertThat(musicalWorkPerformance.getManifestations(), hasSize(1));
        assertThat(musicalWorkPerformance.getManifestations().get(0), instanceOf(Concert.class));
    }

    @And("the concert has a place {string}")
    public void theConcertHasAPlaceDateAndDurationMinutes(String place) {
        var concert = getConcert();
        var expectedPlace = new UnconfirmedPlace(place, null);
        assertThat(concert.getPlace(), is(equalTo(expectedPlace)));
    }

    @And("the concert has a duration of {string} minutes")
    public void theConcertHasADurationOfMinutes(String expectedDuration) {
        var concert = getConcert();
        assertThat(concert.getExtent(), is(equalTo(expectedDuration)));
    }


    @And("the concert has a date {string}")
    public void theConcertHasADate(String time) {
        var expectedTime = new Instant(Time.convertToInstant(time));
        var concert = getConcert();
        assertThat(concert.getTime(), instanceOf(Instant.class));
        assertThat(concert.getTime(), is(equalTo(expectedTime)));
    }

    @And("the concert has a program with title {string}")
    public void theConcertHasAProgramWithTitleComposerAndIsAPremiere(String title) {
        var concert = getConcert();
        assertThat(concert.getConcertProgramme(), hasSize(1));
        var concertProgramme = concert.getConcertProgramme().get(0);
        assertThat(concertProgramme.getTitle(), is(equalTo(title)));
    }

    @And("the concert has a program with composer {string}")
    public void theConcertHasAProgramWithComposer(String expectedComposer) {
        var concert = getConcert();
        var concertProgramme = concert.getConcertProgramme().get(0);
        assertThat(concertProgramme.getComposer(), is(equalTo(expectedComposer)));
    }

    @And("the concert has a program that is a premiere")
    public void theConcertHasAProgramThatIsAPremiere() {
        var concert = getConcert();
        var concertProgramme = concert.getConcertProgramme().get(0);
        assertThat(concertProgramme.isPremiere(), is(equalTo(true)));
    }

    @And("the performance has an event with values:")
    public void thePerformanceHasAnEventWithValues(ArtisticEvent artisticEvent) {
        scenarioContext.getCristinEntry().getCristinArtisticProduction().setEvent(artisticEvent);
    }

    @Then("the Nva Resource has a OtherPerformance")
    public void theNvaResourceHasAOtherPerformance() {
        var musicalWorkPerformance = extractMusicPerformance();
        assertThat(musicalWorkPerformance.getManifestations(), hasSize(1));
        assertThat(musicalWorkPerformance.getManifestations().get(0), instanceOf(OtherPerformance.class));
    }

    @And("the OtherPerformance has a place {string}")
    public void theOtherPerformanceHasAPlaceDateAndDurationMinutes(String place) {
        var otherPerformance = getOtherPerformance();
        var expectedPlace = new UnconfirmedPlace(place, null);
        assertThat(otherPerformance.getPlace(), is(equalTo(expectedPlace)));
    }

    @And("the OtherPerformance has a duration {string} minutes")
    public void theOtherPerformanceHasADurationMinutes(String expectedDurationInMinutes) {
        var otherPerformance = getOtherPerformance();
        assertThat(otherPerformance.getExtent(), is(equalTo(expectedDurationInMinutes)));
    }

    @And("the OtherPerformance has a musicalWorkPerformance with title {string}")
    public void theOtherPerformanceHasAMusicalWPeWithTitleComposerThatIsAPremiere(String title) {
        var program = getMusicalWork();
        assertThat(program.getTitle(), is(equalTo(title)));
    }

    @And("the OtherPerformance has a composer {string}")
    public void theOtherPerformanceHasAComposer(String expectedComposer) {
        var program = getMusicalWork();
        assertThat(program.getComposer(), is(equalTo(expectedComposer)));
    }

    @And("the performance has a ISRC equal to {string}")
    public void thePerformanceHasAnIsrcEqualTo(String isrc) {
        scenarioContext.getCristinEntry().getCristinArtisticProduction().setIsrc(isrc);
    }

    @And("the performance has a medium equal to {string}")
    public void thePerformanceHasAMediumEqualTo(String medium) {
        scenarioContext.getCristinEntry().getCristinArtisticProduction().setMedium(medium);
    }

    @And("the performance has a publisher name equal to {string}")
    public void thePerformanceHasAPublisherNameEqualTo(String publisherName) {
        scenarioContext.getCristinEntry().getCristinArtisticProduction().setPublisherName(publisherName);
    }

    @Then("the Nva resource has a AudioVisualPublication")
    public void theNvaResourceHasAAudioVisualPublication() {
        var musicalWorkPerformance = extractMusicPerformance();
        assertThat(musicalWorkPerformance.getManifestations(),
            hasItem(instanceOf(AudioVisualPublication.class)));
    }

    @And("the performance has a ISMN equal to {string}")
    public void thePerformanceHasAIsmnEqualTo(String ismn) {
        scenarioContext.getCristinEntry().getCristinArtisticProduction().setIsmn(ismn);
    }

    @Then("the NVA resource has a MusicScore")
    public void theNvaResourceHasAMusicScore() {
        var musicalWorkPerformance = extractMusicPerformance();
        assertThat(musicalWorkPerformance.getManifestations(), hasItem(instanceOf(MusicScore.class)));
    }

    @And("the MusicScore has a ISMN equal to {string}")
    public void theMusicScoreHasAnIsmnNEqualTo(String ismn) throws InvalidIsmnException {
        var musicalWorkPerformance = extractMusicPerformance();
        var musicScoreOptional = musicalWorkPerformance
            .getManifestations()
            .stream()
            .filter(manifestation -> manifestation instanceof MusicScore)
            .findFirst();
        assertThat(musicScoreOptional.isPresent(), is(equalTo(true)));
        var musicScore = (MusicScore) musicScoreOptional.get();
        assertThat(musicScore.getIsmn(), is(equalTo(new Ismn(ismn))));
    }

    @And("the performance has a ensemble name equal to {string}")
    public void thePerformanceHasAEnsembleNameEqualTo(String ensembleName) {
        scenarioContext.getCristinEntry().getCristinArtisticProduction().setEnsembleName(ensembleName);
    }

    @And("the MusicScore has ensemble equal to {string}")
    public void theMusicScoreHasEnsembleEqualTo(String ensemble) {
        var musicalWorkPerformance = extractMusicPerformance();
        var musicScoreOptional = musicalWorkPerformance.getManifestations()
            .stream()
            .filter(manifestation -> manifestation instanceof MusicScore)
            .findFirst();
        assertThat(musicScoreOptional.isPresent(), is(equalTo(true)));
        var musicScore = (MusicScore) musicScoreOptional.get();
        assertThat(musicScore.getEnsemble(), is(equalTo(ensemble)));
    }

    @And("the AudioVisualPublication has a mediaSubType equalTo {string}")
    public void theAudioVisualPublicationHasAMediaSubTypeEqualTo(String expectedediaSubType) {
        var audioVisualManifestation = getAudioVisualPublication();
        assertThat(audioVisualManifestation.getMediaType().getType().getValue(),
            is(equalTo(expectedediaSubType)));
    }


    @And("the AudioVisualPublication has ISRC equalTo {string},")
    public void theAudioVisualPublicationHasIsrcEqualTo(String expectedIsrc) throws InvalidIsrcException {
        var audioVisualManifestation = getAudioVisualPublication();
        assertThat(audioVisualManifestation.getIsrc(),
            is(equalTo(new Isrc(expectedIsrc))));
    }

    @And("the AudioVisualPublication has an unconfirmedPublisher name equal to {string}")
    public void theAudioVisualPublicationHasAnUnconfirmedPublisherNameEqualTo(String expectedUnconfirmedPublisher) {
        var audioVisualManifestation = getAudioVisualPublication();
        assertThat(audioVisualManifestation.getPublisher(),
            is(equalTo(new UnconfirmedPublisher(expectedUnconfirmedPublisher))));

    }


    @And("the performance has a field besetning with value {string}")
    public void thePerformanceHasAFieldBesetningWithValue(String crew) {
        scenarioContext.getCristinEntry().getCristinArtisticProduction().setCrew(crew);
    }


    @And("the performance has a field medskapere with value {string}")
    public void thePerformanceHasAFieldMedskapereWithValue(String medskapere) {
        scenarioContext.getCristinEntry().getCristinArtisticProduction().setCoCreators(medskapere);
    }

    @Then("the NVA resource has a description field containing the value {string}")
    public void theNvaResourceHasADescriptionFieldContainingTheValue(String descriptionPart) {
        var description = scenarioContext.getNvaEntry().getEntityDescription().getDescription();
        assertThat(description, is(containsString(descriptionPart)));
    }

    private OtherPerformance getOtherPerformance() {
        var musicalWorkPerformance = extractMusicPerformance();
        var otherPerformance = (OtherPerformance) musicalWorkPerformance.getManifestations().get(0);
        return otherPerformance;
    }


    private AudioVisualPublication getAudioVisualPublication() {
        var musicalWorkPerformance = extractMusicPerformance();
        var audioVisualManifestationOptional = musicalWorkPerformance
            .getManifestations()
            .stream()
            .filter(manifestation -> manifestation instanceof AudioVisualPublication)
            .findFirst();
        assertThat(audioVisualManifestationOptional.isPresent(), is(equalTo(true)));
        return (AudioVisualPublication) audioVisualManifestationOptional.get();
    }

    private String readResourceFile(Path path) {
        return IoUtils.stringFromResources(path);
    }

    private CristinProduct readProductFromResource() {
        var cristinProductString = readResourceFile(Path.of("type_produkt.json"));
        return attempt(
            () -> JsonUtils
                .dtoObjectMapper
                .readValue(cristinProductString, CristinProduct.class))
            .orElseThrow();
    }

    private CristinArtisticProduction readArtisticProductionFromResource() {
        var artisticProduction = readResourceFile(Path.of("type_kunstneriskproduksjon.json"));
        return attempt(() -> JsonUtils.dtoObjectMapper
            .readValue(artisticProduction, CristinArtisticProduction.class))
            .orElseThrow();
    }

    private MusicPerformance extractMusicPerformance() {
        return (MusicPerformance) scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
    }

    private MusicalWork getMusicalWork() {
        var otherPerformance = getOtherPerformance();
        assertThat(otherPerformance.getMusicalWorks(), hasSize(1));
        assertThat(otherPerformance.getMusicalWorks().get(0), instanceOf(MusicalWork.class));
        var program = (MusicalWork) otherPerformance.getMusicalWorks().get(0);
        return program;
    }

    private Concert getConcert() {
        var musicalWorkPerformance = extractMusicPerformance();
        return (Concert) musicalWorkPerformance.getManifestations().get(0);
    }
}
