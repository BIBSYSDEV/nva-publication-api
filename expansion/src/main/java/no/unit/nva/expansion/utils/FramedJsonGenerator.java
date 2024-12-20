package no.unit.nva.expansion.utils;

import static java.util.Objects.isNull;
import static no.unit.nva.expansion.utils.JsonLdDefaults.frameJsonLd;
import com.apicatalog.jsonld.document.Document;
import java.io.InputStream;
import java.util.List;
import nva.commons.core.JacocoGenerated;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JacocoGenerated
public class FramedJsonGenerator {

    private static final Logger logger = LoggerFactory.getLogger(FramedJsonGenerator.class);
    private final String framedJson;

    public FramedJsonGenerator(List<InputStream> streams, Document frame) {
        var model = createModel(streams);
        framedJson = frameJsonLd(model, frame);
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
        addTopLevelOrganizations(model);
        addContributorOrganizations(model);
        addSubUnitsToTopLevelAffiliation(model);
        return model;
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

    private void addTopLevelOrganizations(Model model) {
        var query = AffiliationQueries.TOP_LEVEL_ORGANIZATION;
        try (var qexec = QueryExecutionFactory.create(query, model)) {
            var topLevelNode = qexec.execConstruct();
            model.add(topLevelNode);
        }
    }

    private void addContributorOrganizations(Model model) {
        var query = AffiliationQueries.CONTRIBUTOR_ORGANIZATION;
        try (var qexec = QueryExecutionFactory.create(query, model)) {
            var organizations = qexec.execConstruct();
            model.add(organizations);
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
