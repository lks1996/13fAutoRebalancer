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
    @Value("${googleSheetsapi.cikCellRange}")
    private String CIK_CELL_RANGE;

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

    public String getActiveMonitoredCik() throws Exception {
        String spreadsheetId = SHEET_ID;
        String range = CIK_CELL_RANGE; // 홀딩스 범위가 아닌 CIK 셀 범위 사용

        try {
            Sheets service = getSheets();
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();

            List<List<Object>> values = response.getValues();

            if (values == null || values.isEmpty()) {
                log.warn("[WARN] CIK_CELL_RANGE ({})에 값이 비어있습니다. CIK 갱신을 건너뜁니다.", range);
                return null;
            }

            List<Object> row = values.get(0);
            if (row == null || row.isEmpty() || row.get(0) == null) {
                log.warn("[WARN] CIK_CELL_RANGE ({})의 첫 번째 셀이 비어있습니다. CIK 갱신을 건너뜁니다.", range);
                return null;
            }

            String cik = row.get(0).toString().trim();
            if (cik.isEmpty()) {
                log.warn("[WARN] CIK_CELL_RANGE ({})의 값이 공백입니다. CIK 갱신을 건너뜁니다.", range);
                return null;
            }

            log.info("[INFO] 구글 시트에서 모니터링 중인 CIK를 확인했습니다: {}", cik);
            return cik;

        } catch (Exception e) {
            log.error("[ERROR] 구글 시트에서 CIK 값을 가져오는 중 오류 발생. Range: {}", range, e);
            throw e; // 오류 전파
        }
    }
}
