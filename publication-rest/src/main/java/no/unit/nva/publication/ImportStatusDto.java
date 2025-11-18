package no.unit.nva.publication;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.net.URI;
import no.unit.nva.importcandidate.CandidateStatus;
import no.unit.nva.importcandidate.ImportStatus;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record ImportStatusDto(CandidateStatus candidateStatus, URI nvaPublicationId, String comment) {

    public ImportStatus toImportStatus() {
        return ImportStatus.builder()
                   .withCandidateStatus(candidateStatus)
                   .withNvaPublicationId(nvaPublicationId)
                   .withComment(comment)
                   .build();
    }
}
