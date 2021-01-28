package no.unit.nva.publication.storage.model.daos;

import java.net.URI;
import java.util.Objects;
import no.unit.nva.publication.storage.model.DoiRequest;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;

public class DoiRequestDao extends Dao<DoiRequest> implements JsonSerializable {

    private DoiRequest data;

    @JacocoGenerated
    public DoiRequestDao() {
        super();
    }

    public DoiRequestDao(DoiRequest doiRequest) {
        super();
        this.data = doiRequest;
    }

    @Override
    public DoiRequest getData() {
        return data;
    }

    @Override
    public void setData(DoiRequest data) {
        this.data = data;
    }

    @Override
    protected String getType() {
        return DoiRequest.TYPE;
    }

    @Override
    protected URI getCustomerId() {
        return data.getCustomerId();
    }

    @Override
    protected String getOwner() {
        return data.getOwner();
    }

    @Override
    protected String getIdentifier() {
        return data.getIdentifier().toString();
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
}
