package no.unit.nva.expansion.utils;

import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;

public final class FundingBlankNodeReplacer {

    private static final String NVA_PREFIX = "https://nva.sikt.no/ontology/publication#";

    private FundingBlankNodeReplacer() {}

    public static void replaceFundingBlankNodesInModel(Model model) {
        constructSkolemizedFundings(model);
        deleteBlankNodeFundings(model);
    }
    
    private static void constructSkolemizedFundings(Model model) {
        var constructQuery = """
            PREFIX nva: <%s>
            
            CONSTRUCT {
                ?publication nva:funding ?skolemizedFunding .
                ?skolemizedFunding ?p ?o .
            } WHERE {
                ?publication a nva:Publication ;
                OPTIONAL {
                    ?publication nva:funding ?funding .
                    ?funding ?p ?o .
                    FILTER(isBlank(?funding))
                    FILTER(?funding = ?funding)
            
                    OPTIONAL {
                        ?funding nva:source ?source .
                    }
                    OPTIONAL {
                        ?funding nva:identifier ?identifier .
                    }
            
                    BIND(
                     IF(isBlank(?funding),
                        IRI(CONCAT(STR(?source), "/", STR(?identifier))),
                        ?funding
                     ) AS ?skolemizedFunding
                   )
                }
            }
            """.formatted(NVA_PREFIX);
        
        try (var qexec = QueryExecutionFactory.create(QueryFactory.create(constructQuery), model)) {
            var constructedModel = qexec.execConstruct();
            model.add(constructedModel);
        }
    }
    
    private static void deleteBlankNodeFundings(Model model) {
        var deleteUpdate = """
            PREFIX nva: <%s>
            
            DELETE {
                ?publication nva:funding ?funding .
                ?funding ?p ?o .
            } WHERE {
                ?publication a nva:Publication ;
                    nva:funding ?funding .
                ?funding ?p ?o .
                FILTER(isBlank(?funding))
            }
            """.formatted(NVA_PREFIX);
        
        UpdateAction.execute(UpdateFactory.create(deleteUpdate), model);
    }
}