package com.radicu.ruleengine.service;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import com.radicu.ruleengine.model.Variable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RuleEngineServiceVariable2 {
    private final KieContainer variableKieContainer2;

    @Autowired
    public RuleEngineServiceVariable2(@Qualifier("variableKieContainer2") KieContainer variableKieContainer2) {
        this.variableKieContainer2 = variableKieContainer2;
    }

    public Variable runRules2(Variable variable) {
        KieSession kieSession = variableKieContainer2.newKieSession();
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
