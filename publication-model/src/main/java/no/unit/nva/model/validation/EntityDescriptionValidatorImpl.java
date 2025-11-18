package no.unit.nva.model.validation;

import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.contexttypes.PublicationContext;
import nva.commons.core.paths.UriWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.nonNull;

public class EntityDescriptionValidatorImpl implements EntityDescriptionValidator {

    public static final String UNSYNCHRONIZED_PUBLICATION_CHANNEL_DATE_MESSAGE =
            "EntityDescription contains unsynchronized publication-date and publication-channel combinations";

    @Override
    public ValidationReport validate(EntityDescription entityDescription) {
        var publicationContext = entityDescription.getReference().getPublicationContext();
        var publicationDate = entityDescription.getPublicationDate();
        var errors = hasUnsynchronizedPublicationDateChannelDatePair(publicationContext, publicationDate)
                ? List.of(UNSYNCHRONIZED_PUBLICATION_CHANNEL_DATE_MESSAGE)
                : Collections.<String>emptyList();
        return new EntityDescriptionValidationReport(errors);
    }

    private boolean hasUnsynchronizedPublicationDateChannelDatePair(PublicationContext context,
                                                                    PublicationDate publicationDate) {
        return nonNull(context) && hasInvalidChannelYears(context, publicationDate);

    }

    private boolean hasInvalidChannelYears(PublicationContext context, PublicationDate publicationDate) {
        var channelYears = extractPublicationChannelYears(context);
        return hasMultipleChannelYears(channelYears)
                || hasMismatchedPublicationDate(channelYears.getFirst(), publicationDate);
    }

    private boolean hasMultipleChannelYears(List<String> channelYears) {
        return channelYears.size() > 1;
    }

    private boolean hasMismatchedPublicationDate(String channelYear, PublicationDate publicationDate) {
        return Optional.ofNullable(publicationDate)
                .map(PublicationDate::getYear)
                .map(publicationYear -> !publicationYear.equals(channelYear))
                .orElse(true);
    }

    private static List<String> extractPublicationChannelYears(PublicationContext context) {
        return context.extractPublicationContextUris().stream()
                .map(UriWrapper::fromUri)
                .map(UriWrapper::getLastPathElement)
                .distinct()
                .toList();
    }
}
