package no.unit.nva.publication.sanitizer;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class HtmlSanitizer {

    private static final PolicyFactory POLICY_FACTORY = new HtmlPolicyBuilder()
            .allowElements("a", "i", "p", "br", "b", "strong", "em")
            .allowAttributes("href", "target").onElements("a")
            .allowUrlProtocols("https", "http")
            .requireRelNofollowOnLinks() // Adds rel=nofollow to all links
            .toFactory();

    private HtmlSanitizer() {
    }

    public static String sanitize(String input) {
        return POLICY_FACTORY.sanitize(input);
    }
}
