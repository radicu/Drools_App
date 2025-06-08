package com.radicu.ruleengine.service;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.radicu.ruleengine.model.RuleStructure;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RulesExcel {




    private static final String RULES_PATH = "rules/spindle_rules.drl";
    
    private static final String RULES_PATH2= "src/main/resources/rules/spindle_rules.drl";

    //Log for debugging
    private static final Logger logger = LoggerFactory.getLogger(RulesExcel.class);



    public List<RuleStructure> getAllRules() throws IOException {
        ClassPathResource resource = new ClassPathResource(RULES_PATH);
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return parseRules(content);
    }

    private List<RuleStructure> parseRules(String content) {
        List<RuleStructure> rules = new ArrayList<>();
        logger.debug("Starting rule parsing. Content length: {} characters", content.length());
        
        // Split content into individual rule blocks
        String[] ruleBlocks = content.split("(?=rule\\s+\")");
        logger.debug("Found {} potential rule blocks after split", ruleBlocks.length);
        
        int ruleCount = 0;
        for (String block : ruleBlocks) {
            if (!block.trim().startsWith("rule")) {
                logger.trace("Skipping non-rule block: {}", block.substring(0, Math.min(50, block.length())));
                continue;
            }
            
            try {
                ruleCount++;
                logger.debug("Processing rule block #{}", ruleCount);
                
                RuleStructure rule = parseSingleRule(block);
                if (rule != null) {
                    rules.add(rule);
                    logger.info("Parsed rule: '{}' (salience: {})", 
                                rule.getRuleName(), rule.getSalience());
                }
            } catch (Exception e) {
                logger.error("Error parsing rule block #{}: {}", ruleCount, e.getMessage());
                logger.debug("Problematic rule content:\n{}", block);
            }
        }
        
        logger.info("Successfully parsed {}/{} rules", rules.size(), ruleCount);
        return rules;
    }
    
    private RuleStructure parseSingleRule(String block) {
        // Rule name extraction
        Matcher nameMatcher = Pattern.compile("rule\\s+\"([^\"]+)\"").matcher(block);
        if (!nameMatcher.find()) {
            logger.warn("Rule name not found in block");
            return null;
        }
        String ruleName = nameMatcher.group(1);
        
        // Salience extraction
        Integer salience = null;
        Matcher salienceMatcher = Pattern.compile("salience\\s+(\\d+)").matcher(block);
        if (salienceMatcher.find()) {
            try {
                salience = Integer.parseInt(salienceMatcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Invalid salience value: {}", salienceMatcher.group(1));
            }
        }
        
        // Conditions extraction
        Matcher whenMatcher = Pattern.compile("when\\s+(.*?)\\s+then", Pattern.DOTALL).matcher(block);
        if (!whenMatcher.find()) {
            logger.warn("'when' section not found for rule: {}", ruleName);
            return null;
        }
        String conditions = whenMatcher.group(1).trim();
        
        // Actions extraction
        Matcher thenMatcher = Pattern.compile("then\\s+(.*?)\\s+end", Pattern.DOTALL).matcher(block);
        if (!thenMatcher.find()) {
            logger.warn("'then' section not found for rule: {}", ruleName);
            return null;
        }
        String actions = thenMatcher.group(1).trim();
        
        // Clean up whitespace
        conditions = conditions.replaceAll("\\s+", "").replaceAll("\\$spindle\\s*:", "Spindle:");
        actions = actions.replaceAll("\\s+", " ").replaceAll("\\$spindle\\.", "this.");
        
        return new RuleStructure(ruleName, salience, conditions, actions);
    }


    public String parseRulesToDRL(List<Map<String, Object>> rulesList) {
        StringBuilder drlBuilder = new StringBuilder();
        drlBuilder.append("package rules;\n\n");
        drlBuilder.append("import com.jskool.ruleengine.model.Spindle;\n\n");

        for (Map<String, Object> rule : rulesList) {
            // Extract rule properties
            String ruleName = (String) rule.get("ruleName");
            Integer salience = (Integer) rule.get("salience"); // Using Integer to handle null
            String conditions = (String) rule.get("conditions");
            String actions = (String) rule.get("actions");

            // Build rule header
            drlBuilder.append("rule \"").append(ruleName).append("\"\n");
            
            // Add salience if present
            if (salience != null) {
                drlBuilder.append("    salience ").append(salience).append("\n");
            }

            // Process conditions
            drlBuilder.append("when\n");
            if (conditions.startsWith("Spindle:Spindle(")) {
                String innerConditions = conditions.substring("Spindle:Spindle(".length(), conditions.length() - 1);
                String[] conditionParts = innerConditions.split(",");
                drlBuilder.append("    $spindle : Spindle(\n");
                for (int i = 0; i < conditionParts.length; i++) {
                    drlBuilder.append("        ").append(conditionParts[i].trim());
                    if (i < conditionParts.length - 1) drlBuilder.append(",");
                    drlBuilder.append("\n");
                }
                drlBuilder.append("    )\n");
            } else {
                drlBuilder.append("    ").append(conditions.replace("Spindle:Spindle", "$spindle : Spindle")).append("\n");
            }

            // Process actions
            drlBuilder.append("then\n");
            String processedActions = actions.replace("this.", "$spindle.");
            String[] actionStatements = processedActions.split(";");
            for (String action : actionStatements) {
                if (!action.trim().isEmpty()) {
                    drlBuilder.append("    ").append(action.trim()).append(";\n");
                }
            }
            drlBuilder.append("end\n\n");
        }

        return drlBuilder.toString();
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