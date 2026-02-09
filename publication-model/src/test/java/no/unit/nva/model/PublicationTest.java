package no.unit.nva.model;

import static no.unit.nva.DatamodelConfig.dataModelObjectMapper;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.json.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Set;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.NullAssociatedArtifact;
import no.unit.nva.model.config.ResourcesBuildConfig;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.util.ContextUtil;
import nva.commons.core.ioutils.IoUtils;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.Test;

public class PublicationTest {

    public static final String PUBLICATION_CONTEXT_JSON = Publication.getJsonLdContext(URI.create("https://localhost"));
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final String DOI_REQUEST_FIELD = "doiRequest";
    public static final String FRAME = "publicationFrame.json";
    public static final String BOOK_REVISION_FIELD = ".entityDescription.reference.publicationContext.revision";
    public static final String ALLOWED_OPERATIONS_FIELD = "allowedOperations";
    public static final String IMPORT_DETAILS_FIELD = "importDetails";


    @Test
    void getModelVersionReturnsModelVersionDefinedByGradle() {
        Publication samplePublication = PublicationGenerator.randomPublication();
        assertThat(samplePublication.getModelVersion(), is(equalTo(ResourcesBuildConfig.RESOURCES_MODEL_VERSION)));
    }

    @Test
    void equalsReturnsTrueWhenTwoPublicationInstancesHaveEquivalentFields() {
        Publication samplePublication = PublicationGenerator.randomPublication();
        Publication copy = samplePublication.copy().build();

        assertThat(copy, doesNotHaveEmptyValuesIgnoringFields(Set.of(DOI_REQUEST_FIELD, BOOK_REVISION_FIELD,
                                                                     IMPORT_DETAILS_FIELD)));

        Diff diff = JAVERS.compare(samplePublication, copy);
        assertThat(copy, is(not(sameInstance(samplePublication))));
        assertThat(diff.prettyPrint(),copy, is(equalTo(samplePublication)));
    }

    @Test
    void objectMapperReturnsSerializationWithAllFieldsSerialized()
            throws JsonProcessingException {
        Publication samplePublication = PublicationGenerator.randomPublication();
        String jsonString = dataModelObjectMapper.writeValueAsString(samplePublication);
        Publication copy = dataModelObjectMapper.readValue(jsonString, Publication.class);
        assertThat(copy, is(equalTo(samplePublication)));
    }

