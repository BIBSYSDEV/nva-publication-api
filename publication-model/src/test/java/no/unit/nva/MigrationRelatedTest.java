package no.unit.nva;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.ConfirmedDocument;
import no.unit.nva.model.instancetypes.degree.RelatedDocument;
import no.unit.nva.model.instancetypes.degree.UnconfirmedDocument;
import no.unit.nva.model.instancetypes.researchdata.DataManagementPlan;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Deprecated
public class MigrationRelatedTest {

    public static Stream<Arguments> publicationInstanceWithRelatedSetProviderForDMP() {
        return Stream.of(
            Arguments.of("{\n"
                         + "\"type\" : \"DataManagementPlan\",\n"
                         + "\"related\" : [ \"https://www.example.com/WZZKKtl074rSklWDsy\" ],\n"
                         + "\"pages\" : {\n"
                         + "\"type\" : \"MonographPages\",\n"
                         + "\"introduction\" : {\n"
                         + "\"type\" : \"Range\",\n"
                         + "\"begin\" : \"y3r2zqlZbIcFjxF\",\n"
                         + "\"end\" : \"yskPffUtMN77qfxVn3\"\n"
                         + "},\n"
                         + "\"pages\" : \"M5bEQ4Qhguwmv3\",\n"
                         + "\"illustrated\" : true\n"
                         + "}\n"
                         + "}", ConfirmedDocument.class, DataManagementPlan.class),
                         Arguments.of("{\n"
                         + "\"type\" : \"DataManagementPlan\",\n"
                         + "\"related\" : [ {\n"
                         + "\"type\" : \"ConfirmedDocument\",\n"
                         + "\"identifier\" : \"https://www.example.com/b8mFWO7DuJTxt8DB\"\n"
                         + "} ],\n"
                         + "\"pages\" : {\n"
                         + "\"type\" : \"MonographPages\",\n"
                         + " \"introduction\" : {\n"
                         + "\"type\" : \"Range\",\n"
                         + "\"begin\" : \"y3r2zqlZbIcFjxF\",\n"
                         + "\"end\" : \"yskPffUtMN77qfxVn3\"\n"
                         + "},\n"
                         + "\"pages\" : \"M5bEQ4Qhguwmv3\",\n"
                         + "\"illustrated\" : true\n"
                         + "}\n"
                         + "}", ConfirmedDocument.class, DataManagementPlan.class),
                         Arguments.of("{\n"
                         + "\"type\" : \"DataManagementPlan\",\n"
                         + "\"related\" : [ {\n"
                         + "\"type\" : \"UnconfirmedDocument\",\n"
                         + "\"identifier\" : \"12345\"\n"
                         + "} ],\n"
                         + "\"pages\" : {\n"
                         + "\"type\" : \"MonographPages\",\n"
                         + "\"introduction\" : {\n"
                         + "\"type\" : \"Range\",\n"
                         + "\"begin\" : \"y3r2zqlZbIcFjxF\",\n"
                         + "\"end\" : \"yskPffUtMN77qfxVn3\"\n"
                         + "},\n"
                         + "\"pages\" : \"M5bEQ4Qhguwmv3\",\n"
                         + "\"illustrated\" : true\n"
                         + "}\n"
                         + "}", UnconfirmedDocument.class, DataManagementPlan.class));
    }

    public static Stream<Arguments> publicationInstanceWithRelatedSetProviderForDataSet() {
        return Stream.of(
            Arguments.of("{\n"
                         + "\"type\" : \"DataSet\",\n"
                         + "\"userAgreesToTermsAndConditions\" : false,\n"
                         + "\"geographicalCoverage\" : {\n"
                         + "\"type\" : \"GeographicalDescription\",\n"
                         + "\"description\" : \"GKBtKeOisF0GATbobH\"\n"
                         + "},\n"
                         + "\"referencedBy\" : [ \"https://www.example.com/T8EnYDhn7iu\" ],\n"
                         + "\"related\" : [ \"https://www.example.com/p7z6PI9zBkL\" ],\n"
                         + "\"compliesWith\" : "
                         + "[ \"https://www.example.com/n4DuRn51ChfBN6E0Lz\" ],\n"
                         + "\"pages\" : {\n"
                         + "\"type\" : \"NullPages\"\n"
                         + "}\n"
                         + "}", ConfirmedDocument.class, DataSet.class),
            Arguments.of("{\n"
                         + "\"type\" : \"DataSet\",\n"
                         + "\"userAgreesToTermsAndConditions\" : false,\n"
                         + "\"geographicalCoverage\" : {\n"
                         + "\"type\" : \"GeographicalDescription\",\n"
                         + "\"description\" : \"GKBtKeOisF0GATbobH\"\n"
                         + "},\n"
                         + "\"referencedBy\" : [ \"https://www.example.com/T8EnYDhn7iu\" ],\n"
                         + "\"related\" : [ {\n"
                         + "\"type\" : \"ConfirmedDocument\",\n"
                         + "\"identifier\" : \"https://www.example.com/b8mFWO7DuJTxt8DB\"\n"
                         + "} ],\n"
                         + "\"compliesWith\" : [ \"https://www.example.com/n4DuRn51ChfBN6E0Lz\" ],\n"
                         + "\"pages\" : {\n"
                         + "\"type\" : \"NullPages\"\n"
                         + "}\n"
                         + "}", ConfirmedDocument.class, DataSet.class),
            Arguments.of("{\n"
                         + "\"type\" : \"DataSet\",\n"
                         + "\"userAgreesToTermsAndConditions\" : false,\n"
                         + "\"geographicalCoverage\" : {\n"
                         + "\"type\" : \"GeographicalDescription\",\n"
                         + "\"description\" : \"GKBtKeOisF0GATbobH\"\n"
                         + "},\n"
                         + "\"referencedBy\" : [ \"https://www.example.com/T8EnYDhn7iu\" ],\n"
                         + "\"related\" : [ {\n"
                         + "\"type\" : \"UnconfirmedDocument\",\n"
                         + "\"identifier\" : \"12345\"\n"
                         + "} ],\n"
                         + "\"compliesWith\" : [ \"https://www.example.com/n4DuRn51ChfBN6E0Lz\" ],\n"
                         + "\"pages\" : {\n"
                         + "\"type\" : \"NullPages\"\n"
                         + "}\n"
                         + "}", UnconfirmedDocument.class));
    }

    @Disabled
    @ParameterizedTest
    @MethodSource("publicationInstanceWithRelatedSetProviderForDMP")
    void shouldTakeJsonAsInputAndConvertToDataManagementPlanWithSetOfRelatedDocuments(
        String json, Class<? extends RelatedDocument> expectedClass)
        throws JsonProcessingException {

        var object = (DataManagementPlan) JsonUtils.dtoObjectMapper.readValue(json, PublicationInstance.class);

        assertThat(object.getRelated().iterator().next(), is(instanceOf(expectedClass)));
    }

    @Disabled
    @ParameterizedTest
    @MethodSource("publicationInstanceWithRelatedSetProviderForDataSet")
    void shouldTakeJsonAsInputAndConvertToDataSetWithSetOfRelatedDocuments(
        String json, Class<? extends RelatedDocument> expectedClass)
        throws JsonProcessingException {

        var object = (DataSet) JsonUtils.dtoObjectMapper.readValue(json, PublicationInstance.class);

        assertThat(object.getRelated().iterator().next(), is(instanceOf(expectedClass)));
    }
}
