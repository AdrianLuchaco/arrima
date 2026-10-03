package es.arrima.shared.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextSanitizerTest {

    @Test
    void collapsesWhitespaceIncludingNonBreakingSpacesAndLineBreaks() {
        assertThat(TextSanitizer.clean("  Manuel  García \n\t López  ")).isEqualTo("Manuel García López");
    }

    @Test
    void removesInvisibleAndControlCharacters() {
        // Zero-width space, a right-to-left override (can disguise text) and a NUL character.
        assertThat(TextSanitizer.clean("Pa​qui‮\u0000")).isEqualTo("Paqui");
    }

    @Test
    void keepsEmojisIncludingComposedOnes() {
        String family = "👨‍👩‍👧"; // man ZWJ woman ZWJ girl
        assertThat(TextSanitizer.clean("Paqui 💃 " + family)).isEqualTo("Paqui 💃 " + family);
    }

    @Test
    void normalisesAccentsToTheirComposedForm() {
        String decomposed = "José"; // "e" + combining acute accent
        assertThat(TextSanitizer.clean(decomposed)).isEqualTo("José");
    }

    @Test
    void leavesHtmlAsPlainText() {
        // Escaping is React's job when rendering; here the text is stored exactly as typed.
        assertThat(TextSanitizer.clean("<b>Manolo</b>")).isEqualTo("<b>Manolo</b>");
    }

    @Test
    void nullBecomesEmpty() {
        assertThat(TextSanitizer.clean(null)).isEmpty();
    }
}
