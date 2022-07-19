package no.unit.nva.publication.external.services;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;

/**
 * A simple model of a Bare search result entry, to avoid using resource files.
 */
public class PersonApiEntry {
    
    @JsonProperty("feideids")
    private List<String> feideIds;
    @JsonProperty("orcids")
    private List<URI> orcidIds;
    @JsonProperty("orgunitids")
    private List<URI> orgunitids;
    @JsonProperty("handles")
    private List<URI> handles;
    @JsonProperty("id")
    private URI id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("birthDate")
    private String birthDate;
    
    public static PersonApiEntry create(String feideId, URI... orgunitIds) {
        var entry = new PersonApiEntry();
        entry.setFeideIds(List.of(feideId));
        entry.setOrgunitids(List.of(orgunitIds));
        addRandomData(entry);
        return entry;
    }
    
    public List<String> getFeideIds() {
        return feideIds;
    }
    
    public void setFeideIds(List<String> feideIds) {
        this.feideIds = feideIds;
    }
    
    public List<URI> getOrcidIds() {
        return orcidIds;
    }
    
    public void setOrcidIds(List<URI> orcidIds) {
        this.orcidIds = orcidIds;
    }
    
    public List<URI> getOrgunitids() {
        return orgunitids;
    }
    
    public void setOrgunitids(List<URI> orgunitids) {
        this.orgunitids = orgunitids;
    }
    
    public List<URI> getHandles() {
        return handles;
    }
    
    public void setHandles(List<URI> handles) {
        this.handles = handles;
    }
    
    public URI getId() {
        return id;
    }
    
    public void setId(URI id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getBirthDate() {
        return birthDate;
    }
    
    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }
    
    private static void addRandomData(PersonApiEntry entry) {
        entry.setBirthDate(randomString());
        entry.setHandles(List.of(randomUri()));
        entry.setName(randomString());
        entry.setOrcidIds(List.of(randomUri()));
        entry.setId(randomUri());
    }
}
