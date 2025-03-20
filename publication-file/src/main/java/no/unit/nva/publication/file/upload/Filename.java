package no.unit.nva.publication.file.upload;

import org.apache.commons.text.translate.UnicodeEscaper;
import org.apache.commons.text.translate.UnicodeUnescaper;

public final class Filename {

    private static final String CONTENT_DISPOSITION_TEMPLATE = "filename=\"%s\"";
    private static final String PREFIX = "filename=\"";
    private static final String SUFFIX = "\"";
    public static final int LAST_ASCII_CODEPOINT = 127;
    private static final UnicodeUnescaper UNESCAPER = new UnicodeUnescaper();
    private static final UnicodeEscaper ESCAPER = UnicodeEscaper.above(LAST_ASCII_CODEPOINT);

    private Filename() {

    }

    public static String toContentDispositionValue(String value) {
        var escapedFilename = ESCAPER.translate(value);
        return String.format(CONTENT_DISPOSITION_TEMPLATE, escapedFilename);
    }

    public static String fromContentDispositionValue(String value) {
        var filename = getFilenameFromContentDisposition(value);
        return UNESCAPER.translate(filename);
    }

    private static String getFilenameFromContentDisposition(String value) {
        return (value.startsWith(PREFIX) && value.endsWith(SUFFIX))
                   ? value.substring(PREFIX.length(), value.length() - SUFFIX.length())
                   : value;
    }
}
