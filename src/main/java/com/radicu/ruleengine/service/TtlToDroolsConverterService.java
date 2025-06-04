package com.radicu.ruleengine.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.vocabulary.RDF;

import org.springframework.stereotype.Service;


import java.util.List;
import java.util.ArrayList;





@Service
public class TtlToDroolsConverterService {

    public String convertTtlToDrl(String ttlFilePath) {
        Model model = loadTtlFile(ttlFilePath);

        List<String> drlRules = new ArrayList<>();
        
        // Change this to your actual namespace!
        String namespace = "http://mts.com/"; 

        ResIterator rulesIterator = model.listResourcesWithProperty(
            RDF.type, 
            model.getResource(namespace + "Rule")
        );
        
       while (rulesIterator.hasNext()) {
            Resource ruleResource = rulesIterator.nextResource();
            String drlRule = convertRuleResourceToDrl(ruleResource, model, namespace);
            drlRules.add(drlRule);
        }

        return generateDrl(drlRules);
    }

    private Model loadTtlFile(String ttlFilePath) {
        Model model = ModelFactory.createDefaultModel();
        // Load using RDFDataMgr — will auto detect .ttl based on extension
        RDFDataMgr.read(model, ttlFilePath, Lang.TURTLE);
        return model;
    }

    private String convertRuleResourceToDrl(Resource rule, Model model, String namespace) {
        String ruleName = rule.hasProperty(model.getProperty(namespace + "name"))
            ? rule.getProperty(model.getProperty(namespace + "name")).getString()
            : extractLocalName(rule.getURI());

        int priority = rule.hasProperty(model.getProperty(namespace + "priority"))
            ? rule.getProperty(model.getProperty(namespace + "priority")).getInt()
            : 0;

        List<String> conditions = extractConditions(rule, model, namespace);
        List<String> actions = extractActions(rule, model, namespace);

        // Build DRL
        StringBuilder drl = new StringBuilder();
        drl.append("rule \"").append(ruleName).append("\"\n")
        .append("    salience ").append(priority).append("\n")
        .append("when\n");
        for (String cond : conditions) {
            drl.append("    ").append(cond).append("\n");
        }
        drl.append("then\n");
        for (String action : actions) {
            drl.append("    ").append(action).append(";\n");
        }
        drl.append("end\n");

        return drl.toString();
    }


    private List<String> extractConditions(Resource rule, Model model, String namespace) {
        List<String> conditions = new ArrayList<>();
        
        // Get condition literal
        if (rule.hasProperty(model.getProperty(namespace + "condition"))) {
            String conditionLiteral = rule.getProperty(model.getProperty(namespace + "condition")).getString();
            
            // Clean up: Remove '?' (because Drools needs plain field names)
            String cleanedCondition = conditionLiteral.replaceAll("\\?", "");
            
            // You can also split by '&&' if needed, or keep it as one line
            conditions.add("$fact : FactType(" + cleanedCondition + ")");
        }

        return conditions;
    }


    private List<String> extractActions(Resource rule, Model model, String namespace) {
        List<String> actions = new ArrayList<>();

        // Get decision literal
        if (rule.hasProperty(model.getProperty(namespace + "decision"))) {
            String decisionLiteral = rule.getProperty(model.getProperty(namespace + "decision")).getString();
            
            // Example: ?state = 'Continuous reasoning'
            // Extract field and value
            String[] parts = decisionLiteral.split("=");
            if (parts.length == 2) {
                String field = parts[0].trim().replaceAll("\\?", "");
                String value = parts[1].trim().replaceAll("'", "\""); // replace ' with " for DRL string
                
                actions.add("$fact.set" + capitalize(field) + "(" + value + ")");
            }
        }

        return actions;
    }


    private String generateDrl(List<String> rules) {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.radicu.generatedrules\n\n");
        for (String rule : rules) {
            sb.append(rule).append("\n\n");
        }
        return sb.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String extractLocalName(String uri) {
        if (uri == null) return "UnnamedRule";
        int lastSlash = uri.lastIndexOf('/');
        return lastSlash != -1 ? uri.substring(lastSlash + 1) : uri;
    }
}
