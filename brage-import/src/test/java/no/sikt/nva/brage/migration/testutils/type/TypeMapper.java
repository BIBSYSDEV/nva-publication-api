package no.sikt.nva.brage.migration.testutils.type;

import static java.util.Map.entry;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.brage.migration.record.ErrorDetails.Error.MANY_UNMAPPABLE_TYPES;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.record.ErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(TypeMapper.class);
    private static final Map<Set<BrageType>, NvaType> TYPE_MAP = Map.ofEntries(
        entry(Set.of(BrageType.BOOK, BrageType.PEER_REVIEWED), NvaType.SCIENTIFIC_MONOGRAPH),
        entry(Set.of(BrageType.CHAPTER, BrageType.PEER_REVIEWED), NvaType.SCIENTIFIC_CHAPTER),
        entry(Set.of(BrageType.JOURNAL_ARTICLE, BrageType.PEER_REVIEWED), NvaType.SCIENTIFIC_ARTICLE),
        entry(Set.of(BrageType.BOOK), NvaType.BOOK),
        entry(Set.of(BrageType.CHAPTER), NvaType.CHAPTER),
        entry(Set.of(BrageType.JOURNAL_ARTICLE), NvaType.JOURNAL_ARTICLE),
        entry(Set.of(BrageType.DATASET), NvaType.DATASET),
        entry(Set.of(BrageType.REPORT), NvaType.REPORT),
        entry(Set.of(BrageType.RESEARCH_REPORT), NvaType.RESEARCH_REPORT),
        entry(Set.of(BrageType.BACHELOR_THESIS), NvaType.BACHELOR_THESIS),
        entry(Set.of(BrageType.MASTER_THESIS), NvaType.MASTER_THESIS),
        entry(Set.of(BrageType.DOCTORAL_THESIS), NvaType.DOCTORAL_THESIS),
        entry(Set.of(BrageType.STUDENT_PAPER), NvaType.STUDENT_PAPER),
        entry(Set.of(BrageType.WORKING_PAPER), NvaType.WORKING_PAPER),
        entry(Set.of(BrageType.STUDENT_PAPER_OTHERS), NvaType.STUDENT_PAPER_OTHERS),
        entry(Set.of(BrageType.DESIGN_PRODUCT), NvaType.DESIGN_PRODUCT),
        entry(Set.of(BrageType.CHRONICLE), NvaType.CHRONICLE),
        //        entry(Set.of(BrageType.SOFTWARE), NvaType.SOFTWARE),
        //        entry(Set.of(BrageType.RECORDING_ORAL), NvaType.RECORDING_ORAL),
        entry(Set.of(BrageType.LECTURE), NvaType.LECTURE),
        entry(Set.of(BrageType.RECORDING_MUSICAL), NvaType.RECORDING_MUSICAL),
        entry(Set.of(BrageType.PLAN_OR_BLUEPRINT), NvaType.PLAN_OR_BLUEPRINT),
        entry(Set.of(BrageType.MAP), NvaType.MAP)
    );

    public static String convertBrageTypeToNvaType(List<String> inputTypes) {
        try {
            return mapToNvaTypeIfMappable(inputTypes);
        } catch (Exception e) {
            return mapToAnyMappableNvaTypeWhenUnmappableTypePair(inputTypes);
        }
    }

    public static boolean hasValidType(String inputType) {
        var brageType = convertToBrageType(inputType);
        if (nonNull(brageType)) {
            return TYPE_MAP.containsKey(Collections.singleton(brageType));
        } else {
            return false;
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private static String mapToAnyMappableNvaTypeWhenUnmappableTypePair(List<String> inputTypes) {
        List<BrageType> brageTypes = convertToBrageTypes(inputTypes);
        if (brageTypes.isEmpty()) {
            return null;
        } else {
            for (BrageType type : brageTypes) {
                if (hasValidType(type.toString())) {
                    logger.error(String.valueOf(new ErrorDetails(MANY_UNMAPPABLE_TYPES, inputTypes)));
                    return TYPE_MAP.get(Collections.singleton(type)).getValue();
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    private static List<BrageType> convertToBrageTypes(List<String> inputTypes) {
        return inputTypes.stream()
                   .map(BrageType::fromValue)
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    private static String mapToNvaTypeIfMappable(List<String> inputTypes) {
        List<BrageType> brageTypes = convertToBrageTypes(inputTypes);
        var nvaType = TYPE_MAP.get(Set.copyOf(brageTypes));
        if (isNull(nvaType) && brageTypes.size() >= 2) {
            for (BrageType type : brageTypes) {
                if (hasValidType(type.toString())) {
                    return TYPE_MAP.get(Collections.singleton(type)).getValue();
                }
            }
        }
        if (nonNull(nvaType)) {
            return nvaType.getValue();
        } else {
            return null;
        }
    }

    private static BrageType convertToBrageType(String brageType) {
        return BrageType.fromValue(brageType);
    }
}
