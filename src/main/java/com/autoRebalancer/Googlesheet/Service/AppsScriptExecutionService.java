package com.autoRebalancer.Googlesheet.Service;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.script.Script;
import com.google.api.services.script.model.ExecutionRequest;
import com.google.api.services.script.model.Operation;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class AppsScriptExecutionService {

    @Value("${googleSheetsapi.credentialsFilePath:}")
    private String CREDENTIALS_FILE_PATH;

    @Value("${googleSheetsapi.scriptId}")
    private String SCRIPT_ID;

    /**
     * Apps Script API용 서비스 빌더
     */
    private Script getScriptService() throws Exception {
        GoogleCredentials credentials;

        if (CREDENTIALS_FILE_PATH != null && !CREDENTIALS_FILE_PATH.isBlank()) {
            credentials = GoogleCredentials.fromStream(new FileInputStream(CREDENTIALS_FILE_PATH));
        } else {
            credentials = GoogleCredentials.getApplicationDefault();
        }

        List<String> scopes = Arrays.asList(
                "https://www.googleapis.com/auth/spreadsheets", // 기존 Sheets 스코프
                "https://www.googleapis.com/auth/script.projects" // Apps Script 실행 스코프
        );
        credentials = credentials.createScoped(scopes);

        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        return new Script.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("13fAutoRebalancer")
                .build();
    }

    /**
     * 지정된 Apps Script 함수를 원격으로 실행.
     * @param functionName Apps Script에 정의된 함수 이름
     */
    public void triggerSheetRefresh(String functionName) {
        log.info("[AppsScript] 원격 함수 실행 시도: {}", functionName);
        try {
            Script service = getScriptService();

            ExecutionRequest request = new ExecutionRequest()
                    .setFunction(functionName)
                    .setDevMode(false);

            // 스크립트 실행
            Operation op = service.scripts().run(SCRIPT_ID, request).execute();

            if (op.getError() != null) {
                String errorMessage = op.getError().getMessage();
                log.error("[AppsScript] 스크립트 실행 실패: {}", op.getError().getDetails());
                throw new RuntimeException("Apps Script 실행 오류: " + errorMessage);
            } else {
                log.warn("[AppsScript] 원격 함수 '{}'가 성공적으로 트리거되었습니다.", functionName);
            }

        } catch (GoogleJsonResponseException e) {
            log.error("[AppsScript] API 호출 실패. (GCP API 활성화 확인, 스크립트 ID 확인, 시트 공유 확인)", e);
            log.error("Error details: {}", e.getDetails());
            throw new RuntimeException("Apps Script API 호출 실패", e);
        } catch (Exception e) {
            log.error("[AppsScript] 알 수 없는 오류 발생", e);
            throw new RuntimeException("Apps Script 서비스 초기화 실패", e);
        }
    }
}

