package no.sikt.nva.brage.migration.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import no.sikt.nva.brage.migration.model.content.ResourceContent;
import no.sikt.nva.brage.migration.model.entitydescription.EntityDescription;
import no.sikt.nva.brage.migration.model.entitydescription.Language;
import no.sikt.nva.brage.migration.model.entitydescription.PublishedDate;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.TooManyFields")
public class Record {

    private EntityDescription entityDescription;
    private URI customerId;
    private URI id;
    private URI doi;
    private Path origin;
    private Type type;
    private Boolean publisherAuthority;
    private String rightsholder;
    private String spatialCoverage;
    private String partOf;
    private Publication publication;
    private ResourceContent contentBundle;
    private PublishedDate publishedDate;
    private String cristinId;
    private String brageLocation;
    private List<ErrorDetails> errors;
    private List<WarningDetails> warnings;

    @JacocoGenerated
    public static <T> boolean listEqualsIgnoreOrder(List<T> list1, List<T> list2) {
        return new HashSet<>(list1).equals(new HashSet<>(list2));
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

    @JacocoGenerated
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
        return Objects.hash(entityDescription, customerId, id, doi, origin, type, publisherAuthority,
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
               && Objects.equals(customerId, record.customerId)
               && Objects.equals(id, record.id)
               && Objects.equals(doi, record.doi)
               && Objects.equals(origin, record.origin)
               && Objects.equals(type, record.type)
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


    @JsonProperty("spatialCoverage")
    public String getSpatialCoverage() {
        return spatialCoverage;
    }

    @JacocoGenerated
    public void setSpatialCoverage(String spatialCoverage) {
        this.spatialCoverage = spatialCoverage;
    }

    @JsonInclude
    @JsonProperty("publisherAuthority")
    public Boolean getPublisherAuthority() {
        return publisherAuthority;
    }

    @JacocoGenerated
    public void setPublisherAuthority(Boolean publisherAuthority) {
        this.publisherAuthority = publisherAuthority;
    }

    @JsonProperty("publication")
    public Publication getPublication() {
        return publication;
    }

    @JacocoGenerated
    public void setPublication(Publication publication) {
        this.publication = publication;
    }


    @JsonProperty("customerId")
    public URI getCustomerId() {
        return this.customerId;
    }


    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
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

    @JacocoGenerated
    public void setType(Type type) {
        this.type = type;
    }

    @JsonProperty("bareOrigin")
    public Path getOrigin() {
        return origin;
    }

    @JacocoGenerated
    public void setOrigin(Path origin) {
        this.origin = origin;
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

    @JacocoGenerated
    public void setEntityDescription(EntityDescription entityDescription) {
        this.entityDescription = entityDescription;
    }

}
