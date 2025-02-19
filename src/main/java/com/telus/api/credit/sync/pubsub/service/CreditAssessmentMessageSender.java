package com.telus.api.credit.sync.pubsub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class CreditAssessmentMessageSender {

    public static final String TOPIC_NAME_PROPERTY_KEY = "${creditmgmt.pubsub.topicName}";
    private final PubSubTemplate pubSubTemplate;
    private final String topicName;

    public CreditAssessmentMessageSender(PubSubTemplate pubSubTemplate, @Value(TOPIC_NAME_PROPERTY_KEY) String topicName) {
        this.pubSubTemplate = pubSubTemplate;
        this.topicName = topicName;
    }

    public String publish(Object event) throws ExecutionException, InterruptedException {
        return pubSubTemplate.publish(topicName, event).get();
    }
}
