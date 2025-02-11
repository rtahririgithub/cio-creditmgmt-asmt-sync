package com.telus.api.credit.sync.exception;

import org.springframework.http.HttpStatus;

public class CreditException extends RuntimeException {
	private static final long serialVersionUID = -744690390571315712L;

	private HttpStatus httpStatus;
	private String code;     // Mandatory
	private String reason;   // Mandatory. Text that explains the reason for error. This can be shown to a client user.
	private String message;  // Additional info
	
	public CreditException(HttpStatus httpStatus, String code, String reason, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.reason = reason;
		this.message = message;
	}

   public HttpStatus getHttpStatus() {
      return httpStatus;
   }

   public void setHttpStatus(HttpStatus httpStatus) {
      this.httpStatus = httpStatus;
   }

   public String getCode() {
      return code;
   }

   public void setCode(String code) {
      this.code = code;
   }

   public String getReason() {
      return reason;
   }

   public void setReason(String reason) {
      this.reason = reason;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

}