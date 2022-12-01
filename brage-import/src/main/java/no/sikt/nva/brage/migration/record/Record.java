package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import no.sikt.nva.brage.migration.ErrorDetails;
import no.sikt.nva.brage.migration.record.content.ResourceContent;
import nva.commons.core.JacocoGenerated;

@JsonPropertyOrder({"customer", "brageLocation", "id", "cristinId", "doi", "publishedDate", "publisherAuthority",
    "rightsholder",
    "type", "partOf", "publisherAuthority", "spatialCoverage", "date", "language", "publication", "entityDescription",
    "recordContent", "errors", "warnings"})
@SuppressWarnings("PMD.TooManyFields")
public class Record {

    private EntityDescription entityDescription;
    private String customer;
    private URI id;
    private URI doi;
    private Type type;
    private Language language;
    private PublisherAuthority publisherAuthority;
    private String rightsholder;
    private List<String> spatialCoverage;
    private String partOf;
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
    public static <T> boolean listEqualsIgnoreOrder(List<T> list1, List<T> list2) {
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }

    @JsonProperty("partOf")
    public String getPartOf() {
        return partOf;
    }

    @JacocoGenerated
    public void setPartOf(String partOf) {
        this.partOf = partOf;
    }

    @JsonProperty("brageLocation")
    public String getBrageLocation() {
        return brageLocation;
    }

    public void setBrageLocation(String brageLocation) {
        this.brageLocation = brageLocation;
    }

    @JsonProperty("warnings")
    public List<WarningDetails> getWarnings() {
        return warnings;
    }

    @JacocoGenerated
    public void setWarnings(List<WarningDetails> warnings) {
        this.warnings = warnings;
    }

    @JsonProperty("errors")
    public List<ErrorDetails> getErrors() {
        return errors;
    }

    @JacocoGenerated
    public void setErrors(List<ErrorDetails> errors) {
        this.errors = errors;
    }

    @JsonProperty("cristinId")
    public String getCristinId() {
        return cristinId;
    }

    @JacocoGenerated
    public void setCristinId(String cristinId) {
        this.cristinId = cristinId;
    }

    @JsonProperty("publishedDate")
    public PublishedDate getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(PublishedDate publishedDate) {
        this.publishedDate = publishedDate;
    }

    @JsonProperty("recordContent")
    public ResourceContent getContentBundle() {
        return contentBundle;
    }

    public void setContentBundle(ResourceContent contentBundle) {
        this.contentBundle = contentBundle;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(entityDescription, customer, id, doi, type, language, publisherAuthority,
                            rightsholder, spatialCoverage, partOf, publication, contentBundle, publishedDate, cristinId,
                            brageLocation, errors, warnings);
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
        return Objects.equals(entityDescription, record.entityDescription)
               && Objects.equals(customer, record.customer)
               && Objects.equals(id, record.id)
               && Objects.equals(doi, record.doi)
               && Objects.equals(type, record.type)
               && Objects.equals(language, record.language)
               && Objects.equals(publisherAuthority, record.publisherAuthority)
               && Objects.equals(rightsholder, record.rightsholder)
               && Objects.equals(spatialCoverage, record.spatialCoverage)
               && Objects.equals(partOf, record.partOf)
               && Objects.equals(publication, record.publication)
               && Objects.equals(contentBundle, record.contentBundle)
               && Objects.equals(publishedDate, record.publishedDate)
               && Objects.equals(cristinId, record.cristinId)
               && listEqualsIgnoreOrder(errors, record.errors)
               && listEqualsIgnoreOrder(warnings, record.warnings);
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

    @JsonProperty("publication")
    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    @JacocoGenerated
    @JsonProperty("customer")
    public String getCustomer() {
        return this.customer;
    }

    @JacocoGenerated
    public void setCustomer(String customer) {
        this.customer = customer;
    }

    @JsonProperty("id")
    public URI getId() {
        return this.id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    @JsonProperty("type")
    public Type getType() {
        return this.type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @JsonProperty("language")
    public Language getLanguage() {
        return this.language;
    }

    @JacocoGenerated
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
}
