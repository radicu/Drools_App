package com.radicu.ruleengine.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class RuleEngineServiceGenerator {

    private static final String SERVICE_OUTPUT_DIRECTORY = Paths.get(System.getProperty("user.dir"), "src", "main", "java", "com", "radicu", "ruleengine", "service").toString();

    public void generateServiceFromModel(String modelClassName) throws IOException, TemplateException {
        // Step 1: Prepare Data for Template
        Map<String, Object> data = new HashMap<>();
        data.put("packageName", "com.radicu.ruleengine.service");
        data.put("serviceClassName", "RuleEngineService" + modelClassName);
        data.put("modelClassName", modelClassName);
        data.put("modelParamName", decapitalize(modelClassName));
        data.put("kieContainerName", decapitalize(modelClassName) + "KieContainer");

        // Step 2: Setup FreeMarker
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "/templates");
        cfg.setDefaultEncoding("UTF-8");

        // Step 3: Load service.ftl Template
        Template template = cfg.getTemplate("service.ftl");

        // Step 4: Generate Service Java File
        File outputDir = new File(SERVICE_OUTPUT_DIRECTORY);
        if (!outputDir.exists()) {
            outputDir.mkdirs(); // create directory if missing
        }

        String outputFilePath = Paths.get(SERVICE_OUTPUT_DIRECTORY, "RuleEngineService" + modelClassName + ".java").toString();
        try (Writer fileWriter = new FileWriter(outputFilePath)) {
            template.process(data, fileWriter);
        }

        System.out.println("RuleEngineService" + modelClassName + ".java generated successfully at: " + outputFilePath);
    }

    private String decapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}
