package no.sikt.nva.scopus.conversion.model.cristin;

//Copied from nva-cristin-service

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class ContactDetails {

    private final String telephone;

    @JsonCreator
    public ContactDetails(@JsonProperty("telephone") String telephone) {
        this.telephone = telephone;
    }

    public Optional<String> getTelephone() {
        return Optional.ofNullable(telephone);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContactDetails)) {
            return false;
        }
        ContactDetails that = (ContactDetails) o;
        return Objects.equals(getTelephone(), that.getTelephone());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTelephone());
    }

}