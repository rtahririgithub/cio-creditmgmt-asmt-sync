package com.telus.api.credit.sync.firestore.data;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.telus.api.credit.sync.firestore.model.CreditAssessment;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CreditAssessmentRepository {

    public static final String COLLECTION_NAME_PROPERTY_KEY = "${creditmgmt.firestore.collectionName}";
    public static final String CUSTOMER_ID_FIELD = "customerId";

    private final Firestore firestore;
    private final String collectionName;

    public CreditAssessmentRepository(Firestore firestore, @Value(COLLECTION_NAME_PROPERTY_KEY) String collectionName) {
        this.firestore = firestore;
        this.collectionName = collectionName;
    }

    public CreditAssessment findByCustomerId(String customerId) throws ExecutionException, InterruptedException {
        CreditAssessment creditAssessment = null;
        CollectionReference collection = firestore.collection(collectionName);
        QuerySnapshot result = collection.whereEqualTo(CUSTOMER_ID_FIELD, customerId).limit(1).get().get();
        for (QueryDocumentSnapshot document : result.getDocuments()) {
            creditAssessment = document.toObject(CreditAssessment.class);
        }
        return creditAssessment;
    }
    public ArrayList<CreditAssessment> findCreditAssessmentListByCustomerId(String customerId) throws ExecutionException, InterruptedException {       
        ArrayList<CreditAssessment> creditAssessmentList= new ArrayList<CreditAssessment>();       
        CollectionReference collection = firestore.collection(collectionName);
        QuerySnapshot result = collection.whereEqualTo(CUSTOMER_ID_FIELD, customerId).get().get();
        for (QueryDocumentSnapshot document : result.getDocuments()) {
        	CreditAssessment creditAssessment  = document.toObject(CreditAssessment.class);
        	creditAssessment.setDocumentId(document.getId());
        	populateLOB(creditAssessment);
            creditAssessmentList.add(creditAssessment);
        }
        return creditAssessmentList;
    }
    public String findDocumentIdByCustomerId(String customerId) throws ExecutionException, InterruptedException {
        String documentId = null;
        CollectionReference collection = firestore.collection(collectionName);
        QuerySnapshot result = collection.whereEqualTo(CUSTOMER_ID_FIELD, customerId).select(FieldPath.documentId()).limit(1).get().get();
        for (QueryDocumentSnapshot document : result.getDocuments()) {
             documentId = document.getId();
        }
        return documentId;
    }

    public Timestamp save(CreditAssessment inputCreditAssesmentDoc) throws ExecutionException, InterruptedException {
    	
	    populateLOB(inputCreditAssesmentDoc);
	    
	    Timestamp updatedAt = null;
	   
	    //get list of existing creditassessments
	    ArrayList<CreditAssessment> existingCreditAssessmentList = findCreditAssessmentListByCustomerId(inputCreditAssesmentDoc.getCustomerId());
	    //populateLOB
	    for (CreditAssessment existingCreditAssessment : existingCreditAssessmentList) {
	    	populateLOB(existingCreditAssessment);
		}
	    
	    //if inputCreditAssesmentDoc exist update it otherwise add it
	    Optional<CreditAssessment> matchingCreditAssessmentOptional = existingCreditAssessmentList.stream()
	            .filter(assessment -> inputCreditAssesmentDoc.getLineOfBusiness().equalsIgnoreCase(assessment.getLineOfBusiness()))
	            .findFirst();
	    if (matchingCreditAssessmentOptional.isPresent()) {
	        CreditAssessment matchingCreditAssessment = matchingCreditAssessmentOptional.get();
            inputCreditAssesmentDoc.setId(matchingCreditAssessment.getDocumentId());
	    	updatedAt = firestore.collection(collectionName).document(matchingCreditAssessment.getDocumentId()).set(inputCreditAssesmentDoc).get().getUpdateTime();	    		
	        System.out.println("Found matching CreditAssessment");
	    } else {
	    	updatedAt = firestore.collection(collectionName).add(inputCreditAssesmentDoc).get().get().get().getUpdateTime();	    	
	        System.out.println("No matching CreditAssessment ");
	    }
	    
	    //update lineofbusiness in non matching existingCreditAssesmentDoc
	    Optional<CreditAssessment> nonmatchingCreditAssessmentOptional = existingCreditAssessmentList.stream()
	            .filter(assessment -> !inputCreditAssesmentDoc.getLineOfBusiness().equalsIgnoreCase(assessment.getLineOfBusiness()))
	            .findFirst();	    
	    if (nonmatchingCreditAssessmentOptional.isPresent()) {
	        CreditAssessment nonmatchingCreditAssessment = nonmatchingCreditAssessmentOptional.get();
	        	populateLOB(nonmatchingCreditAssessmentOptional.get());
	        	updatedAt = firestore.collection(collectionName).document(nonmatchingCreditAssessment.getDocumentId()).set(nonmatchingCreditAssessmentOptional.get()).get().getUpdateTime();	  		    	 
	    } 

        return updatedAt;
    }

    public Timestamp saveOrig(CreditAssessment creditAssessment) throws ExecutionException, InterruptedException {
        String documentId = findDocumentIdByCustomerId(creditAssessment.getCustomerId());
        CreditAssessment newDoc = creditAssessment;
        Timestamp updatedAt = null;
        if (Objects.isNull(documentId)) {
            updatedAt = firestore.collection(collectionName).add(newDoc).get().get().get().getUpdateTime();
        } else {
            newDoc.setId(documentId);
            updatedAt = firestore.collection(collectionName).document(documentId).set(newDoc).get().getUpdateTime();
        }
        return updatedAt;
    }
    
	private void populateLOB(CreditAssessment aDoc) {
        List<String> wlsCreditAssessmentSubTypeCdlist = new ArrayList<>(Arrays.asList(
        		"CREDIT_CHECK",
        		"MANUAL_ASSESSMENT",
                "CREDIT_RESULT_OVRD",
                "MONTHLY_CCUD",
                "ADDON",
                "ADDON_ESTIMATOR",
                "ADDON_RESUME"
        ));		
		String creditAssessmentSubTypeCd = aDoc.getCreditAssessmentSubTypeCd();
		if(creditAssessmentSubTypeCd!=null) {
			String upperCaseStr = creditAssessmentSubTypeCd.toUpperCase();
			if (wlsCreditAssessmentSubTypeCdlist.contains(upperCaseStr)) {
				aDoc.setLineOfBusiness("WIRELESS");
			}else {
				aDoc.setLineOfBusiness("WIRELINE");
			}
		}
	}
}
