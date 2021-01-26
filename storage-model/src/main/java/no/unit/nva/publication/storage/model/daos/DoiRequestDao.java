package no.unit.nva.publication.storage.model.daos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.DoiRequest;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;

public class DoiRequestDao extends Dao<DoiRequest>
    implements
    JoinWithResource,
    JsonSerializable {

    private DoiRequest data;

    @JacocoGenerated
    public DoiRequestDao() {
        super();
    }

    public DoiRequestDao(DoiRequest doiRequest) {
        super();
        this.data = doiRequest;
    }

    public static DoiRequestDao queryObject(URI publisherId, String owner, SortableIdentifier doiRequestIdentifier) {
        DoiRequest doi = new DoiRequest(
            doiRequestIdentifier,
            doiRequestIdentifier,
            owner,
            publisherId,
            null,
            null,
            null);
        return new DoiRequestDao(doi);
    }

    @Override
    public DoiRequest getData() {
        return data;
    }

    @Override
    public void setData(DoiRequest data) {
        this.data = data;
    }

    @JsonIgnore
    public static String getContainedType() {
        return DoiRequest.TYPE;
    }

    @Override
    public String getType() {
        return DoiRequest.TYPE;
    }

    @Override
    public URI getCustomerId() {
        return data.getCustomerId();
    }

    @Override
    protected String getOwner() {
        return data.getOwner();
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return data.getIdentifier();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getData());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoiRequestDao)) {
            return false;
        }
        DoiRequestDao that = (DoiRequestDao) o;
        return Objects.equals(getData(), that.getData());
    }

    @Override
    public String toString() {
        return toJsonString();
    }

    @Override
    @JsonIgnore
    public SortableIdentifier getResourceIdentifier() {
        return data.getResourceIdentifier();
    }
}
