package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(ExpandedPerson.TYPE)
public class ExpandedPerson {

    public static final String TYPE = "Person";
    public static final String PREFERRED_FIRST_NAME_FIELD = "preferredFirstName";
    public static final String FIRST_NAME_FIELD = "firstName";
    public static final String PREFERRED_LAST_NAME_FIELD = "preferredLastName";
    public static final String LAST_NAME_FIELD = "lastName";
    public static final String USERNAME_FIELD = "username";
    @JsonProperty(LAST_NAME_FIELD)
    private final String lastName;
    @JsonProperty(PREFERRED_LAST_NAME_FIELD)
    private final String preferredLastName;
    @JsonProperty(FIRST_NAME_FIELD)
    private final String firstName;
    @JsonProperty(PREFERRED_FIRST_NAME_FIELD)
    private final String preferredFirstName;
    @JsonProperty(USERNAME_FIELD)
    private final User username;

    @JsonCreator
    public ExpandedPerson(@JsonProperty(FIRST_NAME_FIELD) String firstName,
                          @JsonProperty(PREFERRED_FIRST_NAME_FIELD) String preferredFirstName,
                          @JsonProperty(LAST_NAME_FIELD) String lastName,
                          @JsonProperty(PREFERRED_LAST_NAME_FIELD) String preferredLastName,
                          @JsonProperty(USERNAME_FIELD) User username) {
        this.lastName = lastName;
        this.preferredLastName = preferredLastName;
        this.firstName = firstName;
        this.preferredFirstName = preferredFirstName;
        this.username = username;
    }

    public static ExpandedPerson defaultExpandedPerson(User username) {
        return new ExpandedPerson(null, null, null, null, username);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExpandedPerson that = (ExpandedPerson) o;
        return Objects.equals(getLastName(), that.getLastName())
                && Objects.equals(getPreferredLastName(), that.getPreferredLastName())
                && Objects.equals(getFirstName(), that.getFirstName())
                && Objects.equals(getPreferredFirstName(), that.getPreferredFirstName())
                && Objects.equals(getUsername(), that.getUsername());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getLastName(), getPreferredLastName(), getFirstName(), getPreferredFirstName(), getUsername());
    }

    public String getLastName() {
        return lastName;
    }

    public String getPreferredLastName() {
        return preferredLastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getPreferredFirstName() {
        return preferredFirstName;
    }

    public User getUsername() {
        return username;
    }

    public static final class Builder {

        private String lastName;
        private String preferredLastName;
        private String firstName;
        private String preferredFirstName;
        private User username;

        public Builder() {
        }

        public ExpandedPerson.Builder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public ExpandedPerson.Builder withPreferredFirstName(String preferredFirstName) {
            this.preferredFirstName = preferredFirstName;
            return this;
        }

        public ExpandedPerson.Builder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public ExpandedPerson.Builder withPreferredLastName(String preferredLastName) {
            this.preferredLastName = preferredLastName;
            return this;
        }

        public ExpandedPerson.Builder withUser(User username) {
            this.username = username;
            return this;
        }

        public ExpandedPerson build() {
            return new ExpandedPerson(firstName, preferredFirstName, lastName, preferredLastName, username);
        }
    }
}
