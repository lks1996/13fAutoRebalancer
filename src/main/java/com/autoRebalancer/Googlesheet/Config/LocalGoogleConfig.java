package com.autoRebalancer.Googlesheet.Config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class LocalGoogleConfig {

    @Value("${googleSheetsapi.credentialsFilePath}")
    private String credentialsPath;

    @PostConstruct
    public void setUp() {
        System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", credentialsPath);
        System.out.println("GOOGLE_APPLICATION_CREDENTIALS set to " + credentialsPath);
    }
}

