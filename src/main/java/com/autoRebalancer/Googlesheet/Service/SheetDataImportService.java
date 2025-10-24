package com.autoRebalancer.Googlesheet.Service;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.util.List;

@Service
@Slf4j
public class SheetDataImportService {

    @Value("${googleSheetsapi.credentialsFilePath:}")
    private String CREDENTIALS_FILE_PATH;
    @Value("${googleSheetsapi.spreadsheetId}")
    private String SHEET_ID;
    @Value("${googleSheetsapi.spreadsheetrange}")
    private String SHEET_RANGE;

    public Sheets getSheets() throws Exception {

        GoogleCredentials credentials;

        if (CREDENTIALS_FILE_PATH != null && !CREDENTIALS_FILE_PATH.isBlank()) {
            credentials = GoogleCredentials.fromStream(new FileInputStream(CREDENTIALS_FILE_PATH));
        } else {
            credentials = GoogleCredentials.getApplicationDefault();
        }
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        return new Sheets.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                requestInitializer
        ).setApplicationName("Portfolio Rebalancer")
                .build();
    }

    public List<List<Object>> getSheetsData() throws Exception {
        String spreadsheetId = SHEET_ID;
        String range = SHEET_RANGE;

        Sheets service = getSheets();
        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        return response.getValues();
    }

}
