package com.radicu.ruleengine.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.radicu.ruleengine.model.Threshold;
import com.radicu.ruleengine.model.ThresholdTimely;

@Service
public class MongoService {
    private final MongoTemplate mongoTemplate;

    public MongoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void updateThreshold(int spindleId, String ruleType, 
                            float accThreshold, float meanThreshold) {
        try {
            // Update current threshold
            Query query = new Query(Criteria.where("spindleId").is(spindleId)
                                .and("ruleType").is(ruleType));
            
            Update update = new Update()
                    .set("accThreshold", accThreshold)
                    .set("meanThreshold", meanThreshold);
            
            mongoTemplate.upsert(query, update, Threshold.class);
            
            // Log to time-series collection
            ThresholdTimely timelyRecord = new ThresholdTimely(
                spindleId, ruleType, accThreshold, meanThreshold
            );
            mongoTemplate.insert(timelyRecord);
            
            System.out.println("Updated max thresholds for Spindle_" + spindleId + 
                            " | " + ruleType + "_ACC: " + accThreshold + ", " + ruleType + "_Mean: " + meanThreshold);
        } catch (Exception e) {
            System.err.println("MongoDB update failed: " + e.getMessage());
            // Implement retry logic here
        }
    }

    
}