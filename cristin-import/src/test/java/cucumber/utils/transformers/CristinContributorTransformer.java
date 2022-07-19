package cucumber.utils.transformers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinContributor;
import no.unit.nva.cristin.mapper.CristinContributor.CristinContributorBuilder;

public class CristinContributorTransformer {
    
    public static final String FAMILY_NAME = "Family Name";
    public static final String GIVEN_NAME = "Given Name";
    public static final String CONTRIBUTOR_ORDINAL_NUMBER = "Ordinal Number";
    public static final String TABLE_FIELD_FOR_EXPECTED_NVA_NAME = "Name";
    public static final String TABLE_FIELD_FOR_EXPECTED_AFFILIATION_URI = "Affiliation URI";
    private static final AtomicInteger nextContributorOrder = new AtomicInteger(0);
    
    public static CristinContributorBuilder toContributor(Map<String, String> nameMap) {
        return CristinContributorTransformer
            .toContributor(nameMap.get(GIVEN_NAME), nameMap.get(FAMILY_NAME));
    }
    
    public static CristinContributorBuilder toContributor(String givenName, String familyName) {
        return CristinContributor.builder()
            .withContributorOrder(nextContributorOrder.incrementAndGet())
            .withAffiliations(List.of(CristinDataGenerator.randomAffiliation()))
            .withIdentifier(CristinDataGenerator.largeRandomNumber())
            .withFamilyName(familyName)
            .withGivenName(givenName);
    }
    
    public static CristinContributorBuilder toContributorWithOrdinalNumber(
        Map<String, String> nameAndOrdinalNumberMap) {
        int ordinalNumber = Integer.parseInt(nameAndOrdinalNumberMap.get(CONTRIBUTOR_ORDINAL_NUMBER));
        return toContributor(nameAndOrdinalNumberMap).withContributorOrder(ordinalNumber);
    }
}
