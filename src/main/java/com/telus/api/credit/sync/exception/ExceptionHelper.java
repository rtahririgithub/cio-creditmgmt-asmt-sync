package com.telus.api.credit.sync.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ExceptionHelper {

   private ExceptionHelper() {
      // utils
   }

   public static ResponseEntity<ErrorResponse> createErrorResponse(CreditException ce) {
      ErrorResponse er = new ErrorResponse();
      er.setCode(ce.getCode());
      er.setReason(ce.getReason());
      er.setMessage(ce.getMessage());
      HttpStatus httpStatus = ce.getHttpStatus();
      return new ResponseEntity<>(er, httpStatus);
   }
   
   public static   String removeBrkLine(String str) {
		if(str!=null && !str.isEmpty()){
			try{
				str = str.replaceAll("\\r\\n|\\r|\\n", " ");
			}catch (Throwable e){}
		}
		return str;
	}

   public static   String leadingTrailingEscapeChar( String str) {
		if(str!=null && !str.isEmpty()){
			try{
			   str = str.startsWith("\"") ? str.substring(1) : str;
			   str = str.endsWith("\"") ? str.substring(0,str.length()-1) : str;
			}catch (Throwable e){}
		}
		return str;
   }
   
	public static String getStackTrace(Throwable t) {

		String stckTraceStr ="[ StackTrace : ";  
		try{
			if(t!= null){
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw, true);
				t.printStackTrace(pw);
				pw.flush();
				sw.flush();
				stckTraceStr =stckTraceStr + sw.toString();
				stckTraceStr=removeBrkLine(stckTraceStr);
				stckTraceStr=leadingTrailingEscapeChar(stckTraceStr);		
			}
		}catch (Throwable e){}
		stckTraceStr =stckTraceStr +"EndOfStackTrace]";
		return stckTraceStr;
	}   
}
