package com.radicu.ruleengine.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;


import com.radicu.ruleengine.model.RuleStructure;
import com.radicu.ruleengine.model.SensorStressData;
import com.radicu.ruleengine.model.Variable;//Un-comment this after upload KG
import com.radicu.ruleengine.service.ControllerUpdater;
import com.radicu.ruleengine.service.KGModelGenerator;
import com.radicu.ruleengine.service.RuleEngineServiceGenerator;
import com.radicu.ruleengine.service.RulesExcel;
import com.radicu.ruleengine.service.StressTestService;
import com.radicu.ruleengine.service.UniversalRuleExtractorService;
import com.radicu.ruleengine.service.RuleEngineServiceVariable;//Un-comment this after upload KG
import com.radicu.ruleengine.service.RuleEngineServiceVariable2;//Rules for exeriment

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;


@RestController
public class RuleController {

    private final RulesExcel ruleEngineService;
    private final UniversalRuleExtractorService universalRuleExtractorService;
    private final StressTestService stressTestService;
    private final KGModelGenerator kgModelGenerator;
    private final RuleEngineServiceGenerator ruleEngineServiceGenerator;
    private final ControllerUpdater controllerUpdater;
    private final RuleEngineServiceVariable ruleEngineServiceVariable;//Un-comment this after upload KG
    private final RuleEngineServiceVariable2 ruleEngineServiceVariable2;//For experiment

    // Log for debugging
    private static final Logger logger = LoggerFactory.getLogger(RuleController.class);

    // Constructor Injection for both services
    public RuleController(RulesExcel ruleEngineService, UniversalRuleExtractorService universalRuleExtractorService
    , StressTestService stressTestService, KGModelGenerator kgModelGenerator, 
    RuleEngineServiceGenerator ruleEngineServiceGenerator, ControllerUpdater controllerUpdater
    , RuleEngineServiceVariable ruleEngineServiceVariable //Un-comment this after upload KG
    , RuleEngineServiceVariable2 ruleEngineServiceVariable2//For experiment
    ) 
    {
        this.ruleEngineService = ruleEngineService;
        this.universalRuleExtractorService = universalRuleExtractorService;
        this.stressTestService = stressTestService;
        this.kgModelGenerator = kgModelGenerator;
        this.ruleEngineServiceGenerator = ruleEngineServiceGenerator;
        this.controllerUpdater = controllerUpdater;
        this.ruleEngineServiceVariable = ruleEngineServiceVariable; //Un-comment this after upload KG
        this.ruleEngineServiceVariable2 = ruleEngineServiceVariable2;
    }

    // Download rule in excel format
    @GetMapping("/download-rule")
        public ResponseEntity<List<RuleStructure>> downloadRules() {
        try {
            List<RuleStructure> rules = ruleEngineService.getAllRules();
            logger.info("Successfully loaded {} rules", rules.size());
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            logger.error("Rule loading failed", e);  // Add detailed logging
            return ResponseEntity.internalServerError().build();
        }
    }

   //Update rule using excel, still in work 
   @PostMapping("/update-rule")
        public ResponseEntity<String> updateRules(@RequestBody List<Map<String, Object>> rulesPayload) {
        try {
            // Parse JSON to DRL
            String drlContent = ruleEngineService.parseRulesToDRL(rulesPayload);
            
            // Save to file
            ruleEngineService.updateRuleFile(drlContent);
            
            return ResponseEntity.ok("Rules updated successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error updating rules: " + e.getMessage());
        }
    }

    @PostMapping("/api/converter/file-to-drl")
    public ResponseEntity<byte[]> convertFileToDrlAndGenerateModel(@RequestParam("file") MultipartFile file) {
        File tempFile = null;
        try {
            // 1. Save uploaded file temporarily
            tempFile = File.createTempFile("uploaded-", file.getOriginalFilename());
            file.transferTo(tempFile);

            String tempFilePath = tempFile.getAbsolutePath();

            // 2. Convert to DRL
            String drlContent = universalRuleExtractorService.convertRdfToDrl(tempFilePath);

            // 3. Generate Model.java
            kgModelGenerator.generateModelFromKG(tempFilePath);

            // 4. Generate Service.java
            ruleEngineServiceGenerator.generateServiceFromModel("Variable");

            // 5. Update Controller.java
            controllerUpdater.addEvaluateEndpoint("Variable");

            // 6. Prepare response for DRL file download
            byte[] drlBytes = drlContent.getBytes();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"converted_rules.drl\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(drlBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(("❌ Failed to process file: " + e.getMessage()).getBytes());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }


    //Stress-test
    @PostMapping("/stress-test")
        public SensorStressData run(@RequestBody SensorStressData sensorStressData) {
            return stressTestService.runStressTest(sensorStressData);
        }



    @PostMapping("/evaluate-rule")
    public ResponseEntity<Variable> evaluateRule(@RequestBody Variable variable) {
        Variable result = ruleEngineServiceVariable.runRules(variable);
        return ResponseEntity.ok(result);
    }

    //For experimenting rules
    @PostMapping("/evaluate-rule-exp")
    public ResponseEntity<Variable> evaluateRule2(@RequestBody Variable variable) {
        Variable result = ruleEngineServiceVariable2.runRules2(variable);
        return ResponseEntity.ok(result);
    }


}
