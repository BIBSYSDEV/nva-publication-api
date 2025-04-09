package no.unit.nva.publication.sanitizer;

import no.unit.nva.model.EntityDescription;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class EntityDescriptionSanitizerTest {

    private static final String EMPTY_STRING = "";

    @ParameterizedTest
    @MethodSource("validInputProvider")
    void shouldAllowValidInputInEntityDescription(String validInput) {
        var randomString = randomString();
        var entityDescription = new EntityDescription.Builder()
                        .withMainTitle(validInput)
                        .withAlternativeTitles(Map.of(randomString, validInput))
                        .withAbstract(validInput)
                        .withAlternativeAbstracts(Map.of(randomString, validInput))
                        .build();

        var sanitized = EntityDescriptionSanitizer.sanitize(entityDescription);

        assertThat(sanitized.getMainTitle(), equalTo(validInput));
        assertThat(sanitized.getAlternativeTitles().get(randomString), equalTo(validInput));
        assertThat(sanitized.getAbstract(), equalTo(validInput));
        assertThat(sanitized.getAlternativeAbstracts().get(randomString), equalTo(validInput));
    }

    @ParameterizedTest
    @MethodSource("dangerousInputProvider")
    void shouldSanitizeInputInEntityDescription(String dangerousInput, String expectedSanitizedInput) {
        var randomString = randomString();
        var entityDescription = new EntityDescription.Builder()
                .withMainTitle(dangerousInput)
                .withAlternativeTitles(Map.of(randomString, dangerousInput))
                .withAbstract(dangerousInput)
                .withAlternativeAbstracts(Map.of(randomString, dangerousInput))
                .build();

        var sanitized = EntityDescriptionSanitizer.sanitize(entityDescription);

        assertThat(sanitized.getMainTitle(), equalTo(expectedSanitizedInput));
        assertThat(sanitized.getAlternativeTitles().get(randomString), equalTo(expectedSanitizedInput));
        assertThat(sanitized.getAbstract(), equalTo(expectedSanitizedInput));
        assertThat(sanitized.getAlternativeAbstracts().get(randomString), equalTo(expectedSanitizedInput));
    }

    private static Stream<Arguments> validInputProvider() {
        return Stream.of(
                argumentSet("Plain text", "Plain text"),
                argumentSet("Paragraph tag", "<p>Paragraph</p>"),
                argumentSet("Italic tag", "<i>Italic</i>"),
                argumentSet("Em tag", "<em>Em</em>"),
                argumentSet("Bold tag", "<b>Bold</b>"),
                argumentSet("Strong tag", "<strong>Strong</strong>"),
                argumentSet("Link tag with rel=nofollow", "<a href=\"https://www.nva.sikt.no\" rel=\"nofollow\">Sikt</a>"),
                argumentSet("Link tag with target blank and rel=nofollow noopener noreferrer", "<a href=\"https://www.nva.sikt.no\" target=\"blank\" rel=\"nofollow noopener noreferrer\">Sikt</a>"),
                argumentSet("Link tag with target _blank and rel=nofollow noopener noreferrer", "<a href=\"https://www.nva.sikt.no\" target=\"_blank\" rel=\"nofollow noopener noreferrer\">Sikt</a>"),
                argumentSet("Special characters", "Special characters $€§!#%/|(){}[]-*?^¨_.:,;"),
                argumentSet("Backslash", "\\"),
                argumentSet("Newlines", "\r \n \r\n"),
                argumentSet("Æøå", "ÆØÅ æøå"),
                argumentSet("Encoded inequality", "3 &gt; 2 and 2 &lt; 3"),
                argumentSet("Line break", "<br />")
        );
    }

    private static Stream<Arguments> dangerousInputProvider() {
        return Stream.of(
                argumentSet("Script tag", "<script>alert('Oh no!');</script>", EMPTY_STRING),
                argumentSet("Backtick", "`", "&#96;"),
                argumentSet("Quotation mark", "\"", "&#34;"),
                argumentSet("Apostrophe", "'", "&#39;"),
                argumentSet("Ampersand", "&", "&amp;"),
                argumentSet("Img tag", "<img src=x />", EMPTY_STRING),
                argumentSet("U tag", "<u>Underline</u>", "Underline"),
                argumentSet("Sup tag", "<sub>Sub</sub>", "Sub"),
                argumentSet("Sub tag", "<sup>Sup</sup>", "Sup"),
                argumentSet("Link tag with onclick", "<a href=\"https://www.nva.sikt.com\" onclick=\"alert('Oh no!')\">Sikt</a>", "<a href=\"https://www.nva.sikt.com\" rel=\"nofollow\">Sikt</a>"),
                argumentSet("Link tag without href", "<a>Sikt</a>", "Sikt"),
                argumentSet("Link tag without rel=nofollow", "<a href=\"https://www.nva.sikt.com\">Sikt</a>", "<a href=\"https://www.nva.sikt.com\" rel=\"nofollow\">Sikt</a>"),
                argumentSet("Link tag with rel=noopener noreferrer but without target", "<a href=\"https://www.nva.sikt.com\" rel=\"noopener noreferrer\">Sikt</a>", "<a href=\"https://www.nva.sikt.com\" rel=\"nofollow\">Sikt</a>"),
                argumentSet("Link tag with target and rel=noopener noreferrer nofollow will change order of rel", "<a href=\"https://www.nva.sikt.com\" target=\"_blank\" rel=\"noopener noreferrer nofollow\">Sikt</a>", "<a href=\"https://www.nva.sikt.com\" target=\"_blank\" rel=\"nofollow noopener noreferrer\">Sikt</a>"),
                argumentSet("Link tag with target and without rel=", "<a href=\"https://www.nva.sikt.com\" target=\"_blank\">Sikt</a>", "<a href=\"https://www.nva.sikt.com\" target=\"_blank\" rel=\"nofollow noopener noreferrer\">Sikt</a>"),
                argumentSet("Link tag with mailto protocol", "<a href=\"mailto:someone@sikt.no\">Mailto</a>", "Mailto"),
                argumentSet("Equals", "=", "&#61;"),
                argumentSet("Plus", "+", "&#43;"),
                argumentSet("At", "@", "&#64;"),
                argumentSet("Unordered list tag", "<ul><li>List item</li></ul>", "List item"),
                argumentSet("Ordered List tag", "<ol><li>List item</li></ol>", "List item"),
                argumentSet("MathML", "<math><mfrac><mrow><mn>1</mn><mo>x</mo><mn>3</mn></mrow></mfrac></math>", "1x3")
        );
    }
}