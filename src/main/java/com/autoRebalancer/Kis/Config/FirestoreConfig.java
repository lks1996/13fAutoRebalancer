package com.autoRebalancer.Kis.Config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirestoreConfig {

    @Value("${googleSheetsapi.credentialsFilePath:}")
    private String CREDENTIALS_FILE_PATH;
    @Value("${google.projectid}")
    private String GOOGLE_PROJECT_ID;

    @Bean
    public Firestore firestore() throws IOException {

        GoogleCredentials credentials;

        if (CREDENTIALS_FILE_PATH != null && !CREDENTIALS_FILE_PATH.isBlank()) {
            credentials = GoogleCredentials.fromStream(new FileInputStream(CREDENTIALS_FILE_PATH));
        } else {
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(GOOGLE_PROJECT_ID)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }

        return FirestoreClient.getFirestore();
    }
}
