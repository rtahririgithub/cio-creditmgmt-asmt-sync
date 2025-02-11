package com.telus.api.credit.sync;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CioCreditmgmtSyncApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(CioCreditmgmtSyncApplication.class);

	public static void main(String[] args) {
		try {
			SpringApplication.run(CioCreditmgmtSyncApplication.class, args);
		} catch (Throwable e1) {
			e1.printStackTrace();
			throw e1;
		}

		Properties prop = new Properties();
		try {
			prop.load(CioCreditmgmtSyncApplication.class.getClassLoader().getResourceAsStream("git.properties"));
			LOGGER.info("Git information: {}", prop);
		} catch (Exception e) {
			LOGGER.debug("Couldn't load git information {}", e.getMessage());
		}
	}
}
