package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import nva.commons.core.JacocoGenerated;

public class Owner {
    
    public static final String OWNER_AFFILIATION = "ownerAffiliation";
    public static final String OWNER_FIELD = "owner";
    
    @JsonProperty(OWNER_FIELD)
    private final User user;
    @JsonProperty(OWNER_AFFILIATION)
    private final URI ownerAffiliation;
    
    @JsonCreator
    public Owner(@JsonProperty(OWNER_FIELD) User user, @JsonProperty(OWNER_AFFILIATION) URI affiliation) {
        this.user = user;
        this.ownerAffiliation = affiliation;
    }
    
    public Owner(String user, URI affiliation) {
        this.user = new User(user);
        this.ownerAffiliation = affiliation;
    }
    
    public static Owner fromResourceOwner(ResourceOwner resourceOwner) {
        return Optional.ofNullable(resourceOwner)
                   .map(owner -> new Owner(new User(owner.getOwner().toString()), owner.getOwnerAffiliation()))
                   .orElse(null);
    }
    
    public ResourceOwner toResourceOwner() {
        return new ResourceOwner(new Username(user.toString()), ownerAffiliation);
    }
    
    public User getUser() {
        return user;
    }
    
    public URI getOwnerAffiliation() {
        return ownerAffiliation;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getUser(), getOwnerAffiliation());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Owner)) {
            return false;
        }
        Owner owner = (Owner) o;
        return Objects.equals(getUser(), owner.getUser()) && Objects.equals(getOwnerAffiliation(),
            owner.getOwnerAffiliation());
    }
}