    @Test
    void objectMapperShouldSerializeAndDeserializeNullAssociatedArtifact() throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(new NullAssociatedArtifact()));
        var serialized = dataModelObjectMapper.writeValueAsString(publication);
        var deserialized = dataModelObjectMapper.readValue(serialized, Publication.class);
        assertThat(deserialized, is(equalTo(publication)));
    }

    @Test
    void some() throws JsonProcessingException {
        var json = """
            {"type":"Publication","identifier":"019aedccff91-a2ac75af-9077-4d33-a32e-d955bb989063","status":"PUBLISHED","resourceOwner":{"owner":"1270574@195.0.0.0","ownerAffiliation":"https://api.nva.unit.no/cristin/organization/195.0.0.0"},"publisher":{"type":"Organization","id":"https://api.nva.unit.no/customer/256b9785-808b-4c2a-a2d9-fd7a2de8da0c"},"createdDate":"2025-12-05T09:17:06.321843056Z","modifiedDate":"2026-02-03T13:48:59.841662765Z","publishedDate":"2025-12-05T09:29:06.411411404Z","entityDescription":{"type":"EntityDescription","mainTitle":"A Review of the Spatiotemporal Evolution of the High Arctic Large Igneous Province, and a New U‐Pb Age of a Mafic Sill Complex on Svalbard","alternativeTitles":{},"language":"http://lexvo.org/id/iso639-3/eng","publicationDate":{"type":"PublicationDate","year":"2025","month":"11","day":"28"},"contributors":[{"type":"Contributor","identity":{"type":"Identity","id":"https://api.nva.unit.no/cristin/person/1821793","name":"Anna Marie Rose Sartell","orcId":"https://orcid.org/0000-0002-6320-150X","verificationStatus":"Verified","additionalIdentifiers":[]},"affiliations":[{"type":"Organization","id":"https://api.nva.unit.no/cristin/organization/10300010.0.0.0"},{"type":"Organization","id":"https://api.nva.unit.no/cristin/organization/195.20.0.0"}],"role":{"type":"Creator"},"sequence":1,"correspondingAuthor":true},{"type":"Contributor","identity":{"type":"Identity","name":"U. Söderlund","nameType":"Personal","additionalIdentifiers":[]},"affiliations":[{"type":"Organization","id":"https://api.nva.unit.no/cristin/organization/10600030.0.0.0"}],"role":{"type":"Creator"},"sequence":2,"correspondingAuthor":false},{"type":"Contributor","identity":{"type":"Identity","id":"https://api.nva.unit.no/cristin/person/59846","name":"Kim Senger","orcId":"https://orcid.org/0000-0001-5379-4658","verificationStatus":"Verified","additionalIdentifiers":[]},"affiliations":[{"type":"Organization","id":"https://api.nva.unit.no/cristin/organization/14400272.0.0.0"},{"type":"Organization","id":"https://api.nva.unit.no/cristin/organization/195.20.0.0"}],"role":{"type":"Creator"},"sequence":3,"correspondingAuthor":false},{"type":"Contributor","identity":{"type":"Identity","id":"https://api.nva.unit.no/cristin/person/600446","name":"H. J. Kjøll","nameType":"Personal","orcId":"https://orcid.org/0000-0001-9560-3389","verificationStatus":"Verified","additionalIdentifiers":[]},"affiliations":[{"type":"Organization","id":"https://api.nva.unit.no/cristin/organization/185.15.18.10"},{"type":"Organization","id":"https://api.nva.unit.no/cristin/organization/185.15.22.0"}],"role":{"type":"Creator"},"sequence":4,"correspondingAuthor":false},{"type":"Contributor","identity":{"type":"Identity","id":"https://api.nva.unit.no/cristin/person/11213","name":"Olivier Galland","orcId":"https://orcid.org/0000-0002-8087-428X","verificationStatus":"Verified","additionalIdentifiers":[]},"affiliations":[{"type":"Organization","id":"https://api.nva.unit.no/cristin/organization/185.15.18.10"},{"type":"Organization","id":"https://api.nva.unit.no/cristin/organization/185.15.22.0"}],"role":{"type":"Creator"},"sequence":5,"correspondingAuthor":false}],"alternativeAbstracts":{},"tags":[],"reference":{"type":"Reference","publicationContext":{"type":"Journal","id":"https://api.nva.unit.no/publication-channels-v2/serial-publication/B4404EEA-4C64-4B6C-B066-C463644EDDFB/2025"},"doi":"https://doi.org/10.1029/2024gc011842","publicationInstance":{"type":"AcademicArticle","volume":"26","issue":"12","articleNumber":"e2024GC011842","pages":{"type":"Range"}}},"metadataSource":"https://doi.org/10.1029/2024GC011842","abstract":"The High Arctic Large Igneous Province (HALIP) formed in the circum‐Arctic during the Cretaceous. The timing and duration of emplacement of these mafic magmas are important for understanding the climatic and environmental effects, yet many uncertainties remain. The dating methods used vary greatly between different regions. For example, the mafic intrusions in Svalbard have mainly been dated using the 40 K/ 40 Ar method, which is more sensitive to overprinting at lower temperatures. This is problematic especially in the Arctic, where the Eocene Eurekan orogeny has impacted the intrusions post‐emplacement. Meanwhile, in the Canadian Arctic, 206 Pb/ 238 U dating on zirconium minerals has been the most common method employed, which requires much higher temperatures to be reset. We present a new compilation of ages for HALIP igneous and volcanic rocks in the circum‐Arctic, derived from a thorough review and reassessment of previously reported data. This compilation applies rigorous, method‐specific criteria to evaluate the reliability of existing HALIP age determinations, ensuring traceability and applicability for future data sets. By establishing a robust framework for assessing age data, this approach enhances the reliability of geological interpretations of HALIP magmatism, and highlights, for example, the spatial migration of peak magmatic activity through time in the High Arctic. To improve our understanding of the temporal evolution of the HALIP, we also present a new 206 Pb/ 238 U baddeleyite isotopic dilution thermal ionization mass spectrometry age from Svalbard. The new weighted mean 206 Pb/ 238 U age from Svalbard, 123.3 ± 1.6 Ma, is based on six samples belonging to one large sill. This age is in perfect agreement with existing published 206 Pb/ 238 U and 40 Ar/ 39 Ar ages, and suggests magma emplacement on Svalbard between 124.7 ± 0.3 and 120.2 ± 1.9 Ma ago.","description":""},"projects":[{"type":"ResearchProject","id":"https://api.nva.unit.no/cristin/project/2749239","name":"Igneous complex of Ekmanfjorden: an integrated field, petrological and geochemical study","approvals":[]},{"type":"ResearchProject","id":"https://api.nva.unit.no/cristin/project/2749240","name":"Structure and emplacement mechanisms of the large sill complexes of central Isfjorden","approvals":[]},{"type":"ResearchProject","id":"https://api.nva.unit.no/cristin/project/2747691","name":"Changes at the Top of the World through Volcanism and Plate Tectonics: Arctic Norwegian-Russian-North American collaboration\\n(NOR-R-AM2)","approvals":[]},{"type":"ResearchProject","id":"https://api.nva.unit.no/cristin/project/2497282","name":"Research Centre for Arctic Petroleum Exploration (ARCEX)","approvals":[]}],"fundings":[],"subjects":[],"associatedArtifacts":[{"identifier":"f532a880-0054-4da0-a54b-d37787dac9cf","type":"OpenFile","name":"Geochem Geophys Geosyst - 2025 - Sartell - A Review of the Spatiotemporal Evolution of the High Arctic Large Igneous.pdf","mimeType":"application/pdf","size":2333372,"license":"https://creativecommons.org/licenses/by/4.0/","publisherVersion":"PublishedVersion","rightsRetentionStrategy":{"type":"NullRightsRetentionStrategy","configuredType":"OverridableRightsRetentionStrategy"},"publishedDate":"2025-12-30T08:15:10.952368909Z","uploadDetails":{"type":"UserUploadDetails","uploadedBy":"1270574@195.0.0.0","uploadedDate":"2025-12-05T09:27:54.324295029Z"},"allowedOperations":["download","read-metadata"]}],"importDetails":[],"additionalIdentifiers":[{"type":"HandleIdentifier","sourceName":"nva@sikt","value":"https://hdl.handle.net/11250/5327196"},{"type":"CristinIdentifier","sourceName":"nva@null","value":"10295071"}],"allowedOperations":["update","partial-update"],"pendingOpenFileCount":0,"publicationNotes":[],"@context":{"@vocab":"https://nva.sikt.no/ontology/publication#","activeFrom":{"@type":"xsd:dateTime"},"activeTo":{"@type":"xsd:dateTime"},"additionalIdentifiers":{"@container":"@set","@id":"additionalIdentifier"},"administrativeAgreement":{"@id":"administrativeAgreement","@type":"xsd:boolean"},"affiliations":{"@container":"@set","@id":"affiliation"},"alternativeAbstracts":{"@container":"@language","@id":"alternativeTitle"},"alternativeTitles":{"@container":"@language","@id":"alternativeTitle"},"amount":{"@type":"xsd:integer"},"approvalStatus":{"@context":{"@vocab":"https://nva.sikt.no/ontology/publication#"},"@type":"@vocab"},"approvals":{"@container":"@set","@id":"approval"},"approvedBy":{"@context":{"@vocab":"https://nva.sikt.no/ontology/approvals-body#"},"@type":"@vocab"},"architectureOutput":{"@container":"@set","@id":"architectureOutput"},"associatedArtifacts":{"@container":"@set","@id":"associatedArtifact"},"compliesWith":{"@container":"@set","@id":"compliesWith"},"concertProgramme":{"@container":"@set","@id":"concertProgramme"},"contributorCristinIds":{"@container":"@set","@id":"contributorCristinId","@type":"@id"},"contributorsPreview":{"@container":"@set"},"contributorOrganizations":{"@container":"@set","@id":"contributorOrganization","@type":"@id"},"contributors":{"@container":"@set","@id":"contributor"},"correspondingAuthor":{"@type":"xsd:boolean"},"countryCode":{"@id":"country"},"createdDate":{"@type":"xsd:dateTime"},"curatingInstitutions":{"@container":"@set","@id":"curatingOrganization","@type":"@id"},"days":{"@type":"xsd:integer"},"doi":{"@type":"@id"},"duplicateOf":{"@type":"@id"},"embargoDate":{"@type":"xsd:dateTime"},"extent":{"@type":"xsd:integer"},"from":{"@type":"xsd:dateTime"},"fundings":{"@container":"@set","@id":"funding"},"handle":{"@type":"@id"},"hasPart":{"@container":"@set"},"hours":{"@type":"xsd:integer"},"id":"@id","illustrated":{"@type":"xsd:boolean"},"importDetails":{"@container":"@set","@id":"importDetail"},"indexedDate":{"@type":"xsd:dateTime"},"isbnList":{"@container":"@set","@id":"isbn"},"labels":{"@container":"@language","@id":"label"},"language":{"@type":"@id"},"link":{"@type":"@id"},"manifestations":{"@container":"@set","@id":"manifestation"},"metadataSource":{"@type":"@id"},"minutes":{"@type":"xsd:integer"},"modifiedDate":{"@type":"xsd:dateTime"},"musicalWorks":{"@container":"@set","@id":"musicalWork"},"nameType":{"@context":{"@vocab":"https://nva.sikt.no/ontology/publication#"},"@type":"@vocab"},"outputs":{"@container":"@set","@id":"output"},"ownerAffiliation":{"@type":"@id"},"partOf":{"@container":"@set"},"premiere":{"@type":"xsd:boolean"},"projects":{"@container":"@set","@id":"project"},"publicationNotes":{"@container":"@set","@id":"publicationNote"},"publishedDate":{"@type":"xsd:dateTime"},"ranking":{"@type":"xsd:integer"},"referencedBy":{"@container":"@set","@id":"referencedBy"},"related":{"@container":"@set","@id":"related"},"role":{"@id":"role"},"sequence":{"@type":"xsd:integer"},"size":{"@type":"xsd:integer"},"source":{"@type":"@id"},"status":{"@context":{"@vocab":"https://nva.sikt.no/ontology/publication#"},"@type":"@vocab"},"subjects":{"@container":"@set","@id":"subject","@type":"@id"},"tags":{"@container":"@set","@id":"tag"},"to":{"@type":"xsd:dateTime"},"topLevelOrganizations":{"@container":"@set","@id":"topLevelOrganization"},"trackList":{"@container":"@set","@id":"trackList"},"type":"@type","userAgreesToTermsAndConditions":{"@type":"xsd:boolean"},"valid":{"@type":"xsd:boolean"},"venues":{"@container":"@set","@id":"venue"},"visibleForNonOwner":{"@type":"xsd:boolean"},"weeks":{"@type":"xsd:integer"},"pendingOpenFileCount":{"@type":"xsd:integer"},"xsd":"http://www.w3.org/2001/XMLSchema#"},"id":"https://api.nva.unit.no/publication/019aedccff91-a2ac75af-9077-4d33-a32e-d955bb989063"}
            """;
        var publication = JsonUtils.dtoObjectMapper.readValue(json, Publication.class);
        var s = "";
    }

    protected JsonNode toPublicationWithContext(Publication publication) throws IOException {
        JsonNode document = dataModelObjectMapper.readTree(dataModelObjectMapper.writeValueAsString(publication));
        JsonNode context = dataModelObjectMapper.readTree(PUBLICATION_CONTEXT_JSON);
        ContextUtil.injectContext(document, context);
        return document;
    }

    protected JsonObject produceFramedPublication(JsonNode publicationWithContext) {
        try {
            var input = IoUtils.stringToStream(dataModelObjectMapper.writeValueAsString(publicationWithContext));
            var document = JsonDocument.of(input);
            var frame = JsonDocument.of(IoUtils.inputStreamFromResources(FRAME));
            return JsonLd.frame(document, frame).get();
        } catch (JsonLdError | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
