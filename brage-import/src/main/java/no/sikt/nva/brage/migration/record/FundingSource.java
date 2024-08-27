package no.sikt.nva.brage.migration.record;

import java.util.Map;

public record FundingSource(String identifier, Map<String, String> name) {

}
