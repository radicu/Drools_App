package com.radicu.ruleengine.service;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import com.radicu.ruleengine.model.Variable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RuleEngineServiceVariable {

    private final KieContainer variableKieContainer;

    @Autowired
    public RuleEngineServiceVariable(@Qualifier("variableKieContainer") KieContainer variableKieContainer) {
        this.variableKieContainer = variableKieContainer;
    }

    public Variable runRules(Variable variable) {
        KieSession kieSession = variableKieContainer.newKieSession();
        try {
            kieSession.insert(variable);
            long startTime = System.nanoTime();
            kieSession.fireAllRules();
            long endTime = System.nanoTime();
            long elapsedMillis = (endTime - startTime) / 1_000_000;

            System.out.println("Reasoning time: {" + elapsedMillis + "}ms");
        } finally {
            kieSession.dispose();
        }
        return variable;
    }
}
