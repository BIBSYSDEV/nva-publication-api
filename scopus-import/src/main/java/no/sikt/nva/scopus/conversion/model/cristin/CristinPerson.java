package no.sikt.nva.scopus.conversion.model.cristin;

//Copied from nva-cristin-service, is it possible to import this instead of copying?

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static no.sikt.nva.scopus.conversion.model.cristin.JsonPropertyNames.AFFILIATIONS;
import static no.sikt.nva.scopus.conversion.model.cristin.JsonPropertyNames.CONTACT_DETAILS;
import static no.sikt.nva.scopus.conversion.model.cristin.JsonPropertyNames.ID;
import static no.sikt.nva.scopus.conversion.model.cristin.JsonPropertyNames.IDENTIFIERS;
import static no.sikt.nva.scopus.conversion.model.cristin.JsonPropertyNames.IMAGE;
import static no.sikt.nva.scopus.conversion.model.cristin.JsonPropertyNames.NAMES;
import static no.sikt.nva.scopus.conversion.model.cristin.JsonPropertyNames.NATIONAL_IDENTITY_NUMBER;
import static no.sikt.nva.scopus.conversion.model.cristin.JsonPropertyNames.RESERVED;
import static no.sikt.nva.scopus.conversion.model.cristin.JsonPropertyNames.TYPE;
import static no.unit.nva.model.util.ContextUtil.CONTEXT;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@JsonPropertyOrder({CONTEXT, ID, TYPE, IDENTIFIERS, NAMES, CONTACT_DETAILS, IMAGE, AFFILIATIONS})
public class CristinPerson implements JsonSerializable {

    public static final String VERIFIED = "verified";
    @JsonProperty(TYPE)
    private static final String type = "Person";
    @JsonProperty(ID)
    private URI id;
    @JsonInclude(NON_NULL)
    @JsonProperty(CONTEXT)
    private String context;
    @JsonProperty(IDENTIFIERS)
    private Set<TypedValue> identifiers;
    @JsonProperty(NAMES)
    private Set<TypedValue> names;
    @JsonProperty(CONTACT_DETAILS)
    private ContactDetails contactDetails;
    @JsonProperty(IMAGE)
    private URI image;
    @JsonProperty(AFFILIATIONS)
    private Set<Affiliation> affiliations;
    @JsonProperty(NATIONAL_IDENTITY_NUMBER)
    private String norwegianNationalId;
    @JsonProperty(RESERVED)
    private Boolean reserved;
    @JsonProperty(VERIFIED)
    private Boolean isVerified;

    private CristinPerson() {

    }

    /**
     * Creates a Person for serialization to client.
     *
     * @param id             Identifier of Person.
     * @param identifiers    Different identifiers related to this object.
     * @param names          Different names for this Person.
     * @param contactDetails How to contact this Person.
     * @param image          URI to picture of this Person.
     * @param affiliations   This person's organization affiliations.
     */
    @JsonCreator
    public CristinPerson(@JsonProperty("id") URI id, @JsonProperty("identifiers") Set<TypedValue> identifiers,
                         @JsonProperty("names") Set<TypedValue> names,
                         @JsonProperty("contactDetails") ContactDetails contactDetails, @JsonProperty("image") URI image,
                         @JsonProperty("affiliations") Set<Affiliation> affiliations) {
        this.id = id;
        this.identifiers = identifiers;
        this.names = names;
        this.contactDetails = contactDetails;
        this.image = image;
        this.affiliations = affiliations;
    }

    public Boolean getVerified() {
        return isVerified;
    }

    public void setVerified(Boolean verified) {
        isVerified = verified;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Set<TypedValue> getIdentifiers() {
        return Objects.nonNull(identifiers) ? identifiers : Collections.emptySet();
    }

    public void setIdentifiers(Set<TypedValue> identifiers) {
        this.identifiers = identifiers;
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public Set<Affiliation> getAffiliations() {
        return Objects.nonNull(affiliations) ? affiliations : Collections.emptySet();
    }

    public void setAffiliations(Set<Affiliation> affiliations) {
        this.affiliations = affiliations;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(ContactDetails contactDetails) {
        this.contactDetails = contactDetails;
    }

    public URI getImage() {
        return image;
    }

    public void setImage(URI image) {
        this.image = image;
    }

    public Set<TypedValue> getNames() {
        return Objects.nonNull(names) ? names : Collections.emptySet();
    }

    public void setNames(Set<TypedValue> names) {
        this.names = names;
    }

    public String getNorwegianNationalId() {
        return norwegianNationalId;
    }

    public void setNorwegianNationalId(String norwegianNationalId) {
        this.norwegianNationalId = norwegianNationalId;
    }

    public Boolean getReserved() {
        return reserved;
    }

    public void setReserved(Boolean reserved) {
        this.reserved = reserved;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getContext(), getId(), getIdentifiers(), getNames(), getContactDetails(), getImage(),
                            getAffiliations());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CristinPerson)) {
            return false;
        }
        CristinPerson that = (CristinPerson) o;
        return Objects.equals(getContext(), that.getContext())
               && Objects.equals(getId(), that.getId())
               && getIdentifiers().equals(that.getIdentifiers())
               && getNames().equals(that.getNames())
               && Objects.equals(getContactDetails(), that.getContactDetails())
               && Objects.equals(getImage(), that.getImage())
               && getAffiliations().equals(that.getAffiliations());
    }

    @Override
    public String toString() {
        return toJsonString();
    }

    @JacocoGenerated
    public static final class Builder {

        private final transient CristinPerson cristinPerson;

        public Builder() {
            cristinPerson = new CristinPerson();
        }

        public Builder withContext(String context) {
            cristinPerson.setContext(context);
            return this;
        }

        public Builder withId(URI id) {
            cristinPerson.setId(id);
            return this;
        }

        public Builder withIdentifiers(Set<TypedValue> identifiers) {
            cristinPerson.setIdentifiers(identifiers);
            return this;
        }

        public Builder withNames(Set<TypedValue> names) {
            cristinPerson.setNames(names);
            return this;
        }

        public Builder withContactDetails(ContactDetails contactDetails) {
            cristinPerson.setContactDetails(contactDetails);
            return this;
        }

        public Builder withImage(URI image) {
            cristinPerson.setImage(image);
            return this;
        }

        public Builder withAffiliations(Set<Affiliation> affiliations) {
            cristinPerson.setAffiliations(affiliations);
            return this;
        }

        public Builder withNorwegianNationalId(String norwegianNationalId) {
            cristinPerson.setNorwegianNationalId(norwegianNationalId);
            return this;
        }

        public Builder withReserved(Boolean reserved) {
            cristinPerson.setReserved(reserved);
            return this;
        }

        public Builder withVerifiedStatus(Boolean verifiedStatus) {
            cristinPerson.setVerified(verifiedStatus);
            return this;
        }

        public CristinPerson build() {
            return cristinPerson;
        }
    }
}