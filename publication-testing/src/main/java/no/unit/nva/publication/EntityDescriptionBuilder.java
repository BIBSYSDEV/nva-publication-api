package no.unit.nva.publication;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.PublicationContextBuilder.randomPublicationContext;
import static no.unit.nva.publication.PublicationGenerator.randomUri;
import static no.unit.nva.publication.PublicationInstanceBuilder.randomPublicationInstance;
import static no.unit.nva.publication.RandomUtils.randomLabels;
import static no.unit.nva.publication.RandomUtils.randomPublicationDate;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.util.List;
import java.util.Map;
import no.unit.nva.language.Language;
import no.unit.nva.language.LanguageConstants;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.NameType;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Reference;
import no.unit.nva.model.Role;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class EntityDescriptionBuilder {

    public static EntityDescription randomEntityDescription(Class<?> publicationInstanceClass) {
        return new EntityDescription.Builder()
            .withReference(randomReference(publicationInstanceClass))
            .withNpiSubjectHeading(randomNpiSubjectHeading())
            .withDescription(randomString())
            .withMainTitle(randomString())
            .withLanguage(randomLanguage().getLexvoUri())
            .withTags(randomTags())
            .withMetadataSource(randomUri())
            .withDate(randomPublicationDate())
            .withContributors(randomContributors())
            .withAlternativeTitles(randomAlternativeTitles())
            .withAbstract(randomString())
            .build();
    }

    private static Map<String, String> randomAlternativeTitles() {
        Language randomLanguageWithIso6391Code = randomLanguage();
        while (isNull(randomLanguageWithIso6391Code.getIso6391Code())) {
            randomLanguageWithIso6391Code = randomLanguage();
        }
        return Map.of(randomLanguageWithIso6391Code.getIso6391Code(), randomString());
    }

    private static List<Contributor> randomContributors() {
        return List.of(randomContributor(), randomContributor());
    }

    private static Contributor randomContributor() {
        return new Contributor.Builder()
            .withAffiliations(randomOrganizations())
            .withSequence(randomInteger(10))
            .withRole(randomRole())
            .withIdentity(randomIdentity())
            .build();
    }

    private static Identity randomIdentity() {
        return new Identity.Builder()
            .withId(randomUri())
            .withArpId(randomString())
            .withName(randomString())
            .withOrcId(randomString())
            .withNameType(randomNameType())
            .build();
    }

    private static NameType randomNameType() {
        return randomElement(NameType.values());
    }

    private static Role randomRole() {
        return randomElement(Role.values());
    }

    private static List<Organization> randomOrganizations() {
        return List.of(randomOrganization());
    }

    private static Organization randomOrganization() {
        return new Organization.Builder()
            .withLabels(randomLabels())
            .withId(randomUri())
            .build();
    }

    private static List<String> randomTags() {
        return List.of(randomString());
    }

    private static Language randomLanguage() {
        return randomElement(LanguageConstants.ALL_LANGUAGES);
    }

    private static String randomNpiSubjectHeading() {
        return randomString();
    }

    private static Reference randomReference(Class<?> publicationInstanceClass) {
        return new Reference.Builder()
            .withPublicationInstance(randomPublicationInstance(publicationInstanceClass))
            .withPublishingContext(randomPublicationContext(publicationInstanceClass))
            .withDoi(randomUri())
            .build();
    }
}
