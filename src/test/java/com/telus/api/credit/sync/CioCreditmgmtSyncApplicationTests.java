package com.telus.api.credit.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telus.api.credit.sync.firestore.data.CreditAssessmentRepository;
import com.telus.api.credit.sync.firestore.model.CreditAssessment;
import com.telus.api.credit.sync.pubsub.model.CreditAssessmentEvent;
import com.telus.api.credit.sync.pubsub.service.CreditAssessmentMessageSender;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SpringBootTest
class CioCreditmgmtSyncApplicationTests {

    private static final Log LOGGER = LogFactory.getLog(CioCreditmgmtSyncApplicationTests.class);
    private static final String TEST_DATA_CUSTOMER_ID = "10000";
    private static final String TEST_DATA_CREATED_BY = "TEST";
    private static final String TEST_DATA_EVENT_TYPE = "assessmentCreate";
    private static final Long TIMEOUT_SECONDS = 30L;
    private static final Long POLL_INTERVAL_SECONDS = 2L;

    @Autowired
    CreditAssessmentMessageSender sender;

    @Autowired
    CreditAssessmentRepository repository;

    @Test
    void contextLoads() {
    }

    @Test
    public void testCreditAssessmentRepositorySave() {
        try {
        //not a mock test, this test will update firestore collection
            repository.save(createCreditAssessment());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }   
    
    @Test
    public void verifyCreditAssessmentIntegration1() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            CreditAssessmentEvent creditAssessmentEvent = createCreditAssessmentEvent();
            String event = mapper.writeValueAsString(creditAssessmentEvent);
            sender.publish(event);
            LOGGER.info("Published CreditAssessmentEvent: " + event);
            findCustomerInFirestore(creditAssessmentEvent);
        } catch (Exception e) {
            Assertions.fail("Failed test case verifyCreditAssessmentIntegration1", e);
        }
    }

    @Test
    public void verifyCreditAssessmentIntegration2() {
        try {
            CreditAssessmentEvent creditAssessmentEvent = createCreditAssessmentEvent();
            CreditAssessment payloadAssessment = creditAssessmentEvent.getEvent().stream().findFirst().get();
            payloadAssessment.setCustomerId(TEST_DATA_CUSTOMER_ID);
            ObjectMapper mapper = new ObjectMapper();
            String event = mapper.writeValueAsString(creditAssessmentEvent);
            sender.publish(event);
            LOGGER.info("Published CreditAssessmentEvent: " + event);
        } catch (Exception e) {
            Assertions.fail("Failed test case verifyCreditAssessmentIntegration2", e);
        }
    }

    @Test
    public void verifyCreditAssessmentIntegration1Negative() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            CreditAssessmentEvent creditAssessmentEvent = createCreditAssessmentEvent();
            String event = mapper.writeValueAsString(creditAssessmentEvent);
            sender.publish(event);
            LOGGER.info("Published CreditAssessmentEvent: " + event);
            CreditAssessment assessment = creditAssessmentEvent.getEvent().stream().findFirst().get();
            assessment.setCustomerId("TEST_NEGATIVE");
            Assertions.assertThrows(Exception.class, () -> findCustomerInFirestore(creditAssessmentEvent));
        } catch (Exception e) {
            Assertions.fail("Failed test case verifyCreditAssessmentIntegration1", e);
        }
    }

    private void findCustomerInFirestore(CreditAssessmentEvent creditAssessmentEvent) {
        Awaitility.await()
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .pollInterval(Duration.ofSeconds(POLL_INTERVAL_SECONDS))
                .until(() -> {
                    CreditAssessment payloadAssessment = creditAssessmentEvent.getEvent().stream().findFirst().get();
                    CreditAssessment repositoryAssessment = repository.findByCustomerId(payloadAssessment.getCustomerId());
                    if (Objects.isNull(repositoryAssessment) || StringUtils.isBlank(repositoryAssessment.getCustomerId())) {
                        return false;
                    }
                    return payloadAssessment.equals(repositoryAssessment);
                });
    }

    private CreditAssessmentEvent createCreditAssessmentEvent() {
        CreditAssessmentEvent event = new CreditAssessmentEvent();
        
        CreditAssessment payload = createCreditAssessment();
        
        List<CreditAssessment> payloadList = new ArrayList<>(1);
        payloadList.add(payload);
        event.setEvent(payloadList);
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(TEST_DATA_EVENT_TYPE);
        //event.setEventTime(Instant.now().toString());
        event.setDescription(RandomStringUtils.randomAlphabetic(16, 16));
        return event;
    }

	private CreditAssessment createCreditAssessment() {
		CreditAssessment payload = new CreditAssessment();
        payload.setCreditAssessmentTypeCd("FULL_ASSESSMENT");
        payload.setCreditAssessmentSubTypeCd("CREDIT_CHECK");
        
		payload.setCustomerId("7000034");
        payload.setCreditProfileId("11111");
        payload.setCreditAssessmentId("2222");
        payload.setCreditAssessmentResultCd("asmtrsltcd01");
        payload.setAssessmentMessageCd("asmtmsgcd01");
        payload.setCreditAssessmentResultReasonCd("asmtrslrsncd01");
        payload.setCreditAssessmentTimestamp(Instant.now().toString());
        payload.setOriginatorAppId("orgappid");
        payload.setChannelOrgId("chnnlorgid");
        payload.setCreatedBy("userid01");
        payload.setCreatedTimestamp(Instant.now().toString());
        payload.setUpdatedBy("userid01");
        payload.setUpdatedTimestamp(Instant.now().toString());
		return payload;
	}
}
