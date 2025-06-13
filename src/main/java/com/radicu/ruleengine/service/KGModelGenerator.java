package com.radicu.ruleengine.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.*;

@Service
public class KGModelGenerator {

    private static final String MODEL_OUTPUT_DIRECTORY = Paths.get(System.getProperty("user.dir"), "src", "main", "java", "com", "radicu", "ruleengine", "model").toString();

    private static final String PREFIXES =
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
        "PREFIX mts: <http://mts.com/>\n";

    private static final String SPARQL_QUERY =
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
        "PREFIX mts: <http://mts.com/>\n" +
        "SELECT ?var ?name ?type ?constant WHERE {\n" +
        "    ?var rdf:type owl:NamedIndividual , mts:Variable .\n" +
        "    OPTIONAL { ?var mts:name ?name . }\n" +
        "    OPTIONAL { ?var mts:type ?type . }\n" +
        "    OPTIONAL { ?var <http://www.co-ode.org/ontologies/ont.owl#constantValue> ?constant . }\n" +
        "}";

    public void generateModelFromKG(String owlFilePath) throws IOException, TemplateException {
        // 1. Load KG Model
        Model model = ModelFactory.createDefaultModel();
        model.read(owlFilePath);

        // 2. Execute SPARQL Query
        Query query = QueryFactory.create(PREFIXES + SPARQL_QUERY);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();

            List<Map<String, String>> fields = new ArrayList<>();

            while (results.hasNext()) {
                QuerySolution sol = results.nextSolution();

                // Get variable name
                String varName = sol.contains("name") ? sol.getLiteral("name").getString() : null;
                if (varName == null) {
                    // fallback if name is missing
                    Resource varResource = sol.getResource("var");
                    varName = extractLocalName(varResource.getURI());
                }

                // Get variable type
                String typeLiteral = sol.contains("type") ? sol.getLiteral("type").getString() : null;
                String javaType = mapLiteralToJavaType(typeLiteral);

                Map<String, String> field = new HashMap<>();
                field.put("name", varName);
                field.put("type", javaType);

                // Check constant value
                if (sol.contains("constant")) {
                    field.put("constant", sol.getLiteral("constant").getString());
                }

                fields.add(field);
            }

            if (fields.isEmpty()) {
                throw new IllegalStateException("No variables found in the KG!");
            }

            // 3. Prepare Data for FreeMarker
            Map<String, Object> data = new HashMap<>();
            data.put("packageName", "com.radicu.ruleengine.model");
            data.put("className", "Variable"); // We always generate Variable.java
            data.put("fields", fields);

            // 4. Setup FreeMarker
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
            cfg.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "/templates");
            cfg.setDefaultEncoding("UTF-8");

            // 5. Load Template
            Template template = cfg.getTemplate("model.ftl");

            // 6. Generate Java File
            File outputDir = new File(MODEL_OUTPUT_DIRECTORY);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            String outputFilePath = Paths.get(MODEL_OUTPUT_DIRECTORY, "Variable.java").toString();
            try (Writer fileWriter = new FileWriter(outputFilePath)) {
                template.process(data, fileWriter);
            }

            System.out.println("Variable.java generated successfully at: " + outputFilePath);
        }
    }

    private String extractLocalName(String uri) {
        if (uri == null) return null;
        int idx = uri.lastIndexOf('/');
        if (idx != -1 && idx + 1 < uri.length()) {
            return uri.substring(idx + 1);
        }
        return uri;
    }

    private String mapLiteralToJavaType(String typeLiteral) {
        if (typeLiteral == null) {
            return "String";
        }
        switch (typeLiteral.toLowerCase()) {
            case "string": return "String";
            case "int":
            case "integer": return "int";
            case "float": return "float";
            case "double": return "double";
            case "boolean": return "boolean";
            case "decimal": return "java.math.BigDecimal";
            case "datetime": return "java.time.LocalDateTime";
            case "date": return "java.time.LocalDate";
            default: return "String";
        }
    }
}

