package es.arrima.participant.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import org.junit.jupiter.api.Test;

class WhatsAppListParserTest {

    private final WhatsAppListParser parser = new WhatsAppListParser();

    @Test
    void readsATypicalForwardedListIgnoringTheHeaderAndEmptySlots() {
        String message = """
                🎯 MELÉ SÁBADO a las 9:30 en el club
                Apuntaos copiando y reenviando:

                1. Manuel
                2. Paqui 💃
                3.
                4. José Luis García
                5.

                Pagad en la barra 🙏
                """;

        assertThat(names(parser.parse(message))).containsExactly("Manuel", "Paqui 💃", "José Luis García");
        assertThat(parser.parse(message)).extracting(ParsedEntry::listNumber).containsExactly(1, 2, 4);
    }

    @Test
    void acceptsEveryUsualSeparator() {
        String message = """
                1. Uno
                2) Dos
                3- Tres
                4 Cuatro
                5.Cinco
                6 – Seis
                7)Siete
                """;

        assertThat(names(parser.parse(message)))
                .containsExactly("Uno", "Dos", "Tres", "Cuatro", "Cinco", "Seis", "Siete");
    }

    @Test
    void toleratesAMessyListOutOfOrderWithRepeatedNumbersSpacesAndEmojis() {
        String message = """
                Lista melé!!

                   1.   Manuel   García
                3. Paqui
                2.Pepe 👍🏽
                3. Antonio
                ▪️ 4. Lola
                👉5) Juan 🎯🎯

                6.    👍
                """;

        List<ParsedEntry> entries = parser.parse(message);

        assertThat(names(entries)).containsExactly("Manuel García", "Paqui", "Pepe 👍🏽", "Antonio", "Lola", "Juan 🎯🎯");
        // Repeated numbers are kept: the admin decides in the preview.
        assertThat(entries).extracting(ParsedEntry::listNumber).containsExactly(1, 3, 2, 3, 4, 5);
    }

    @Test
    void readsKeycapEmojiNumbers() {
        assertThat(parser.parse("1️⃣ Manuel\n2️⃣ Paqui"))
                .extracting(ParsedEntry::listNumber, ParsedEntry::name)
                .containsExactly(tuple(1, "Manuel"), tuple(2, "Paqui"));
    }

    @Test
    void removesThePrefixWhatsAppAddsWhenCopyingMessages() {
        String copied = """
                [10:31, 4/10/2026] Paqui García: 1. Manuel
                2. Paqui
                4/10/26, 10:35 - Pepe: 3. Pepe
                """;

        assertThat(names(parser.parse(copied))).containsExactly("Manuel", "Paqui", "Pepe");
    }

    @Test
    void removesWhatsAppFormattingAndFlagsStruckNames() {
        List<ParsedEntry> entries = parser.parse("1. *Manuel*\n2. _Paqui_\n3. ~Antonio~");

        assertThat(names(entries)).containsExactly("Manuel", "Paqui", "Antonio");
        assertThat(entries).extracting(ParsedEntry::struckThrough).containsExactly(false, false, true);
    }

    @Test
    void numberedHeaderLinesBeforeTheListAreFlaggedNotDropped() {
        String message = """
                4 de octubre, melé a las 10
                1. Manuel
                2. Paqui
                """;

        List<ParsedEntry> entries = parser.parse(message);

        assertThat(names(entries)).containsExactly("de octubre, melé a las 10", "Manuel", "Paqui");
        assertThat(entries).extracting(ParsedEntry::beforeListStart).containsExactly(true, false, false);
    }

    @Test
    void timesAndYearsAreNotListNumbers() {
        String message = """
                10:30 en la pista 4
                2026 melé de otoño
                2€ por persona
                1. Manuel
                """;

        assertThat(names(parser.parse(message))).containsExactly("Manuel");
    }

    @Test
    void invisibleCharactersAreRemovedAndLongNamesTruncated() {
        String longName = "Bartolomé ".repeat(10);

        List<ParsedEntry> entries = parser.parse("1. Pa​qui‮\n2. " + longName);

        assertThat(entries.get(0).name()).isEqualTo("Paqui");
        assertThat(entries.get(1).name().codePointCount(0, entries.get(1).name().length()))
                .isLessThanOrEqualTo(WhatsAppListParser.MAX_NAME_LENGTH);
    }

    @Test
    void textWithoutAnyListGivesNoEntries() {
        assertThat(parser.parse("Hola, ¿quién viene el sábado?\n\n¡Yo!")).isEmpty();
        assertThat(parser.parse("")).isEmpty();
    }

    private static List<String> names(List<ParsedEntry> entries) {
        return entries.stream().map(ParsedEntry::name).toList();
    }
}
