package com.autoRebalancer.Kis.Config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirestoreConfig {

    @Value("${googleSheetsapi.credentialsFilePath:#{null}}")
    private String CREDENTIALS_FILE_PATH;
    @Value("${googleSheetsapi.credentials-json:#{null}}")
    private String CREDENTIALS_JSON;
    @Value("${google.projectid}")
    private String GOOGLE_PROJECT_ID;

    @Bean
    public Firestore firestore() throws IOException {

        GoogleCredentials credentials;

        // AWS 람다 환경 (JSON 문자열이 환경변수로 주입된 경우)
        if (CREDENTIALS_JSON != null && !CREDENTIALS_JSON.isBlank()) {
            InputStream stream = new ByteArrayInputStream(CREDENTIALS_JSON.getBytes(StandardCharsets.UTF_8));
            credentials = GoogleCredentials.fromStream(stream);
        }
        // 윈도우 환경 (볼륨 마운트된 파일 경로가 있는 경우)
        else if (CREDENTIALS_FILE_PATH != null && !CREDENTIALS_FILE_PATH.isBlank()) {
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
