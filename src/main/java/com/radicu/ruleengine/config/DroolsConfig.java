package com.radicu.ruleengine.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.kie.internal.io.ResourceFactory;  // Add this import

@Configuration
public class DroolsConfig {


    @Bean(name = "stressTestContainer")
    public KieContainer stressTestContainer(){
        return buildContainer("rules/stress_test_rules.drl");
    }

    // After generate the new KG templates please uncomment these lines below
    @Bean(name = "variableKieContainer")
    public KieContainer variableKieContainer(){
        return buildContainer("rules/converted_rules.drl");
    }

    private KieContainer buildContainer(String drlFile) {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource(drlFile));
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        if (kieBuilder.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            System.out.println("### Drools Build Errors ###");
            kieBuilder.getResults().getMessages(org.kie.api.builder.Message.Level.ERROR)
                .forEach(msg -> System.out.println(msg.toString()));
            throw new IllegalStateException("Error building KieContainer for: " + drlFile);
        }
        KieModule kieModule = kieBuilder.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }
}
