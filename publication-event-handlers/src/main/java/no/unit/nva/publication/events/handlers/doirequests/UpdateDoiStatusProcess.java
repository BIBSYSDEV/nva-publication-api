package no.unit.nva.publication.events.handlers.doirequests;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.doi.update.dto.DoiUpdateDto;
import no.unit.nva.publication.doi.update.dto.DoiUpdateHolder;

import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateDoiStatusProcess {

    public static final String ERROR_BAD_DOI_UPDATE_HOLDER_FORMAT = "Invalid payload for DoiUpdateHolder DTO: %s";
    public static final String MODIFIED_DOI_IS_IN_THE_FUTURE = "Modified doi is in the future, bailing!";
    public static final String DOI_DOES_NOT_MATCH_DOI_IN_PUBLICATION =
        "DOI does not match DOI in Publication, bad update request, bailing!";

    public static final String UPDATED_PUBLICATION_FORMAT =
        "Updated publication %s with doi: %s which was last modified: %s";
    public static final Object NO_REQUEST_PAYLOAD = null;
    private static final Logger logger = LoggerFactory.getLogger(UpdateDoiStatusProcess.class);
    private final ResourceService resourceService;
    private final DoiUpdateDto request;
    private final Publication publication;

    /**
     * Start a UpdateDoiStatus process.
     *
     * @param resourceService publicationService for accessing publications
     * @param request         {@code DoiUpdateHolder} representing a update doi status result.
     */
    public UpdateDoiStatusProcess(ResourceService resourceService, DoiUpdateHolder request) {
        if (isInvalidPayloadFormat(request)) {
            throw new IllegalArgumentException(
                String.format(ERROR_BAD_DOI_UPDATE_HOLDER_FORMAT,
                              nonNull(request) ? request.toJsonString() : NO_REQUEST_PAYLOAD));
        }
        this.resourceService = resourceService;
        this.request = request.getItem();

        SortableIdentifier requestedPublicationId = request.getItem().getPublicationIdentifier();
        this.publication = fetchPublication(resourceService, requestedPublicationId);

        validateInput();
    }

    public void updateDoi() {
        publication.setModifiedDate(request.getModifiedDate());
        publication.setDoi(request.getDoi().orElseThrow());
        resourceService.updatePublication(publication);
        logPublicationDoiUpdate();
    }

    private Publication fetchPublication(ResourceService resourceService,
                                         SortableIdentifier requestedPublicationId) {
        return attempt(() -> resourceService.getPublicationByIdentifier(requestedPublicationId))
            .orElseThrow(this::handleNotFoundException);
    }

    private DependencyRemoteNvaApiException handleNotFoundException(Failure<Publication> fail) {
        Exception exception = fail.getException();
        if (exception instanceof NotFoundException) {
            return DependencyRemoteNvaApiException.wrap((NotFoundException) exception);
        } else {
            return new DependencyRemoteNvaApiException("Unknown exception: ", exception);
        }
    }

    private void logPublicationDoiUpdate() {
        logger.info(String.format(UPDATED_PUBLICATION_FORMAT,
                                  publication.getIdentifier(),
                                  publication.getDoi(),
                                  publication.getModifiedDate()));
    }

    private boolean isInvalidPayloadFormat(DoiUpdateHolder request) {
        return request == null || !request.hasItem() || !request.getItem().hasAllRequiredValuesSet();
    }

    private void validateInput() {
        if (request.getModifiedDate().isAfter(Instant.now())) {
            throw new IllegalStateException(MODIFIED_DOI_IS_IN_THE_FUTURE);
        }
        if (publicationHasAlreadyDoiAndDoiIsDifferent()) {
            throw new IllegalStateException(DOI_DOES_NOT_MATCH_DOI_IN_PUBLICATION);
        }
    }

    private boolean publicationHasAlreadyDoiAndDoiIsDifferent() {
        return
            nonNull(publication.getDoi())
            && request.getDoi().isPresent()
            && !publication.getDoi().equals(request.getDoi().orElseThrow());
    }
}
