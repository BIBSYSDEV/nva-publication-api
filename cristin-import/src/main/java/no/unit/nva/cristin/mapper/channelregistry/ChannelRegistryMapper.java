package no.unit.nva.cristin.mapper.channelregistry;

import com.opencsv.bean.CsvToBeanBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import nva.commons.core.ioutils.IoUtils;

public final class ChannelRegistryMapper {

    public static final String JOURNAL_SERIES_ID_MAPPING_CSV = "channelRegistry/journal_series_id_mapping_251023.csv";
    private static final char SEPARATOR = ';';
    private static final String PATH_PUBLISHER_CSV = "channelRegistry/publisher_id_mapping.csv";

    private final Map<Integer, ChannelRegistryEntry> channelRegisterJournals;

    private final Map<Integer, String> channelRegisterPublishers;

    private ChannelRegistryMapper() {
        this.channelRegisterJournals = getMapWithEntryFromResource(JOURNAL_SERIES_ID_MAPPING_CSV);
        this.channelRegisterPublishers = getMapFromResource(PATH_PUBLISHER_CSV);
    }

    public static ChannelRegistryMapper getInstance() {
        return ChannelRegistrySingletonHelper.INSTANCE;
    }

    public Optional<ChannelRegistryEntry> convertNsdJournalCodeToPid(int nsdCode) {
        return Optional.ofNullable(channelRegisterJournals.get(nsdCode));
    }

    public Optional<String> convertNsdPublisherCodeToPid(int nsdCode) {
        return Optional.ofNullable(channelRegisterPublishers.get(nsdCode));
    }

    private static Map<Integer, String> getMapFromResource(String path) {
        try (var inputStream = IoUtils.inputStreamFromResources(path);
            var bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            var microJournal = new CsvToBeanBuilder<ChannelRegistryRepresentation>(bufferedReader)
                                   .withSeparator(SEPARATOR)
                                   .withType(ChannelRegistryRepresentation.class)
                                   .build();

            return microJournal.stream()
                       .collect(Collectors.toMap(ChannelRegistryRepresentation::getNsdCode,
                                                 ChannelRegistryRepresentation::getPid));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<Integer, ChannelRegistryEntry> getMapWithEntryFromResource(String path) {
        try (var inputStream = IoUtils.inputStreamFromResources(path);
            var bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            var microJournal = new CsvToBeanBuilder<ChannelRegistryRepresentation>(bufferedReader)
                                   .withSeparator(SEPARATOR)
                                   .withType(ChannelRegistryRepresentation.class)
                                   .build();

            return microJournal.stream()
                       .collect(Collectors.toMap(ChannelRegistryRepresentation::getNsdCode,
                                                 ChannelRegistryEntry::fromChannelRegistryRepresentation));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ChannelRegistrySingletonHelper {

        private static final ChannelRegistryMapper INSTANCE = new ChannelRegistryMapper();
    }
}
