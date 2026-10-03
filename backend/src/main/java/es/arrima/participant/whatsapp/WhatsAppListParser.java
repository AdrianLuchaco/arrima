package es.arrima.participant.whatsapp;

import es.arrima.shared.text.TextSanitizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the numbered sign-up list that people copy and forward in the club's WhatsApp group:
 * <pre>
 *   MELÉ SÁBADO 🎯 a las 9:30
 *   1. Manuel
 *   2) Paqui 💃
 *   3.
 *   4- *Pepe*
 *   5 ~Antonio~
 * </pre>
 * Rules:
 * <ul>
 *   <li>only lines that start with a number count; the header and other text are ignored;</li>
 *   <li>accepted separators: "1." "1)" "1-" "1 " (and "1.Manuel", en/em dashes, keycap emojis like 1️⃣);</li>
 *   <li>empty numbers ("3.") are skipped; repeated numbers are kept (the admin decides in the preview);</li>
 *   <li>WhatsApp formatting (*bold*, _italic_, ~struck~) is removed; a struck name is flagged;</li>
 *   <li>the "[10:31, 4/10/2026] Paqui: " prefix that WhatsApp adds when copying messages is removed.</li>
 * </ul>
 * Nothing is saved from here: the result is a preview the admin edits before confirming.
 */
public final class WhatsAppListParser {

    public static final int MAX_NAME_LENGTH = 60;

    private static final Pattern COPIED_MESSAGE_PREFIX = Pattern.compile(
            // iPhone: "[10:31, 4/10/2026] Paqui: "   Android: "4/10/26, 10:31 - Paqui: "
            "^(?:\\[[^\\]]*\\d{1,2}:\\d{2}[^\\]]*\\]|\\d{1,2}/\\d{1,2}/\\d{2,4},? \\d{1,2}:\\d{2} -)\\s*[^:]{1,60}:\\s*");
    private static final Pattern KEYCAP_DIGIT = Pattern.compile("(\\d)\\uFE0F?\\u20E3");
    private static final Pattern LEADING_SYMBOLS = Pattern.compile("^[^\\p{L}\\p{N}]+");
    /** A number of up to 3 digits, then a separator (. ) - – —), spaces, or nothing at all. */
    private static final Pattern NUMBERED_LINE = Pattern.compile("^(\\d{1,3})(?:\\s*[.)\\-\\u2013\\u2014]\\s*|\\s+|$)(.*)$");
    private static final Pattern ANY_LETTER = Pattern.compile("\\p{L}");

    public List<ParsedEntry> parse(String text) {
        List<NumberedLine> numberedLines = text.lines()
                .map(WhatsAppListParser::toNumberedLine)
                .filter(Objects::nonNull)
                .toList();
        int listStart = indexOfNumberOne(numberedLines);

        List<ParsedEntry> entries = new ArrayList<>();
        for (int i = 0; i < numberedLines.size(); i++) {
            NumberedLine line = numberedLines.get(i);
            ParsedEntry entry = toEntry(line, i < listStart);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static NumberedLine toNumberedLine(String rawLine) {
        String line = COPIED_MESSAGE_PREFIX.matcher(rawLine.strip()).replaceFirst("");
        line = KEYCAP_DIGIT.matcher(line).replaceAll("$1");
        line = LEADING_SYMBOLS.matcher(line).replaceFirst("");
        Matcher matcher = NUMBERED_LINE.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        return new NumberedLine(Integer.parseInt(matcher.group(1)), matcher.group(2));
    }

    /** The list proper starts at the line numbered 1; if there is none, every numbered line counts. */
    private static int indexOfNumberOne(List<NumberedLine> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).number() == 1) {
                return i;
            }
        }
        return 0;
    }

    private static ParsedEntry toEntry(NumberedLine line, boolean beforeListStart) {
        String text = line.text().strip();
        boolean struckThrough = text.length() > 2 && text.startsWith("~") && text.endsWith("~");
        String name = TextSanitizer.clean(removeFormatting(text));
        if (!ANY_LETTER.matcher(name).find()) {
            return null; // empty slot ("3."), or only emojis or punctuation
        }
        Integer number = line.number() > 0 ? line.number() : null;
        return new ParsedEntry(number, truncate(name), struckThrough, beforeListStart);
    }

    private static String removeFormatting(String text) {
        String withoutMarks = text.replace("*", "").replace("~", "");
        if (withoutMarks.length() > 2 && withoutMarks.startsWith("_") && withoutMarks.endsWith("_")) {
            return withoutMarks.substring(1, withoutMarks.length() - 1);
        }
        return withoutMarks;
    }

    private static String truncate(String name) {
        if (name.codePointCount(0, name.length()) <= MAX_NAME_LENGTH) {
            return name;
        }
        return name.substring(0, name.offsetByCodePoints(0, MAX_NAME_LENGTH)).strip();
    }

    private record NumberedLine(int number, String text) {
    }
}
