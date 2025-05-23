package no.unit.nva.publication.utils;

import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.utils.CuratingInstitutionsUtil;
import org.junit.jupiter.api.Test;

class CuratingInstitutionsUtilTest {

    @Test
    void whenVerifiedContributorReturnInstitution() {
        var util = mock(CristinUnitsUtil.class);
        when(util.getTopLevel(any())).thenReturn(URI.create("https://example.com"));
        var list =
            CuratingInstitutionsUtil.getCuratingInstitutionsCached(
                PublicationGenerator.fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES)
                    .getEntityDescription(),
                util);

        assertThat(list, is(not(empty())));
    }

    @Test
    void whenNotVerifiedContributorDoNotReturnInstitution() {
        var util = mock(CristinUnitsUtil.class);
        when(util.getTopLevel(any())).thenReturn(URI.create("https://example.com"));
        var entityDescription = PublicationGenerator.fromInstanceClassesExcluding(
            PROTECTED_DEGREE_INSTANCE_TYPES).getEntityDescription();

        entityDescription.setContributors(entityDescription.getContributors().stream().map(contributor -> {
            contributor.getIdentity().setId(null);
            return contributor;
        }).toList());

        var list =
            CuratingInstitutionsUtil.getCuratingInstitutionsCached(
                entityDescription,
                util);

        assertThat(list, is(empty()));
    }
}
