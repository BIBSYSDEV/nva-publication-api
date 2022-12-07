package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import no.sikt.nva.brage.migration.record.content.ResourceContent;
import nva.commons.core.JacocoGenerated;

@JsonPropertyOrder({"customer", "resourceOwner", "brageLocation", "id", "cristinId", "doi", "publishedDate",
    "publisherAuthority",
    "rightsholder",
    "type", "partOf", "hasPart", "publisherAuthority", "spatialCoverage", "date", "language", "publication",
    "entityDescription",
    "recordContent", "errors", "warnings"})
@SuppressWarnings("PMD.TooManyFields")
public class Record {

    private ResourceOwner resourceOwner;
    private EntityDescription entityDescription;
    private Customer customer;
    private URI id;
    private URI doi;
    private Type type;
    private Language language;
    private PublisherAuthority publisherAuthority;
    private String rightsholder;
    private List<String> spatialCoverage;
    private String partOf;
    private String part;
    private Publication publication;
    private ResourceContent contentBundle;
    private PublishedDate publishedDate;
    private String cristinId;
    private String brageLocation;
    private List<ErrorDetails> errors;
    private List<WarningDetails> warnings;

    public Record() {
    }

    @JacocoGenerated
    @JsonProperty("resourceOwner")
    public ResourceOwner getResourceOwner() {
        return resourceOwner;
    }

    @JacocoGenerated
    public void setResourceOwner(ResourceOwner resourceOwner) {
        this.resourceOwner = resourceOwner;
    }

    @JacocoGenerated
    @JsonProperty("hasPart")
    public String getPart() {
        return part;
    }

    @JacocoGenerated
    public void setPart(String part) {
        this.part = part;
    }

    @JacocoGenerated
    @JsonProperty("partOf")
    public String getPartOf() {
        return partOf;
    }

    @JacocoGenerated
    public void setPartOf(String partOf) {
        this.partOf = partOf;
    }

    @JacocoGenerated
    @JsonProperty("brageLocation")
    public String getBrageLocation() {
        return brageLocation;
    }

    @JacocoGenerated
    public void setBrageLocation(String brageLocation) {
        this.brageLocation = brageLocation;
    }

    @JacocoGenerated
    @JsonProperty("warnings")
    public List<WarningDetails> getWarnings() {
        return warnings;
    }

    @JacocoGenerated
    public void setWarnings(List<WarningDetails> warnings) {
        this.warnings = warnings;
    }

    @JacocoGenerated
    @JsonProperty("errors")
    public List<ErrorDetails> getErrors() {
        return errors;
    }

    @JacocoGenerated
    public void setErrors(List<ErrorDetails> errors) {
        this.errors = errors;
    }

    @JacocoGenerated
    @JsonProperty("cristinId")
    public String getCristinId() {
        return cristinId;
    }

    @JacocoGenerated
    public void setCristinId(String cristinId) {
        this.cristinId = cristinId;
    }

    @JacocoGenerated
    @JsonProperty("publishedDate")
    public PublishedDate getPublishedDate() {
        return publishedDate;
    }

    @JacocoGenerated
    public void setPublishedDate(PublishedDate publishedDate) {
        this.publishedDate = publishedDate;
    }

    @JacocoGenerated
    @JsonProperty("recordContent")
    public ResourceContent getContentBundle() {
        return contentBundle;
    }

    @JacocoGenerated
    public void setContentBundle(ResourceContent contentBundle) {
        this.contentBundle = contentBundle;
    }

    @JacocoGenerated
    @JsonProperty("spatialCoverage")
    public List<String> getSpatialCoverage() {
        return spatialCoverage;
    }

    @JacocoGenerated
    public void setSpatialCoverage(List<String> spatialCoverage) {
        this.spatialCoverage = spatialCoverage;
    }

    @JsonInclude
    @JsonProperty("publisherAuthority")
    public PublisherAuthority getPublisherAuthority() {
        return publisherAuthority;
    }

    @JacocoGenerated
    public void setPublisherAuthority(PublisherAuthority publisherAuthority) {
        this.publisherAuthority = publisherAuthority;
    }

    @JacocoGenerated
    @JsonProperty("publication")
    public Publication getPublication() {
        return publication;
    }

    @JacocoGenerated
    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    @JacocoGenerated
    @JsonProperty("customer")
    public Customer getCustomer() {
        return this.customer;
    }

    @JacocoGenerated
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    @JacocoGenerated
    @JsonProperty("id")
    public URI getId() {
        return this.id;
    }

    @JacocoGenerated
    public void setId(URI id) {
        this.id = id;
    }

    @JacocoGenerated
    @JsonProperty("type")
    public Type getType() {
        return this.type;
    }

    @JacocoGenerated
    public void setType(Type type) {
        this.type = type;
    }

    @JacocoGenerated
    @JsonProperty("language")
    public Language getLanguage() {
        return this.language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    @JsonProperty("rightsholder")
    public String getRightsholder() {
        return rightsholder;
    }

    @JacocoGenerated
    public void setRightsHolder(String rightsholder) {
        this.rightsholder = rightsholder;
    }

    @JsonProperty("doi")
    public URI getDoi() {
        return doi;
    }

    @JacocoGenerated
    public void setDoi(URI doi) {
        this.doi = doi;
    }

    @JsonProperty("entityDescription")
    public EntityDescription getEntityDescription() {
        return entityDescription;
    }

    public void setEntityDescription(EntityDescription entityDescription) {
        this.entityDescription = entityDescription;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getResourceOwner(), getEntityDescription(), getCustomer(), getId(), getDoi(), getType(),
                            getLanguage(), getPublisherAuthority(), getRightsholder(), getSpatialCoverage(),
                            getPartOf(),
                            getPart(), getPublication(), getContentBundle(), getPublishedDate(), getCristinId(),
                            getBrageLocation(), getErrors(), getWarnings());
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
        Record record = (Record) o;
        return Objects.equals(getResourceOwner(), record.getResourceOwner())
               && Objects.equals(getEntityDescription(), record.getEntityDescription())
               && Objects.equals(getCustomer(), record.getCustomer())
               && Objects.equals(getId(), record.getId())
               && Objects.equals(getDoi(), record.getDoi())
               && Objects.equals(getType(), record.getType())
               && Objects.equals(getLanguage(), record.getLanguage())
               && Objects.equals(getPublisherAuthority(), record.getPublisherAuthority())
               && Objects.equals(getRightsholder(), record.getRightsholder())
               && Objects.equals(getSpatialCoverage(), record.getSpatialCoverage())
               && Objects.equals(getPartOf(), record.getPartOf())
               && Objects.equals(getPart(), record.getPart())
               && Objects.equals(getPublication(), record.getPublication())
               && Objects.equals(getContentBundle(), record.getContentBundle())
               && Objects.equals(getPublishedDate(), record.getPublishedDate())
               && Objects.equals(getCristinId(), record.getCristinId())
               && Objects.equals(getBrageLocation(), record.getBrageLocation())
               && Objects.equals(getErrors(), record.getErrors())
               && Objects.equals(getWarnings(), record.getWarnings());
    }
}
