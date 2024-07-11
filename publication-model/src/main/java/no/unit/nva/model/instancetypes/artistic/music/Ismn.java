package no.unit.nva.model.instancetypes.artistic.music;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;

@JsonTypeInfo(use = Id.NAME, property = "type")
@SuppressWarnings("PMD.CognitiveComplexity")
public class Ismn {

    public static final Map<String, Integer> REGISTRANTS = Map.of(
        "0", 4,
        "1", 5,
        "2", 5,
        "3", 5,
        "4", 6,
        "5", 6,
        "6", 6,
        "7", 7,
        "8", 7,
        "9", 8);
    public static final Set<Character> validDelimiters = Set.of(' ', '-');
    public static final String FORMATTED_ISMN_TEMPLATE = "%s-%s-%s-%s";
    public static final String INVALID_ISMN_TEMPLATE = "The ISMN %s is invalid";
    public static final int EVEN_MULTIPLIER = 3;
    public static final int ODD_MULTIPLIER = 1;
    public static final char ISMN_10_PREFIX = 'M';
    public static final int UNSET = -1;
    public static final int PREFIX_CALCULATED_DEFAULT = 39;
    public static final int REGISTRANT_ITEM_LENGTH = 8;
    public static final int STARTING_STATE = 0;
    public static final String VALUE_FIELD = "value";
    public static final String FORMATTED_FIELD = "formatted";
    private static final List<Character> VALID_MUMERICS = List.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');
    private Prefix prefix;
    private String publisherId;
    private String itemId;
    private String checkBit;

    /**
     * Provides a parsed, validated ISMN 10 or ISMN 13.
     *
     * <p>The method parses the prefix, publisher identifier part, item identifier part
     * and check bit and validates these.
     *
     * <p>In essence, this constructor demonstrates why semantically meaningful identifiers
     * are a terrible idea.
     *
     * @param ismn A string representation of an ISMN.
     * @throws InvalidIsmnException If the parsing or validation fails.
     */
    @JsonCreator
    public Ismn(@JsonProperty(VALUE_FIELD) String ismn) throws InvalidIsmnException {

        if (isNull(ismn)) {
            return;
        }

        extractIsmnParts(ismn, ismn.toUpperCase(Locale.ENGLISH));
    }

    @JsonProperty(VALUE_FIELD)
    public String value() {
        return prefix.prefix + publisherId + itemId + checkBit;
    }

