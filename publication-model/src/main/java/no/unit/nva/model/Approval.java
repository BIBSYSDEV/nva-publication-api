package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Approval {

    @JsonAlias("date")
    private Instant approvalDate;
    private ApprovalsBody approvedBy;
    private ApprovalStatus approvalStatus;
    private String applicationCode;

    public Approval() {

    }

    private Approval(Builder builder) {
        setApprovalDate(builder.approvalDate);
        setApprovedBy(builder.approvedBy);
        setApprovalStatus(builder.approvalStatus);
        setApplicationCode(builder.applicationCode);
    }

    public Instant getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(Instant approvalDate) {
        this.approvalDate = approvalDate;
    }

    public ApprovalsBody getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(ApprovalsBody approvedBy) {
        this.approvedBy = approvedBy;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Approval)) {
            return false;
        }
        Approval approval = (Approval) o;
        return Objects.equals(getApprovalDate(), approval.getApprovalDate())
                && Objects.equals(getApprovedBy(), approval.getApprovedBy())
                && Objects.equals(getApprovalStatus(), approval.getApprovalStatus())
                && Objects.equals(getApplicationCode(), approval.getApplicationCode());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getApprovalDate(), getApprovedBy(), getApprovalStatus(), getApplicationCode());
    }

    public static final class Builder {
        private Instant approvalDate;
        private ApprovalsBody approvedBy;
        private ApprovalStatus approvalStatus;
        private String applicationCode;

        public Builder() {
        }

        public Builder withApprovalDate(Instant approvalDate) {
            this.approvalDate = approvalDate;
            return this;
        }

        public Builder withApprovedBy(ApprovalsBody approvedBy) {
            this.approvedBy = approvedBy;
            return this;
        }

        public Builder withApprovalStatus(ApprovalStatus approvalStatus) {
            this.approvalStatus = approvalStatus;
            return this;
        }

        public Builder withApplicationCode(String applicationCode) {
            this.applicationCode = applicationCode;
            return this;
        }

        public Approval build() {
            return new Approval(this);
        }
    }
}
