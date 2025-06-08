package com.radicu.ruleengine.service;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ControllerUpdater {

    private static final String CONTROLLER_PATH = "src/main/java/com/radicu/ruleengine/controller/RuleController.java";

    public void addEvaluateEndpoint(String modelClassName) throws IOException {
        // 1. Read all lines of the controller file
        Path controllerFilePath = Paths.get(CONTROLLER_PATH);
        List<String> lines = Files.readAllLines(controllerFilePath);

        // 2. Find insertion point — before last '}' of class
        int insertPosition = lines.size() - 1;

        // 3. Create endpoint method text
        String modelParamName = decapitalize(modelClassName);
        String serviceInstanceName = "ruleEngineService" + modelClassName;

        String endpoint = String.format(
            "    @PostMapping(\"/evaluate-rule\")\n" +
            "    public ResponseEntity<%s> evaluateRule(@RequestBody %s %s) {\n" +
            "        %s result = %s.runRules(%s);\n" +
            "        return ResponseEntity.ok(result);\n" +
            "    }\n\n",
            modelClassName, modelClassName, modelParamName,
            modelClassName, serviceInstanceName, modelParamName
        );

        // 4. Insert endpoint code
        lines.add(insertPosition, endpoint);

        // 5. Write updated file back
        Files.write(controllerFilePath, lines);

        System.out.println("@PostMapping('/evaluate-rule') added successfully for model: " + modelClassName);
    }

    private String decapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}
