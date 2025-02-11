package com.telus.api.credit.sync.filters;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import com.telus.api.credit.sync.common.CommonConstants;

@Order(10)
@Component
public class CorrelationIdFilter implements Filter {
   private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
         throws IOException, ServletException {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      try {
         // Setup MDC data:
         // Get correlationId
         String correlationId = httpRequest.getHeader(CommonConstants.HEADER_CORR_ID);
         if (StringUtils.isBlank(correlationId)) {
            correlationId = UUID.randomUUID().toString().toLowerCase();
         } else {
            correlationId = HtmlUtils.htmlEscape(correlationId);
         }

         // Kong auto-injects Kong-Request-ID to trace calls too
         String kongRequestId = httpRequest.getHeader("Kong-Request-ID");
         if (!StringUtils.isBlank(kongRequestId)) {
            MDC.put("kongRequestId", kongRequestId);
         }
         
         MDC.put("correlationId", correlationId);
         MDC.put("containerId", InetAddress.getLocalHost().getHostName());

         logger.trace("CorrelationIdFilter.doFilter() called for:{} ", httpRequest.getRequestURI());

         httpRequest.setAttribute(CommonConstants.HEADER_CORR_ID, correlationId);
         httpResponse.addHeader(CommonConstants.HEADER_CORR_ID, correlationId);
         
         try {
        	 chain.doFilter(httpRequest, httpResponse);
 		} catch (Throwable e) {
 			logger.warn("doFilter failed.",e);
 		}          
      } finally {
         // Tear down MDC data:
         MDC.clear();
      }
   }

   @Override
   public void destroy() {
      // Destroy is not required
   }

   @Override
   public void init(FilterConfig arg0) {
      // Initialization is not required
   }

}