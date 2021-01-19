package no.unit.nva.doi;

import static java.util.Objects.nonNull;
import java.time.Instant;
import no.unit.nva.doi.handler.exception.DependencyRemoteNvaApiException;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.doi.update.dto.DoiUpdateDto;
import no.unit.nva.publication.doi.update.dto.DoiUpdateHolder;
import no.unit.nva.publication.service.PublicationService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process handling Update DOI Status event for Publication.
 */
public class UpdateDoiStatusProcess {

    public static final String ERROR_BAD_DOI_UPDATE_HOLDER_FORMAT = "Invalid payload for DoiUpdateHolder DTO: %s";
    public static final String MODIFIED_DOI_IS_IN_THE_FUTURE = "Modified doi is in the future, bailing!";
    public static final String DOI_DOES_NOT_MATCH_DOI_IN_PUBLICATION =
        "DOI does not match DOI in Publication, bad update request, bailing!";
    public static final String PUBLICATION_IDENTIFIER_DOES_NOT_LOOK_LIKE_A_ID =
        "Publication Identifier does not look like a id (URI)!";
    public static final String FORWARD_SLASH = "/";
    public static final String UPDATED_PUBLICATION_FORMAT =
        "Updated publication %s with doi: %s which was last modified: %s";
    public static final int NOT_FOUND = -1;
    public static final Object NO_REQUEST_PAYLOAD = null;
    private static final Logger logger = LoggerFactory.getLogger(UpdateDoiStatusProcess.class);
    private final PublicationService publicationService;
    private final DoiUpdateDto request;
    private final Publication publication;

    /**
     * Start a UpdateDoiStatus process.
     *
     * @param publicationService publicationService for accessing publications
     * @param request            {@code DoiUpdateHolder} representing a update doi status result.
     */
    public UpdateDoiStatusProcess(PublicationService publicationService, DoiUpdateHolder request) {
        if (isInvalidPayloadFormat(request)) {
            throw new IllegalArgumentException(
                String.format(ERROR_BAD_DOI_UPDATE_HOLDER_FORMAT,
                    nonNull(request) ? request.toJsonString() : NO_REQUEST_PAYLOAD));
        }
        this.publicationService = publicationService;
        this.request = request.getItem();

        try {
            SortableIdentifier requestedPublicationId = extractPublicationFromRequest();
            this.publication = publicationService.getPublication(requestedPublicationId);
        } catch (ApiGatewayException e) {
            throw DependencyRemoteNvaApiException.wrap(e);
        }
        validateInput();
    }

    /**
     * Update DOI Status.
     */
    public void updateDoiStatus() {
        try {
            publication.setModifiedDate(request.getModifiedDate());
            publication.setDoi(request.getDoi().get());
            publicationService.updatePublication(publication.getIdentifier(), publication);
            logPublicationDoiUpdate();
        } catch (ApiGatewayException e) {
            throw DependencyRemoteNvaApiException.wrap(e);
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
        if (publication.getDoi() != null
            && request.getDoi().isPresent()
            && !publication.getDoi().toString().equals(request.getDoi().get())) {
            throw new IllegalStateException(DOI_DOES_NOT_MATCH_DOI_IN_PUBLICATION);
        }
    }

    private SortableIdentifier extractPublicationFromRequest() {
        String s = request.getPublicationId().toString();
        int beginIndex = s.lastIndexOf(FORWARD_SLASH);
        if (NOT_FOUND == beginIndex) {
            throw new IllegalArgumentException(PUBLICATION_IDENTIFIER_DOES_NOT_LOOK_LIKE_A_ID);
        }
        return new SortableIdentifier(s.substring(++beginIndex));
    }
}
