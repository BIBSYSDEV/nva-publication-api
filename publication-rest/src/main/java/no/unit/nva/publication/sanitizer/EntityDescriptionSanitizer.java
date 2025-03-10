package no.unit.nva.publication.sanitizer;
import no.unit.nva.model.EntityDescription;
import java.util.HashMap;
import static java.util.Objects.nonNull;

public final class EntityDescriptionSanitizer {

    private EntityDescriptionSanitizer() {}

    public static EntityDescription sanitize(EntityDescription entityDescription) {
        sanitizeMainTitle(entityDescription);
        sanitizeAlternativeTitles(entityDescription);
        sanitizeAbstract(entityDescription);
        sanitizeAlternativeAbstracts(entityDescription);

        return entityDescription;
    }

    private static void sanitizeAlternativeAbstracts(EntityDescription entityDescription) {
        if (nonNull(entityDescription.getAlternativeAbstracts())) {
            var myMap = new HashMap<String, String>();
            entityDescription.getAlternativeAbstracts().forEach((key, value) -> {
                myMap.put(key, HtmlSanitizer.sanitize(value));
            });
            entityDescription.setAlternativeAbstracts(myMap);
        }
    }

    private static void sanitizeAbstract(EntityDescription entityDescription) {
        if (nonNull(entityDescription.getAbstract())) {
            entityDescription.setAbstract(HtmlSanitizer.sanitize(entityDescription.getAbstract()));
        }
    }

    private static void sanitizeAlternativeTitles(EntityDescription entityDescription) {
        if (nonNull(entityDescription.getAlternativeTitles())) {
            var myMap = new HashMap<String, String>();
            entityDescription.getAlternativeTitles().forEach((key, value) -> {
                myMap.put(key, HtmlSanitizer.sanitize(value));
            });
            entityDescription.setAlternativeTitles(myMap);
        }
    }

    private static void sanitizeMainTitle(EntityDescription entityDescription) {
        if (nonNull(entityDescription.getMainTitle())) {
            entityDescription.setMainTitle(HtmlSanitizer.sanitize(entityDescription.getMainTitle()));
        }
    }
}
