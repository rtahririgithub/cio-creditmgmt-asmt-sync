package com.telus.api.credit.sync.api;

import static com.telus.api.credit.sync.common.CommonConstants.HEADER_CORR_ID;
import static com.telus.api.credit.sync.exception.ExceptionConstants.ERR_CODE_1000;
import static com.telus.api.credit.sync.exception.ExceptionConstants.ERR_CODE_1000_MSG;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telus.api.credit.sync.exception.CreditException;
import com.telus.api.credit.sync.exception.ExceptionConstants;
import com.telus.api.credit.sync.firestore.model.CreditAssessment;
import com.telus.api.credit.sync.pubsub.model.CreditAssessmentEvent;
import com.telus.api.credit.sync.pubsub.service.CreditAssessmentMessageSender;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@Api(tags = {"Customer Credit Profile Assessment Change Listener"}, produces = "application/json")
@RequestMapping(path = "/v1/customer/listener/assessmentChangeEvent", produces = "application/json")
@Validated
public class AssessmentChangeListenerAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssessmentChangeListenerAPI.class);

    @Autowired
    private CreditAssessmentMessageSender messageSender;

    @PatchMapping
    @ApiOperation(value = "Receive and publish assessment message to topic")
    public ResponseEntity<Object> newAssessmentMessage(HttpServletRequest request,
                                               @ApiParam(value = "Event", required = true) @RequestBody CreditAssessmentEvent event) {

        boolean status = !(Objects.isNull(event) || Objects.isNull(event.getEvent()) || event.getEvent().isEmpty());
        if (status) {
            for (CreditAssessment e : event.getEvent()) {
                if (Objects.isNull(e) || Objects.isNull(e.getCustomerId())) {
                    status = false;
                    break;
                }
            }
        }

        if (!status) {
            LOGGER.error("{}:Bad data {}", ExceptionConstants.STACKDRIVER_METRIC ,event);
            throw new CreditException(HttpStatus.BAD_REQUEST, ERR_CODE_1000, "Customer info is missing", ERR_CODE_1000_MSG);
        }

        HttpStatus retCode = HttpStatus.OK;
        try {
            messageSender.publish(event);
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error("{}: {} Error publishing {}", ExceptionConstants.STACKDRIVER_METRIC, ExceptionConstants.PUBSUB200, event, e);
            retCode = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity.status(retCode).body(Collections.singletonMap(HEADER_CORR_ID, request.getAttribute(HEADER_CORR_ID)));
    }
}
