package no.unit.nva.publication.storage.model;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.storage.model.DoiRequestUtils.extractDataFromResource;
import static no.unit.nva.publication.storage.model.DoiRequestUtils.extractDoiRequestCreatedDate;
import static no.unit.nva.publication.storage.model.DoiRequestUtils.extractDoiRequestModifiedDate;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.storage.model.exceptions.IllegalDoiRequestUpdate;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@Data
@lombok.Builder(
    builderClassName = "DoiRequestBuilder",
    builderMethodName = "builder",
    toBuilder = true,
    setterPrefix = "with")
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class DoiRequest
    implements WithIdentifier,
               RowLevelSecurity,
               WithStatus,
               ResourceUpdate,
               ConnectedToResource {
    
    public static final String RESOURCE_STATUS_FIELD = "resourceStatus";
    public static final String STATUS_FIELD = "status";
    public static final String MODIFIED_DATE_FIELD = "modifiedDate";
    public static final String TYPE = DoiRequest.class.getSimpleName();
    
    public static final String MISSING_RESOURCE_REFERENCE_ERROR = "Resource identifier cannot be null or empty";
    
    public static final SortableIdentifier NOT_IMPORTANT = null;
    public static final String RESOURCE_IDENTIFIER_MISMATCH_ERROR = "Resource identifier mismatch";
    
    @JsonProperty
    private SortableIdentifier identifier;
    @JsonProperty
    private SortableIdentifier resourceIdentifier;
    @JsonProperty(STATUS_FIELD)
    private DoiRequestStatus status;
    @JsonProperty(RESOURCE_STATUS_FIELD)
    private PublicationStatus resourceStatus;
    @JsonProperty
    private Instant modifiedDate;
    @JsonProperty
    @JsonAlias("date")
    private Instant createdDate;
    @JsonProperty("customerId")
    private URI customerId;
    @JsonProperty("owner")
    private String owner;
    @JsonProperty("resourceTitle")
    private String resourceTitle;
    @JsonProperty
    private Instant resourceModifiedDate;
    @JsonProperty
    private PublicationInstance<? extends Pages> resourcePublicationInstance;
    @JsonProperty
    private PublicationDate resourcePublicationDate;
    @JsonProperty
    private String resourcePublicationYear;
    @JsonProperty
    private URI doi;
    @JsonProperty
    private List<Contributor> contributors;
    
    public DoiRequest() {
    
    }
    
    public static DoiRequest newDoiRequestForResource(Resource resource) {
        return newDoiRequestForResource(SortableIdentifier.next(), resource, Clock.systemDefaultZone().instant());
    }
    
    public static DoiRequest newDoiRequestForResource(Resource resource, Instant now) {
        return newDoiRequestForResource(SortableIdentifier.next(), resource, now);
    }
    
    public static DoiRequest newDoiRequestForResource(SortableIdentifier doiRequestIdentifier,
                                                      Resource resource,
                                                      Instant now) {
        
        DoiRequest doiRequest =
            extractDataFromResource(builder(), resource)
                .withIdentifier(doiRequestIdentifier)
                .withStatus(DoiRequestStatus.REQUESTED)
                .withModifiedDate(now)
                .withCreatedDate(now)
                .withDoi(resource.getDoi())
                .build();

        doiRequest.validate();
        return doiRequest;
    }

    public static String getType() {
        return DoiRequest.TYPE;
    }

    /**
     * Creates a DoiRequest from a {@link Publication} object which is currently the de-facto DTO.
     *
     * @param publication          a {@link Publication} objcet
     * @param doiRequestIdentifier the identifier that the new DoiRequest will have
     * @return a new DoiRequest
     */
    public static DoiRequest fromPublication(Publication publication, SortableIdentifier doiRequestIdentifier) {
        return fromDto(publication, doiRequestIdentifier);
    }

    public static DoiRequest fromDto(Publication publication, SortableIdentifier doiRequestIdentifier) {
        Resource resource = Resource.fromPublication(publication);

        return extractDataFromResource(DoiRequest.builder(), resource)
                   .withModifiedDate(extractDoiRequestModifiedDate(publication.getDoiRequest()))
                   .withCreatedDate(extractDoiRequestCreatedDate(publication.getDoiRequest()))
                   .withIdentifier(doiRequestIdentifier)
                   .withStatus(extractDoiRequestStatus(publication.getDoiRequest()))
                   .build();
    }
    
    public DoiRequest update(Resource resource) {
        if (updateIsAboutTheSameResource(resource)) {
            return extractDataFromResource(this.copy(), resource)
                       .build();
        }
        throw new IllegalDoiRequestUpdate(RESOURCE_IDENTIFIER_MISMATCH_ERROR);
    }
    
    @Override
    public String getStatusString() {
        return Objects.nonNull(getStatus()) ? getStatus().toString() : null;
    }
    
    public DoiRequestBuilder copy() {
        return this.toBuilder();
    }
    
    public void validate() {
        if (isNull(resourceIdentifier)) {
            throw new IllegalArgumentException(MISSING_RESOURCE_REFERENCE_ERROR);
        }
    }
    
    @Override
    public Publication toPublication() {
        
        no.unit.nva.model.DoiRequest doiRequest = new no.unit.nva.model.DoiRequest.Builder()
                                                      .withStatus(getStatus())
                                                      .withModifiedDate(getModifiedDate())
                                                      .withCreatedDate(getCreatedDate())
                                                      .build();
        
        Reference reference = new Reference.Builder()
                                  .withPublicationInstance(getResourcePublicationInstance())
                                  .build();
        
        EntityDescription entityDescription = new EntityDescription.Builder()
                                                  .withMainTitle(getResourceTitle())
                                                  .withDate(getResourcePublicationDate())
                                                  .withReference(reference)
                                                  .withContributors(getContributors())
                                                  .build();
        
        Organization customer = new Organization.Builder()
                                    .withId(getCustomerId())
                                    .build();
        
        return new
                   Publication.Builder()
                   .withIdentifier(getResourceIdentifier())
                   .withModifiedDate(getResourceModifiedDate())
                   .withDoi(getDoi())
                   .withStatus(getResourceStatus())
                   .withEntityDescription(entityDescription)
                   .withPublisher(customer)
                   .withOwner(getOwner())
                   .withDoiRequest(doiRequest)
                   .build();
    }
    
    private static DoiRequestStatus extractDoiRequestStatus(no.unit.nva.model.DoiRequest doiRequest) {
        return Optional.ofNullable(doiRequest).map(no.unit.nva.model.DoiRequest::getStatus).orElse(null);
    }
    
    private boolean updateIsAboutTheSameResource(Resource resource) {
        return resource.getIdentifier().equals(this.getResourceIdentifier());
    }
}
