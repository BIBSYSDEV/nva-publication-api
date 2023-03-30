package no.sikt.nva.scopus.conversion;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.util.Set;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.PersonalnameType;
import no.sikt.nva.scopus.conversion.model.cristin.Person;
import no.sikt.nva.scopus.conversion.model.cristin.TypedValue;
import nva.commons.core.StringUtils;
import org.junit.jupiter.api.Test;

public class CristinContributorExtractorTest {

    public static final String FIRST_NAME_CRISTIN_FIELD_NAME = "FirstName";
    public static final String LAST_NAME_CRISTIN_FIELD_NAME = "LastName";
    public static final TypedValue FIRST_NAME = new TypedValue(FIRST_NAME_CRISTIN_FIELD_NAME, "First");
    public static final TypedValue LAST_NAME = new TypedValue(LAST_NAME_CRISTIN_FIELD_NAME, "Last");

    @Test
    void shouldCreateIdentityWithFullName() {
        var person = new Person(null, null, Set.of(FIRST_NAME, LAST_NAME), null, null, null);
        var contributor = CristinContributorExtractor.generateContributorFromCristin(person, authorTp(), null, null);
        var expectedName = "Last, First";
        assertThat(contributor.getIdentity().getName(), is(equalTo(expectedName)));
    }

    @Test
    void shouldCreateEmptyNameWhenTypedValueIsNull() {
        var person = new Person(null, null, null, null, null, null);
        var contributor = CristinContributorExtractor.generateContributorFromCristin(person, authorTp(), null, null);
        assertThat(contributor.getIdentity().getName(), is(equalTo(StringUtils.EMPTY_STRING)));
    }

    @Test
    void shouldCreateNameWithFirstNameOnlyWhenLastNameValueIsEmpty() {
        var person = new Person(null, null, Set.of(FIRST_NAME), null, null, null);
        var contributor = CristinContributorExtractor.generateContributorFromCristin(person, authorTp(), null, null);
        assertThat(contributor.getIdentity().getName(), is(equalTo(FIRST_NAME.getValue())));
    }

    @Test
    void shouldReturnContributorWhichIsCorrespondingAuthorWhenIndexedNameEqualsCorrespondencePersonIndexedName() {
        var person = new Person(null, null, Set.of(FIRST_NAME, LAST_NAME), null, null, null);
        var contributor = CristinContributorExtractor.generateContributorFromCristin(person, authorTp(), correspondencePerson(), null);
        assertThat(contributor.isCorrespondingAuthor(), is(true));
    }

    private static PersonalnameType correspondencePerson() {
        var correspondencePerson = new PersonalnameType();
        correspondencePerson.setIndexedName("First, Last");
        return correspondencePerson;
    }

    private static AuthorTp authorTp() {
        var author = new AuthorTp();
        author.setSeq(String.valueOf(randomInteger()));
        author.setAuid(randomString());
        author.setIndexedName("First, Last");
        return author;
    }
}
