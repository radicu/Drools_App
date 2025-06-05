package com.radicu.ruleengine.service;

import com.radicu.ruleengine.model.SpindleData;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RuleEngineServiceEC {

    private final KieContainer spindleDataKieContainer;

    @Autowired
    public RuleEngineServiceEC(@Qualifier("spindleDataKieContainer") KieContainer spindleDataKieContainer) {
        this.spindleDataKieContainer = spindleDataKieContainer;
    }

    public SpindleData runRules(SpindleData spindleData) {
        KieSession kieSession = spindleDataKieContainer.newKieSession();
        try {
            kieSession.insert(spindleData);
             long startTime = System.nanoTime();
            kieSession.fireAllRules();
            long endTime = System.nanoTime();
            long elapsedMillis = (endTime - startTime) / 1_000_000;

            System.out.println("Reasoning time: {" + elapsedMillis + "}ms");
        } finally {
            kieSession.dispose();
        }
        return spindleData;
    }
}
