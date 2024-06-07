package no.sikt.nva.brage.migration.model;

import no.sikt.nva.brage.migration.lambda.MergeSource;
import no.unit.nva.model.Publication;

public record PublicationForUpdate(MergeSource source, Publication existingPublication) {

}
