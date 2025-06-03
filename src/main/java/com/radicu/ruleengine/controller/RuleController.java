package com.radicu.ruleengine.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.radicu.ruleengine.model.DrillRule;
import com.radicu.ruleengine.model.Spindle;
import com.radicu.ruleengine.service.SpindleReasonService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
public class RuleController {

    private final SpindleReasonService ruleEngineService;

    //Log for debugging
    private static final Logger logger = LoggerFactory.getLogger(RuleController.class);

    public RuleController(SpindleReasonService ruleEngineService) {
        this.ruleEngineService = ruleEngineService;
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

    
}