package no.unit.nva.publication.indexing;

import static java.util.Map.entry;
import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

public class PublicationChannelGenerator {

    public static final String SAMPLE_JSON_FILENAME = "framed-json/publication_channel_sample.json";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_ID = "id";
    public static final String FIELD_LEVEL = "level";
    public static final String FIELD_CONTEXT = "@context";
    public static final String PUBLICATION_CHANNEL_CONTEXT = "https://bibsysdev.github"
                                                             + ".io/src/publication-channel/channel-context.json";

    public static String getPublicationChannelSampleJournal(URI journalId, String journalName)
        throws JsonProcessingException {
        String publicationChannelSample = stringFromResources(Path.of(SAMPLE_JSON_FILENAME));
        JsonNode channelRoot = objectMapper.readTree(publicationChannelSample);
        
        ((ObjectNode) channelRoot).put(FIELD_ID, journalId.toString());
        ((ObjectNode) channelRoot).put(FIELD_NAME, journalName);
        ((ObjectNode) channelRoot).put(FIELD_LEVEL, "1");
        return objectMapper.writeValueAsString(channelRoot);
    }

    public static String getPublicationChannelJournal(URI identifier, String publisherName)
        throws JsonProcessingException {
        var publisherMap = getPublicationChannelMap(identifier, publisherName);
        return objectMapper.writeValueAsString(publisherMap);
    }

    public static String getPublicationChannelSamplePublisher(URI identifier, String publisherName)
        throws JsonProcessingException {
        var publisherMap = getPublicationChannelMap(identifier, publisherName);
        return objectMapper.writeValueAsString(publisherMap);
    }

    public static String getPublicationChannelSampleSeries(URI seriesId, String seriesName)
        throws JsonProcessingException {
        var seriesMap = getPublicationChannelMap(seriesId, seriesName);
        return objectMapper.writeValueAsString(seriesMap);
    }

    private static Map<String, String> getPublicationChannelMap(URI channelId, String channelName) {
        return Map.ofEntries(
            entry(FIELD_CONTEXT, PUBLICATION_CHANNEL_CONTEXT),
            entry(FIELD_ID, channelId.toString()),
            entry(FIELD_NAME, channelName),
            entry(FIELD_LEVEL, "1"));
    }
}
