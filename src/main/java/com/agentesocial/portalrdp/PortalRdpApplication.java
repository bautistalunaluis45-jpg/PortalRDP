package com.agentesocial.portalrdp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PortalRdpApplication {

	public static void main(String[] args) {
		SpringApplication.run(PortalRdpApplication.class, args);
	}

}
