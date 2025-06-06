package com.radicu.ruleengine.service;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class UniversalRuleExtractorService {

    // private static final String NAMESPACE = "http://mts.com/";

    private static final String RULES_PATH2= "src/main/resources/rules/converted_rules.drl";

    // ✅ Fields matching your Java model (camelCase)
    private static final Set<String> validFields = new HashSet<>(Arrays.asList(
        "spindleCurrent",
        "alarmLimit3200",
        "alarmLimit4200",
        "alarmLimit5100",
        "alarmLimit2",
        "xTableCurrent",
        "yTableCurrent",
        "drillingCondition1",
        "drillingCondition2",
        "drillingCondition3",
        "state"
    ));


    public String convertRdfToDrl(String filePath) throws IOException {
        InputStream in = FileManager.get().open(filePath);
        if (in == null) {
            throw new IllegalArgumentException("File not found at path: " + filePath);
        }

        Model model = ModelFactory.createDefaultModel();
        String format = detectFormat(filePath); // Detect format TTL/RDF/XML
        model.read(in, null, format);

        StringBuilder drl = new StringBuilder();
        drl.append("package rules;\n\n");
        drl.append("import com.radicu.ruleengine.model.SpindleData;\n\n");

        String sparqlQuery = """
            PREFIX mts: <http://mts.com/>
            SELECT ?rule ?name ?priority ?condition ?decision
            WHERE {
                ?rule a mts:Rule ;
                    mts:name ?name ;
                    mts:priority ?priority .
                OPTIONAL { ?rule mts:condition ?condition }
                OPTIONAL { ?rule mts:decision ?decision }
            }
            """;

        Query query = QueryFactory.create(sparqlQuery);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String ruleName = soln.getLiteral("name").getString();
                int priority = soln.getLiteral("priority").getInt();

                String condition = soln.contains("condition") ? soln.getLiteral("condition").getString() : "";
                String decision = soln.contains("decision") ? soln.getLiteral("decision").getString() : "";

                String drlRule = convertToDrlRule(ruleName, priority, condition, decision);
                if (!drlRule.isEmpty()) {
                    drl.append(drlRule).append("\n\n");
                }
            }
        }

        // ✅ Save to file right before returning
        updateRuleFile(drl.toString());

        return drl.toString();
    }


    private String convertToDrlRule(String ruleName, int priority, String conditionLiteral, String decisionLiteral) {
        List<String> conditions = extractConditions(conditionLiteral);
        List<String> actions = extractActions(decisionLiteral); // 🚩 Now multiple actions

        if (conditions.isEmpty() && actions.isEmpty()) {
            System.out.println("⚠️ Skipping rule: " + ruleName + " due to no conditions and no actions.");
            return "";
        }

        StringBuilder drl = new StringBuilder();
        drl.append("rule \"").append(ruleName).append("\"\n")
                .append("    salience ").append(priority).append("\n")
                .append("when\n");

        if (!conditions.isEmpty()) {
            for (String cond : conditions) {
                drl.append("    ").append(cond).append("\n");
            }
        }

        drl.append("then\n");

        if (!actions.isEmpty()) {
            for (String action : actions) {
                drl.append("    ").append(action).append("\n");  // 🚩 Each action, one per line
            }
        }

        drl.append("end");

        return drl.toString();
    }



    private List<String> extractConditions(String conditionLiteral) {
        List<String> conditions = new ArrayList<>();
        Set<String> usedFields = new HashSet<>();

        if (conditionLiteral != null && !conditionLiteral.isEmpty()) {
            // Clean the condition string
            String cleanedCondition = conditionLiteral.replaceAll("\\?", "").trim();
            String[] conditionParts = cleanedCondition.split("&&");

            StringBuilder conditionBuilder = new StringBuilder();
            conditionBuilder.append("$fact : SpindleData(");

            boolean first = true;
            for (String part : conditionParts) {
                String cond = part.trim();
                if (cond.isEmpty()) continue;

                // 🚩 NEW: Smart detect comparison vs bare field
                Pattern pattern = Pattern.compile("^([a-zA-Z0-9_]+)\\s*([><=!]+)\\s*(.+)$"); // left operator right
                Matcher matcher = pattern.matcher(cond);

                if (matcher.find()) {
                    // Comparison condition
                    String leftField = matcher.group(1).trim();
                    String operator = matcher.group(2).trim();
                    String rightValue = matcher.group(3).trim();

                    String normalizedLeft = normalizeFieldName(leftField);
                    String normalizedRight = normalizeFieldName(rightValue.replaceAll("\\?", ""));

                    boolean validLeft = validFields.contains(normalizedLeft);
                    boolean validRight = isNumeric(rightValue) || validFields.contains(normalizedRight) ||
                            (rightValue.startsWith("'") && rightValue.endsWith("'")) ||
                            (rightValue.startsWith("\"") && rightValue.endsWith("\""));

                    if (validLeft && validRight) {
                        String updatedCond = normalizedLeft + " " + operator + " ";

                        if (isNumeric(rightValue)) {
                            updatedCond += rightValue;
                        } else if ((rightValue.startsWith("'") && rightValue.endsWith("'")) ||
                                (rightValue.startsWith("\"") && rightValue.endsWith("\""))) {
                            // String literal case: remove outer quotes and wrap with double quotes
                            String cleanString = rightValue.substring(1, rightValue.length() - 1);
                            updatedCond += "\"" + cleanString + "\"";
                        } else {
                            updatedCond += normalizedRight;
                        }

                        if (!first) {
                            conditionBuilder.append(", ");
                        }
                        conditionBuilder.append(updatedCond);
                        first = false;
                    }
                } else {
                    // 🚩 Bare field condition (float, assume > 0.0 check)
                    String fieldName = cond;
                    String normalizedField = normalizeFieldName(fieldName);

                    if (validFields.contains(normalizedField) && !usedFields.contains(normalizedField)) {
                        usedFields.add(normalizedField);

                        String updatedCond = normalizedField + " > 0.0";

                        if (!first) {
                            conditionBuilder.append(", ");
                        }
                        conditionBuilder.append(updatedCond);
                        first = false;
                    }
                }
            }

            conditionBuilder.append(")");

            if (!first) {
                conditions.add(conditionBuilder.toString());
            }
        }

        return conditions;
    }





    private List<String> extractActions(String decisionLiteral) {
        List<String> actions = new ArrayList<>();

        if (decisionLiteral == null || decisionLiteral.isEmpty()) {
            return actions;
        }

        // Split by ; → multiple decisions
        String[] decisions = decisionLiteral.split(";");

        for (String decision : decisions) {
            decision = decision.trim();
            if (decision.isEmpty()) continue;

            if (decision.contains("=")) {
                String[] parts = decision.split("=");

                if (parts.length == 2) {
                    String fieldName = parts[0].replace("?", "").trim();
                    String value = parts[1].trim();

                    if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) {
                        value = value.substring(1, value.length() - 1); // remove outer quotes
                        value = "\"" + value + "\""; // make double quotes
                    }

                    String normalizedField = normalizeFieldName(fieldName);

                    actions.add("$fact.set" + capitalize(normalizedField) + "(" + value + ");");
                }
            }
        }

        return actions;
    }




    private String normalizeFieldName(String fieldName) {
    if (fieldName == null || fieldName.isEmpty()) return fieldName;

        // Lowercase first character, preserve numbers
        return fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
    }





   private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }


    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        if (str.contains("_")) {
            String[] parts = str.split("_");
            StringBuilder capitalized = new StringBuilder();
            for (String part : parts) {
                capitalized.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
            }
            return capitalized.toString();
        } else {
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }
    }

    private String detectFormat(String filePath) {
        if (filePath.endsWith(".ttl")) {
            return "TURTLE";
        } else if (filePath.endsWith(".rdf") || filePath.endsWith(".owl")) {
            return "RDF/XML";
        } else if (filePath.endsWith(".nt")) {
            return "N-TRIPLES";
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + filePath);
        }
    }

    public void updateRuleFile(String drlContent) throws IOException {
        // Get project root from working directory
        String projectRoot = System.getProperty("user.dir");
        Path fullPath = Paths.get(projectRoot, RULES_PATH2);
        
        // Ensure directories exist and write file
        Files.createDirectories(fullPath.getParent());
        Files.write(fullPath, drlContent.getBytes());
    }
}