    @JsonProperty(FORMATTED_FIELD)
    public String formatted() {
        return String.format(FORMATTED_ISMN_TEMPLATE, prefix.formattedPrefix, publisherId, itemId, checkBit);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(prefix, publisherId, itemId, checkBit);
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ismn)) {
            return false;
        }
        Ismn ismn = (Ismn) o;
        return prefix == ismn.prefix && Objects.equals(publisherId, ismn.publisherId) && Objects.equals(
            itemId, ismn.itemId) && Objects.equals(checkBit, ismn.checkBit);
    }

    @Override
    public String toString() {
        return value();
    }

    private static boolean isEven(int counter) {
        return counter % 2 == 0;
    }

    private static int calculateModuloTenDifference(int sum) {
        var modulo = sum % 10;
        return modulo != 0 ? 10 - modulo : 0;
    }

    private static void throwInvalidIsmnException(String candidate) throws InvalidIsmnException {
        throw new InvalidIsmnException(String.format(INVALID_ISMN_TEMPLATE, candidate));
    }

    private void extractIsmnParts(String ismn, String candidate) throws InvalidIsmnException {
        Prefix prefix = null;
        var registrant = new StringBuilder();
        var item = new StringBuilder();
        var checkBit = UNSET;

        var checksum = STARTING_STATE;
        var parsedPart = new StringBuilder();
        var itemStart = UNSET;

        for (int count = STARTING_STATE; count < candidate.length(); count++) {
            var current = candidate.charAt(count);

            if (isIrrelevant(count, current)) {
                continue;
            } else {
                parsedPart.append(current);
            }

            if (isPrefixEnd(parsedPart, prefix, current)) {
                try {
                    prefix = Prefix.find(parsedPart.toString());
                    checksum = PREFIX_CALCULATED_DEFAULT;
                    continue;
                } catch (IllegalArgumentException e) {
                    throwInvalidIsmnException(ismn);
                }
            }

            if (isIncompletePrefix(parsedPart, prefix)) {
                continue;
            }

            checkBit = getCheckBit(prefix, registrant, item, current);

            checksum = updateChecksum(checksum, parsedPart, Character.getNumericValue(current), checkBit);

            itemStart = getItemStart(prefix, registrant, itemStart, current);

            if (isPublisherIdPart(parsedPart, prefix, itemStart)) {
                registrant.append(current);
                continue;
            }

            if (isItemPart(registrant, item)) {
                item.append(current);
                continue;
            }

            if (calculateModuloTenDifference(checksum) == checkBit) {
                continue;
            }

            throwInvalidIsmnException(ismn);
        }

        applyIsmn(ismn, prefix, registrant.toString(), item.toString(), checkBit);
    }

    private int getItemStart(Prefix prefix, StringBuilder registrant, int itemStart, char current) {
        if (itemStart == UNSET && nonNull(prefix) && isPublisherIdStart(prefix, registrant)) {
            return REGISTRANTS.get(String.valueOf(current));
        }
        return itemStart;
    }

    private boolean isIrrelevant(int count, char current) {
        return isFormattingCharacter(current) || isInvalidCharacter(count, current);
    }

    private boolean isInvalidCharacter(int count, char character) {
        return (count != STARTING_STATE || character != ISMN_10_PREFIX) && !VALID_MUMERICS.contains(character);
    }

    private int getCheckBit(Prefix prefix, StringBuilder registrant, StringBuilder item, char current) {
        if (isCheckBit(prefix, registrant, item)) {
            return Character.getNumericValue(current);
        }
        return UNSET;
    }

    private int updateChecksum(int checksum, StringBuilder parsedPart, int charAsInt, int checkBit) {
        if (checkBit == UNSET) {
            return isEven(calculateParsedPartLogicalLength(parsedPart.toString()))
                       ? checksum + (EVEN_MULTIPLIER * charAsInt)
                       : checksum + (ODD_MULTIPLIER * charAsInt);
        }
        return checksum;
    }

    private boolean isItemPart(StringBuilder registrant, StringBuilder item) {
        return registrant.length() + item.length() < REGISTRANT_ITEM_LENGTH;
    }

    private boolean isPublisherIdPart(StringBuilder parsedPart, Prefix prefix, Integer itemStart) {
        return itemStart != UNSET && parsedPart.length() - prefix.getLength() < itemStart;
    }

    private boolean isPublisherIdStart(Prefix prefix, StringBuilder registrant) {
        return nonNull(prefix) && registrant.length() == STARTING_STATE;
    }

    private boolean isCheckBit(Prefix prefix, StringBuilder registrant, StringBuilder item) {
        return nonNull(prefix) && registrant.length() + item.length() == REGISTRANT_ITEM_LENGTH;
    }

    private boolean isIncompletePrefix(StringBuilder parsedPart, Prefix prefix) {
        return isNull(prefix) && parsedPart.length() < Prefix.ISMN13.getLength();
    }

    private boolean isPrefixEnd(StringBuilder parsedPart, Prefix prefix, char current) {
        return isNull(prefix) && (current == ISMN_10_PREFIX || parsedPart.length() == Prefix.ISMN13.getLength());
    }

    private boolean isFormattingCharacter(char current) {
        return validDelimiters.contains(current);
    }

    private void applyIsmn(String ismn, Prefix prefix, String registrant, String item, int checkBit)
        throws InvalidIsmnException {
        if (checkBit == UNSET) {
            throwInvalidIsmnException(ismn);
        } else {
            this.prefix = prefix;
            this.publisherId = registrant;
            this.itemId = item;
            this.checkBit = String.valueOf(checkBit);
        }
    }

    private int calculateParsedPartLogicalLength(String parsedPart) {
        var length = parsedPart.length();
        return parsedPart.startsWith(String.valueOf(ISMN_10_PREFIX)) ? compensateForIsmn10Prefix(length) : length;
    }

    private int compensateForIsmn10Prefix(int length) {
        return length - 1 + Prefix.ISMN13.getLength();
    }

    protected enum Prefix {

        ISMN10(1, "M", "M"),
        ISMN13(4, "9790", "979-0");

        private final int length;
        private final String prefix;
        private final String formattedPrefix;

        Prefix(int length, String prefix, String formattedPrefix) {
            this.length = length;
            this.prefix = prefix;
            this.formattedPrefix = formattedPrefix;
        }

        public static Prefix find(String prefix) {
            var attempt = Arrays.stream(values())
                .filter(p -> p.prefix.equals(prefix))
                .collect(SingletonCollector.tryCollect());
            if (attempt.isSuccess()) {
                return attempt.get();
            } else {
                throw new IllegalArgumentException("Invalid ISMN prefix: " + prefix);
            }
        }

        public String getString() {
            return prefix;
        }

        private int getLength() {
            return length;
        }
    }
}

