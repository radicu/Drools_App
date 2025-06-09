package com.radicu.ruleengine.service;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ControllerUpdater {

    public void addEvaluateEndpoint(String modelClassName) throws IOException {
        // Dynamically detect project root
        String projectRoot = System.getProperty("user.dir");
        Path controllerFilePath = Paths.get(projectRoot, "src", "main", "java", "com", "radicu", "ruleengine", "controller", "RuleController.java");

        List<String> lines = Files.readAllLines(controllerFilePath);

        // Check if endpoint already exists
        boolean endpointExists = lines.stream()
            .anyMatch(line -> line.contains("@PostMapping(\"/evaluate-rule\")"));

        if (endpointExists) {
            System.out.println("✅ @PostMapping('/evaluate-rule') already exists, skipping endpoint generation.");
            return; // Do nothing if already exists
        }

        // Find insertion point — before last '}'
        int insertPosition = lines.size() - 1;

        String modelParamName = decapitalize(modelClassName);
        String serviceInstanceName = "ruleEngineService" + modelClassName;

        String endpoint = String.format(
            "    // @PostMapping(\"/evaluate-rule\")\n" +
            "    // public ResponseEntity<%s> evaluateRule(@RequestBody %s %s) {\n" +
            "    //     %s result = %s.runRules(%s);\n" +
            "    //     return ResponseEntity.ok(result);\n" +
            "    // }\n\n",
            modelClassName, modelClassName, modelParamName,
            modelClassName, serviceInstanceName, modelParamName
        );

        lines.add(insertPosition, endpoint);

        Files.write(controllerFilePath, lines);

        System.out.println("✅ @PostMapping('/evaluate-rule') added successfully for model: " + modelClassName);
    }

    private String decapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}
