package ${packageName};

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import com.radicu.ruleengine.model.Variable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ${serviceClassName} {

    private final KieContainer ${kieContainerName};

    @Autowired
    public ${serviceClassName}(@Qualifier("${kieContainerName}") KieContainer ${kieContainerName}) {
        this.${kieContainerName} = ${kieContainerName};
    }

    public ${modelClassName} runRules(${modelClassName} ${modelParamName}) {
        KieSession kieSession = ${kieContainerName}.newKieSession();
        try {
            kieSession.insert(${modelParamName});
            long startTime = System.nanoTime();
            kieSession.fireAllRules();
            long endTime = System.nanoTime();
            long elapsedMillis = (endTime - startTime) / 1_000_000;

            System.out.println("Reasoning time: {" + elapsedMillis + "}ms");
        } finally {
            kieSession.dispose();
        }
        return ${modelParamName};
    }
}
