package com.radicu.ruleengine.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StreamUtils;

import com.radicu.ruleengine.model.DrillRule;
import com.radicu.ruleengine.model.Spindle;
import com.radicu.ruleengine.service.SpindleReasonService;
import com.radicu.ruleengine.service.TtlToDroolsConverterService;

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
    private final TtlToDroolsConverterService ttlToDroolsConverterService;

    // Log for debugging
    private static final Logger logger = LoggerFactory.getLogger(RuleController.class);

    // Constructor Injection for both services
    public RuleController(SpindleReasonService ruleEngineService, TtlToDroolsConverterService ttlToDroolsConverterService) {
        this.ruleEngineService = ruleEngineService;
        this.ttlToDroolsConverterService = ttlToDroolsConverterService;
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
    @PostMapping("/api/converter/ttl-to-drl")
        public ResponseEntity<InputStreamResource> convertTtlToDrl(@RequestParam("file") MultipartFile file) throws IOException {
            logger.info("Received .ttl file for conversion: {}", file.getOriginalFilename());

            // Save MultipartFile to a temporary file
            File tempFile = File.createTempFile("uploaded", ".ttl");
            try (OutputStream os = new FileOutputStream(tempFile)) {
                file.getInputStream().transferTo(os);
            }

            // Call the converter service
            String drlContent = ttlToDroolsConverterService.convertTtlToDrl(tempFile.getAbsolutePath());

            // Prepare .drl file as response
            ByteArrayInputStream drlStream = new ByteArrayInputStream(drlContent.getBytes());

            InputStreamResource resource = new InputStreamResource(drlStream);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rules.drl\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }

    
}