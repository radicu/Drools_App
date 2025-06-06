package com.radicu.ruleengine.service;

import org.springframework.stereotype.Service;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.radicu.ruleengine.model.SensorStressData;

@Service
public class StressTestService {

    private final KieContainer stressTestContainer;

    @Autowired
    public StressTestService(@Qualifier("stressTestContainer") KieContainer stressTestContainer) {
        this.stressTestContainer = stressTestContainer;
    }

    public SensorStressData runStressTest(SensorStressData stressTestData) {
        KieSession kieSession = stressTestContainer.newKieSession();
        try {
            kieSession.insert(stressTestData);
             long startTime = System.nanoTime();
            kieSession.fireAllRules();
            long endTime = System.nanoTime();
            long elapsedMillis = (endTime - startTime) / 1_000_000;

            System.out.println("Reasoning time: {" + elapsedMillis + "}ms");
        } finally {
            kieSession.dispose();
        }
        return stressTestData;
    }
}
