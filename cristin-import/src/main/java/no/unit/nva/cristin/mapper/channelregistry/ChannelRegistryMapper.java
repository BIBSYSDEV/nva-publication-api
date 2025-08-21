package no.unit.nva.cristin.mapper.channelregistry;

import com.opencsv.bean.CsvToBeanBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import nva.commons.core.ioutils.IoUtils;

public final class ChannelRegistryMapper {

    public static final String JOURNAL_SERIES_ID_MAPPING_CSV = "channelRegistry/journals_series_by_nsd_code.csv";
    private static final char SEPARATOR = ';';
    private static final String PATH_PUBLISHER_ID_CSV = "channelRegistry/publishers_by_nsd_code.csv";
    private static final String PATH_PUBLISHER_NAME_CSV = "channelRegistry/publishers_by_name.csv";
    private static final String PATH_JOURNAL_NAME_CSV = "channelRegistry/journals_by_name_and_issn.csv";

    private final Map<Integer, ChannelRegistryEntry> channelRegisterJournals;
    private final Map<Integer, String> channelRegisterPublishersById;
    private final List<ChannelRegistryPublisherByNameRepresentation> channelRegisterPublishersByName;
    private final List<ChannelRegistryJournalByNameRepresentation> channelRegisterJournalsByName;

    private ChannelRegistryMapper() {
        this.channelRegisterJournals = getMapWithEntryFromResource(JOURNAL_SERIES_ID_MAPPING_CSV);
        this.channelRegisterPublishersById = getMapFromResource(PATH_PUBLISHER_ID_CSV);
        this.channelRegisterPublishersByName = getMapFromPublisherNameList(PATH_PUBLISHER_NAME_CSV);
        this.channelRegisterJournalsByName = getJournalNameList(PATH_JOURNAL_NAME_CSV);
    }

    public static ChannelRegistryMapper getInstance() {
        return ChannelRegistrySingletonHelper.INSTANCE;
    }

    public Optional<ChannelRegistryEntry> convertNsdJournalCodeToPid(int nsdCode) {
        return Optional.ofNullable(channelRegisterJournals.get(nsdCode));
    }

    public Optional<String> convertNsdPublisherCodeToPid(int nsdCode) {
        return Optional.ofNullable(channelRegisterPublishersById.get(nsdCode));
    }

    public Optional<String> convertPublisherNameToPid(String name) {
        return channelRegisterPublishersByName.stream()
                   .filter(entry -> entry.hasTitle(name))
                   .findFirst()
                   .map(ChannelRegistryPublisherByNameRepresentation::getPid);
    }

    public Optional<String> convertJournalNameToPid(String name) {
        return channelRegisterJournalsByName.stream()
                   .filter(entry -> entry.hasTitle(name))
                   .findFirst()
                   .map(ChannelRegistryJournalByNameRepresentation::getPid);
    }

    public Optional<String> convertJournalIssnToPid(String issn) {
        return channelRegisterJournalsByName.stream()
                   .filter(entry -> entry.hasIssn(issn))
                   .findFirst()
                   .map(ChannelRegistryJournalByNameRepresentation::getPid);
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

    private static List<ChannelRegistryPublisherByNameRepresentation> getMapFromPublisherNameList(String path) {
        try (var inputStream = IoUtils.inputStreamFromResources(path);
            var bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            return new CsvToBeanBuilder<ChannelRegistryPublisherByNameRepresentation>(bufferedReader)
                                   .withSeparator(SEPARATOR)
                                   .withType(ChannelRegistryPublisherByNameRepresentation.class)
                                   .build()
                                   .stream()
                                   .toList();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ChannelRegistryJournalByNameRepresentation> getJournalNameList(String path) {
        try (var inputStream = IoUtils.inputStreamFromResources(path);
            var bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            return new CsvToBeanBuilder<ChannelRegistryJournalByNameRepresentation>(bufferedReader)
                       .withSeparator(SEPARATOR)
                       .withType(ChannelRegistryJournalByNameRepresentation.class)
                       .build()
                       .stream()
                       .toList();

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

    private static final class ChannelRegistrySingletonHelper {

        private static final ChannelRegistryMapper INSTANCE = new ChannelRegistryMapper();
    }
}
