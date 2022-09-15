package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class User {
    
    private final String userName;
    
    @JsonCreator
    public User(String userName) {
        this.userName = userName;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(userName);
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(userName, user.userName);
    }
    
    @JsonValue
    @Override
    public String toString() {
        return userName;
    }
}
