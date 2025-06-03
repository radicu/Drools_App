package com.radicu.ruleengine.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrillRule {
    private String ruleName;
    private Integer salience;
    private String conditions;
    private String actions;
    private String formattedRule;  // Add this new field

    // Update constructor
    public DrillRule(String ruleName, Integer salience, 
                    String conditions, String actions) {
        this.ruleName = ruleName;
        this.salience = salience;
        this.conditions = conditions;
        this.actions = actions;
        this.formattedRule = formatRule();
    }

    private String formatRule() {
        return String.format("Rule: %s\nWhen: %s\nThen: %s",
                ruleName, conditions, actions);
    }
}