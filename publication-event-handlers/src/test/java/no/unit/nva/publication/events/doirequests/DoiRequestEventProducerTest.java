package no.unit.nva.publication.events.doirequests;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.events.handlers.EventParser;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.doi.update.dto.PublicationHolder;
import no.unit.nva.publication.events.DynamoEntryUpdateEvent;
import nva.commons.core.ioutils.IoUtils;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.file.Path;

import static no.unit.nva.publication.events.PublicationEventsConfig.dynamoImageSerializerRemovingEmptyFields;
import static no.unit.nva.publication.events.doirequests.DoiRequestEventProducer.EMPTY_EVENT;
import static no.unit.nva.publication.events.doirequests.DoiRequestEventProducer.NO_RESOURCE_IDENTIFIER_ERROR;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DoiRequestEventProducerTest {

    private static final String DOI_FIELD = "doi";
    private static final JsonPointer NEW_DATA_FIELD = JsonPointer.compile(
        "/detail/responsePayload/newData");
    private static final String EVENT_PUBLICATION_WITH_DOI_IS_UPDATED =
        "doirequests/resource_update_event_updated_metadata_with_existing_doi.json";
    private static final String RESOURCE_UPDATE_EVENT_OLD_AND_NEW_PRESENT_DIFFERENT_NEW_HAS_DOI =
        "doirequests/resource_update_event_old_and_new_present_different_new_has_doi.json";
    private static final String RESOURCE_UPDATE_EVENT_OLD_AND_NEW_PRESENT_DIFFERENT_NO_DOI =
        "doirequests/resource_update_event_old_and_new_present_different_no_doi.json";
    private static final String RESOURCE_UPDATE_EVENT_OLD_AND_NEW_PRESENT_EQUAL =
        "doirequests/resource_update_event_old_and_new_present_equal.json";
    private static final String RESOURCE_UPDATE_EVENT_OLD_ONLY =
        "doirequests/resource_update_event_old_only.json";
    private static final String RESOURCE_UPDATE_NEW_IMAGE_ONLY_WITH_DOI =
        "doirequests/resource_update_event_new_image_only_with_doi.json";
    private static final String RESOURCE_UPDATE_EVENT_WITHOUT_IDENTIFIER =
            "doirequests/resource_update_event_without_identifier.json";
    private static final String RESOURCE_UPDATE_EVENT_DOI_REQUEST_APPROVED =
            "doirequests/resource_update_event_doi_request_approved_for_published_publication.json";
    private static final String EVENT_PUBLICATION_UPDATED_ONLY_BY_MODIFIED_DATE =
        "doirequests/resource_update_event_old_and_new_present_with_doi_and_different_modified_date.json";

    private static final Javers JAVERS = JaversBuilder.javers().build();
    private DoiRequestEventProducer handler;
    private Context context;
    private ByteArrayOutputStream outputStream;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        handler = new DoiRequestEventProducer();
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void handleRequestThrowsExceptionWhenEventContainsResourceUpdateWithoutIdentifier() {
        var eventInputStream = IoUtils.inputStreamFromResources(RESOURCE_UPDATE_EVENT_WITHOUT_IDENTIFIER);
        Executable action = () -> handler.handleRequest(eventInputStream, outputStream, context);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        assertThat(exception.getMessage(), is(equalTo(NO_RESOURCE_IDENTIFIER_ERROR)));
    }

    @Test
    void handlerCreatesUpdateDoiEventWhenThereIsNoPreviousDoiRequestButThereIsDoi() throws JsonProcessingException {
        String eventInput = IoUtils.stringFromResources(Path.of(RESOURCE_UPDATE_NEW_IMAGE_ONLY_WITH_DOI));
        assertTrue(hasDoiField(eventInput));

        handler.handleRequest(IoUtils.stringToStream(eventInput), outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);
        assertThat(actual.getType(), is(equalTo(DoiRequestEventProducer.EMPTY_EVENT_TYPE)));
        assertThat(actual.getItem(), nullValue());
        //assertThat(actual.getType(), is(equalTo(DoiRequestEventProducer.TYPE_UPDATE_EXISTING_DOI)));
        //assertThat(actual.getItem(), notNullValue());
    }

    @Test
    void handlerCreatesUpdateDoiEventWhenThereIsPreviousDoiRequestAndThereIsDoi() throws JsonProcessingException {
        String eventInput = IoUtils.stringFromResources(Path.of(
            RESOURCE_UPDATE_EVENT_OLD_AND_NEW_PRESENT_DIFFERENT_NEW_HAS_DOI));
        assertTrue(hasDoiField(eventInput));

        handler.handleRequest(IoUtils.stringToStream(eventInput), outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);
        assertThat(actual.getType(), is(equalTo(DoiRequestEventProducer.TYPE_UPDATE_EXISTING_DOI)));
        assertThat(actual.getItem(), notNullValue());
    }

    @Test
    void handlerCreatesUpdateDoiEventWhenThereIsPreviousDoiRequestAndThereIsNoDoi() throws JsonProcessingException {
        String eventInput = IoUtils.stringFromResources(Path.of(
            RESOURCE_UPDATE_EVENT_OLD_AND_NEW_PRESENT_DIFFERENT_NO_DOI));
        assertTrue(hasDoiField(eventInput));

        handler.handleRequest(IoUtils.stringToStream(eventInput), outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);
        assertThat(actual.getType(), is(equalTo(DoiRequestEventProducer.TYPE_UPDATE_EXISTING_DOI)));
        assertThat(actual.getItem(), notNullValue());
    }

    @Test
    void handlerCreatesOutputWithNonEmptyDoiWhenNewImageHasPublicationWithDoi() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources(EVENT_PUBLICATION_WITH_DOI_IS_UPDATED);
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);
        URI doiInResourceFile = URI.create("https://doi.org/10.1103/physrevd.100.085005");
        URI actualDoi = actual.getItem().getDoi();
        assertThat(actualDoi, is(equalTo(doiInResourceFile)));
    }

    @Test
    void handlerCreatesNoOutputWhenPublicationUpdateDiffersOnlyInModifiedDate() throws JsonProcessingException {
        var event =
            IoUtils.stringFromResources(Path.of(EVENT_PUBLICATION_UPDATED_ONLY_BY_MODIFIED_DATE));

        assertThatEventsDifferOnlyInModifiedDate(event);

        var eventInputStream = IoUtils.stringToStream(event);
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);

        assertThat(actual, is(equalTo(EMPTY_EVENT)));
    }

    @Test
    void processInputSkipsCreatingDtosWhenNoNewImageIsPresentInDao() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources((RESOURCE_UPDATE_EVENT_OLD_ONLY));
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);
        assertThat(actual, is(equalTo(EMPTY_EVENT)));
    }

    @Test
    void processInputCreatesDtosWhenOldAndNewImageAreDifferent() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources(
            RESOURCE_UPDATE_EVENT_OLD_AND_NEW_PRESENT_DIFFERENT_NEW_HAS_DOI);
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);

        assertThat(actual.getType(),
                   is(equalTo(DoiRequestEventProducer.TYPE_UPDATE_EXISTING_DOI)));
        assertThat(actual.getItem(), notNullValue());
    }

    @Test
    void processInputSkipsCreatingDtosWhenOldAndNewImageAreEqual() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources(RESOURCE_UPDATE_EVENT_OLD_AND_NEW_PRESENT_EQUAL);
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);

        assertThat(actual, is(equalTo(EMPTY_EVENT)));
    }

    @Test
    void handlerCreatesEventWhenDoiRequestIsApprovedForPublishedPublication() throws JsonProcessingException {
        var eventInputStream = IoUtils.inputStreamFromResources(RESOURCE_UPDATE_EVENT_DOI_REQUEST_APPROVED);
        handler.handleRequest(eventInputStream, outputStream, context);
        PublicationHolder actual = outputToPublicationHolder(outputStream);

        assertThat(actual.getType(), is(equalTo(DoiRequestEventProducer.TYPE_UPDATE_EXISTING_DOI)));
        assertThat(actual.getItem(), notNullValue());
    }

    private void assertThatEventsDifferOnlyInModifiedDate(String event) {
        AwsEventBridgeEvent<AwsEventBridgeDetail<DynamoEntryUpdateEvent>> eventObject = parseEvent(event);

        Publication newPublication = eventObject.getDetail().getResponsePayload().getNewData().toPublication();
        Publication oldPublication = eventObject.getDetail().getResponsePayload().getOldData().toPublication();

        assertThat(newPublication, is(not(equalTo(oldPublication))));
        assertThatNewAndOldDifferOnlyInModifiedDate(newPublication, oldPublication);
    }

    private void assertThatNewAndOldDifferOnlyInModifiedDate(Publication newPublication, Publication oldPublication) {
        Publication newFromOld = oldPublication.copy().build();
        newFromOld.setModifiedDate(newPublication.getModifiedDate());
        newFromOld.getDoiRequest().setModifiedDate(newPublication.getDoiRequest().getModifiedDate());

        Diff diff = JAVERS.compare(newFromOld, newPublication);
        assertThat(diff.prettyPrint(), newPublication, is((equalTo(newFromOld))));
    }

    @SuppressWarnings("unchecked")
    private AwsEventBridgeEvent<AwsEventBridgeDetail<DynamoEntryUpdateEvent>> parseEvent(String event) {
        EventParser<AwsEventBridgeDetail<DynamoEntryUpdateEvent>> eventEventParser =
            new EventParser<>(event, dynamoImageSerializerRemovingEmptyFields);
        return (AwsEventBridgeEvent<AwsEventBridgeDetail<DynamoEntryUpdateEvent>>)
                   eventEventParser.parse(AwsEventBridgeDetail.class, DynamoEntryUpdateEvent.class);
    }

    private boolean hasDoiField(String eventInput) {
        JsonNode event = attempt(() -> dynamoImageSerializerRemovingEmptyFields.readTree(eventInput)).orElseThrow();
        return event.at(NEW_DATA_FIELD).has(DOI_FIELD);
    }

    private PublicationHolder outputToPublicationHolder(ByteArrayOutputStream outputStream)
        throws JsonProcessingException {
        String outputString = outputStream.toString();
        return dynamoImageSerializerRemovingEmptyFields.readValue(outputString, PublicationHolder.class);
    }
}