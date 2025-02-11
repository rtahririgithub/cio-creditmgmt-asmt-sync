package com.telus.api.credit.sync.pubsub.service;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telus.api.credit.sync.exception.ExceptionConstants;
import com.telus.api.credit.sync.firestore.data.CreditAssessmentRepository;
import com.telus.api.credit.sync.firestore.model.CreditAssessment;
import com.telus.api.credit.sync.pubsub.model.CreditAssessmentEvent;

@Service
public class CreditAssessmentMessageReceiver {

    private static final Log LOGGER = LogFactory.getLog(CreditAssessmentMessageReceiver.class);

    private final CreditAssessmentRepository creditAssessmentRepository;
    private final JacksonPubSubMessageConverter messageConverter;

    public CreditAssessmentMessageReceiver(CreditAssessmentRepository creditAssessmentRepository, ObjectMapper objectMapper) {
        this.creditAssessmentRepository = creditAssessmentRepository;
        this.messageConverter = new JacksonPubSubMessageConverter(objectMapper);
    }

    @ServiceActivator(inputChannel = "pubSubInputChannel")
    public void messageReceiver(@Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {
        try {
            CreditAssessmentEvent event = toCreditAssessmentEvent(message);
            if (validate(event, message)) {
                processMessage(event, message);
            }
            ackMessage(message.ack(), message.getPubsubMessage().getMessageId());
        } catch (Exception e) {
            LOGGER.error(
            			ExceptionConstants.STACKDRIVER_METRIC + ":" +
            			ExceptionConstants.PUBSUB100 + " Exception processing pubsub message. Message will be retried. " + 
            			" PubsubMessageId=" + message.getPubsubMessage().getMessageId() +
            			" PubsubMessage=" + message.getPubsubMessage()+
            			 com.telus.api.credit.sync.exception.ExceptionHelper.getStackTrace(e));
            
            ackMessage(message.nack(), message.getPubsubMessage().getMessageId());
        }
    }

    public CreditAssessmentEvent toCreditAssessmentEvent(BasicAcknowledgeablePubsubMessage message) {
        try {
            CreditAssessmentEvent event = messageConverter.fromPubSubMessage(message.getPubsubMessage(), CreditAssessmentEvent.class);
            return event;
        } catch (Exception e) {
            LOGGER.error(
            			ExceptionConstants.STACKDRIVER_METRIC + ":" +ExceptionConstants.PUBSUB101 + 
            			" Invalid pubsub message type." + 
            			" messageId=" + message.getPubsubMessage().getMessageId()
            			+ com.telus.api.credit.sync.exception.ExceptionHelper.getStackTrace(e));
            
            ackMessage(message.nack(), message.getPubsubMessage().getMessageId());
        }
        return null;
    }

    private Boolean validate(CreditAssessmentEvent event, BasicAcknowledgeablePubsubMessage message) {
        Boolean status = Boolean.TRUE;
        if (Objects.isNull(event) || Objects.isNull(event.getEvent()) || event.getEvent().isEmpty()) {
            status = Boolean.FALSE;
        }
        if (status) {
            for (CreditAssessment e : event.getEvent()) {
                if (Objects.isNull(e) || Objects.isNull(e.getCustomerId())) {
                    status = Boolean.FALSE;
                    break;
                }
            }
        }
        if (!status) {
            LOGGER.error(ExceptionConstants.STACKDRIVER_METRIC + ":" +ExceptionConstants.PUBSUB102 + " Validation error. Invalid pubsub message data. messageId=" + message.getPubsubMessage().getMessageId() + ", event=" + event);
            ackMessage(message.nack(), message.getPubsubMessage().getMessageId());
        }
        return status;
    }

    private void processMessage(CreditAssessmentEvent event, BasicAcknowledgeablePubsubMessage message) throws ExecutionException, InterruptedException {
        for (CreditAssessment payload : event.getEvent()) {
            LOGGER.info("CustId=" + payload.getCustomerId() +
            		"Start CreditAssessmentMessageReceiver.messageReceive" +
            		"MessageId=" + message.getPubsubMessage().getMessageId() + 
                    ", publishTime=" + message.getPubsubMessage().getPublishTime().getSeconds() +
            		",Message=" + payload 
                    );

            creditAssessmentRepository.save(payload);
        }
    }

    private void ackMessage(ListenableFuture<Void> future, String messageId) {
        try {
            future.get();
        } catch (Exception e) {
            LOGGER.error(ExceptionConstants.STACKDRIVER_METRIC + ":" +ExceptionConstants.PUBSUB103 + " Exception acknowledging pubsub message. messageId=" + messageId);
        }
    }
}
