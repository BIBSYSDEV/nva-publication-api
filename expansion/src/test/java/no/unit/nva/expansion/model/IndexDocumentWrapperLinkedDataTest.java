package no.unit.nva.expansion.model;

import org.junit.jupiter.api.Test;

class IndexDocumentWrapperLinkedDataTest {

    @Test
    void testInverseRelation() {

        var ld = new IndexDocumentWrapperLinkedData(null);
        var result = ld.updateDataWithInverseRelations();
    }

}