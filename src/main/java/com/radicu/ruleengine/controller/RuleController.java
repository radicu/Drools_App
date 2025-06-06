package com.radicu.ruleengine.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;


import com.radicu.ruleengine.model.DrillRule;
import com.radicu.ruleengine.model.SensorStressData;
import com.radicu.ruleengine.model.Spindle;
import com.radicu.ruleengine.model.SpindleData;
import com.radicu.ruleengine.service.RuleEngineServiceEC;
import com.radicu.ruleengine.service.SpindleReasonService;
import com.radicu.ruleengine.service.StressTestService;
import com.radicu.ruleengine.service.UniversalRuleExtractorService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;


@RestController
public class RuleController {

    private final SpindleReasonService ruleEngineService;
    private final RuleEngineServiceEC ruleEngineServiceEC;
    private final UniversalRuleExtractorService universalRuleExtractorService;
    private final StressTestService stressTestService;

    // Log for debugging
    private static final Logger logger = LoggerFactory.getLogger(RuleController.class);

    // Constructor Injection for both services
    public RuleController(SpindleReasonService ruleEngineService, UniversalRuleExtractorService universalRuleExtractorService
    ,RuleEngineServiceEC ruleEngineServiceEC, StressTestService stressTestService) {
        this.ruleEngineService = ruleEngineService;
        this.universalRuleExtractorService = universalRuleExtractorService;
        this.ruleEngineServiceEC = ruleEngineServiceEC;
        this.stressTestService = stressTestService;
    }
    @PostMapping(value = "/evaluate", produces = MediaType.APPLICATION_JSON_VALUE)
        public Spindle evaluateSpindleRules(@RequestBody Spindle spindle) {
        return ruleEngineService.evaluateRules(spindle);
    }

    // Inside the class:
    @GetMapping("/download-rule")
        public ResponseEntity<List<DrillRule>> downloadRules() {
        try {
            List<DrillRule> rules = ruleEngineService.getAllRules();
            logger.info("Successfully loaded {} rules", rules.size());
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            logger.error("Rule loading failed", e);  // Add detailed logging
            return ResponseEntity.internalServerError().build();
        }
    }

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

    //Endpoint to convert ttl-to-drl (for rule only, for now)
    @PostMapping("/api/converter/file-to-drl")
        public ResponseEntity<byte[]> convertFileToDrl(@RequestParam("file") MultipartFile file) throws IOException {
            // Save uploaded file temporarily
            File tempFile = File.createTempFile("uploaded-", file.getOriginalFilename());
            file.transferTo(tempFile);

            // Extract DRL
            String drlContent = universalRuleExtractorService.convertRdfToDrl(tempFile.getAbsolutePath());

            // Clean up temp file
            tempFile.delete();

            // Prepare response
            byte[] drlBytes = drlContent.getBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"converted_rules.drl\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(drlBytes);
        }

    @PostMapping("/run")
        public SpindleData runRules(@RequestBody SpindleData spindleData) {
            return ruleEngineServiceEC.runRules(spindleData);
        }

    @PostMapping("/stress-test")
        public SensorStressData run(@RequestBody SensorStressData sensorStressData) {
            return stressTestService.runStressTest(sensorStressData);
        }    

    
}