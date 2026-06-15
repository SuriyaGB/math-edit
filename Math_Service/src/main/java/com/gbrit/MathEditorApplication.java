package com.gbrit;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
@SecurityScheme(description = "JWT Auth Description", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, name = "Authorization")
@OpenAPIDefinition(info = @Info(title = "Math APIS", version = "1.0", description = "MathEditor APIS."))
public class MathEditorApplication {
	public static void main(String[] args) {
		SpringApplication.run(MathEditorApplication.class, args);
	}

}
