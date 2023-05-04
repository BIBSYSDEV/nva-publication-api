package no.unit.nva.cristin.mapper.nva;

import static no.unit.nva.cristin.mapper.CristinMainCategory.isBook;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isChapter;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isEvent;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isJournal;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isMediaContribution;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isReport;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreeLicentiate;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreeMaster;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreePhd;
import static no.unit.nva.cristin.mapper.nva.exceptions.ExceptionHandling.castToCorrectRuntimeException;
import static nva.commons.core.attempt.Try.attempt;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.cristin.mapper.CristinJournalPublication;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.CristinSecondaryCategory;
import no.unit.nva.cristin.mapper.PeriodicalBuilder;
import no.unit.nva.cristin.mapper.PublicationInstanceBuilderImpl;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.MediaContribution;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.media.MediaFormat;
import no.unit.nva.model.contexttypes.media.MediaSubType;
import no.unit.nva.model.contexttypes.media.MediaSubTypeEnum;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import nva.commons.doi.DoiConverter;
import nva.commons.doi.DoiValidator;

public class ReferenceBuilder extends CristinMappingModule {

    private static final Config config = loadConfiguration();
    private static final boolean VALIDATE_DOI_ONLINE = parseValidateDoiOnline();

    private final DoiConverter doiConverter;

    public ReferenceBuilder(CristinObject cristinObject) {
        super(cristinObject);
        DoiValidator doiValidator = new DoiValidator();
        doiConverter = new DoiConverter(doiUri -> validateDoi(doiValidator, doiUri));
    }

    public Reference buildReference() {
        PublicationInstanceBuilderImpl publicationInstanceBuilderImpl = new PublicationInstanceBuilderImpl(
            cristinObject);
        PublicationInstance<? extends Pages> publicationInstance = publicationInstanceBuilderImpl.build();
        PublicationContext publicationContext = attempt(this::buildPublicationContext).orElseThrow(
            failure -> castToCorrectRuntimeException(failure.getException()));
        return new Reference.Builder().withPublicationInstance(publicationInstance)
                   .withPublishingContext(publicationContext)
                   .withDoi(extractDoi())
                   .build();
    }

    private static boolean validateDoi(DoiValidator doiValidator, URI doiUri) {
        return VALIDATE_DOI_ONLINE ? doiValidator.validateOnline(doiUri) : DoiValidator.validateOffline(doiUri);
    }

    private static boolean parseValidateDoiOnline() {
        return config.getBoolean("doi.validation.online");
    }

    private static Config loadConfiguration() {
        return ConfigFactory.load(ConfigFactory.defaultApplication());
    }

    private PublicationContext buildPublicationContext()
        throws InvalidIsbnException, InvalidIssnException, InvalidUnconfirmedSeriesException {
        if (isBook(cristinObject)) {
            return new NvaBookBuilder(cristinObject).buildBookForPublicationContext();
        }
        if (isJournal(cristinObject)) {
            return new PeriodicalBuilder(cristinObject).buildPeriodicalForPublicationContext();
        }
        if (isReport(cristinObject)) {
            return buildPublicationContextWhenMainCategoryIsReport();
        }
        if (isChapter(cristinObject)) {
            return buildChapterForPublicationContext();
        }
        if (isEvent(cristinObject)) {
            return buildEventForPublicationContext();
        }
        if (isMediaContribution(cristinObject)) {
            return buildMediaContributionForPublicationContext();
        }
        return null;
    }

    private PublicationContext buildMediaContributionForPublicationContext() {
        return isWrittenInterview(cristinObject) ? new MediaContribution.Builder().withFormat(MediaFormat.TEXT)
                                                       .withMedium(MediaSubType.create(MediaSubTypeEnum.JOURNAL))
                                                       .build()
                   : new MediaContribution.Builder().withFormat(new MediaFormatBuilder(cristinObject).build())
                         .withMedium(new MediaSubTypeBuilder(cristinObject).build())
                         .build();
    }

    private boolean isWrittenInterview(CristinObject cristinObject) {
        return CristinSecondaryCategory.WRITTEN_INTERVIEW.equals(cristinObject.getSecondaryCategory());
    }

    private PublicationContext buildPublicationContextWhenMainCategoryIsReport()
        throws InvalidIsbnException, InvalidIssnException, InvalidUnconfirmedSeriesException {
        if (isDegreePhd(cristinObject) || isDegreeMaster(cristinObject) || isDegreeLicentiate(cristinObject)) {
            return new NvaDegreeBuilder(cristinObject).buildDegree();
        }
        return new NvaReportBuilder(cristinObject).buildNvaReport();
    }

    private Anthology buildChapterForPublicationContext() {
        return new Anthology.Builder().build();
    }

    private PublicationContext buildEventForPublicationContext() {
        return new Event.Builder().build();
    }

    private URI extractDoi() {
        if (isJournal(cristinObject)) {
            return Optional.of(extractCristinJournalPublication())
                       .map(CristinJournalPublication::getDoi)
                       .map(doiConverter::toUri)
                       .orElse(null);
        }
        return null;
    }
}
