package no.unit.nva.expansion.utils;

import static java.util.Objects.isNull;
import static no.unit.nva.expansion.utils.JsonLdDefaults.frameJsonLd;
import static no.unit.nva.expansion.utils.JsonLdUtils.toJsonString;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.util.List;
import nva.commons.core.JacocoGenerated;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.apache.jena.update.UpdateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JacocoGenerated
public class FramedJsonGenerator {

    private static final Logger logger = LoggerFactory.getLogger(FramedJsonGenerator.class);
    private final String framedJson;

    public FramedJsonGenerator(JsonNode indexDocument, List<InputStream> streams, String frame) {
        removeAffiliationLabels(indexDocument);
        var indexDocumentInputStream = stringToStream(toJsonString(indexDocument));
        streams.add(indexDocumentInputStream);
        var model = createModel(streams);
        framedJson = frameJsonLd(model, frame);
    }

    private static void removeAffiliationLabels(JsonNode indexDocument) {
        indexDocument.at("/entityDescription/contributors")
            .forEach(contributor -> emptyAffiliationLabels(contributor.get("affiliations")));
    }

    private static void emptyAffiliationLabels(JsonNode affiliations) {
        affiliations.forEach(affiliation -> emptyLabels(affiliations));
    }

    private static void emptyLabels(JsonNode affiliations) {
        if (affiliations instanceof ObjectNode) {
            ((ObjectNode) affiliations).putObject("labels");
        }
    }

    private void removeOrganizationLabels(Model model) {
        UpdateAction.parseExecute(AffiliationQueries.REMOVE_ORGANIZATIONS_LABELS, model);
    }

    public String getFramedJson() {
        return framedJson;
    }

    private static void logInvalidJsonLdInput(Exception exception) {
        logger.warn("Invalid JSON LD input encountered: ", exception);
    }

    private Model createModel(List<InputStream> streams) {
        var model = ModelFactory.createDefaultModel();
        streams.forEach(s -> loadDataIntoModel(model, s));
        addTopLevelAffiliation(model);
        return addSubUnitsToTopLevelAffiliation(model);
    }

    private void loadDataIntoModel(Model model, InputStream inputStream) {
        if (isNull(inputStream)) {
            return;
        }
        try {
            RDFDataMgr.read(model, inputStream, Lang.JSONLD);
        } catch (RiotException e) {
            logInvalidJsonLdInput(e);
        }
    }

    private void addTopLevelAffiliation(Model model) {
        var query = AffiliationQueries.TOP_LEVEL_AFFILIATION;
        try (var qexec = QueryExecutionFactory.create(query, model)) {
            var topLevelNode = qexec.execConstruct();
            model.add(topLevelNode);
        }
    }

    private Model addSubUnitsToTopLevelAffiliation(Model model) {
        var query = AffiliationQueries.HAS_PART;
        try (var qexec = QueryExecutionFactory.create(query, model)) {
            var hasPartNodes = qexec.execConstruct();
            return model.add(hasPartNodes);
        }
    }
}
