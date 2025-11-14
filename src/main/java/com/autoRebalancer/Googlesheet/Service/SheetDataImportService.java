package com.autoRebalancer.Googlesheet.Service;

import com.autoRebalancer._13f.Dto.Filer;
import com.autoRebalancer._13f.Dto.PortfolioHolding;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SheetDataImportService {

    @Value("${googleSheetsapi.credentialsFilePath:}")
    private String CREDENTIALS_FILE_PATH;
    @Value("${googleSheetsapi.spreadsheetId}")
    private String SHEET_ID;
    @Value("${googleSheetsapi.filerSheetName:FilerData}")
    private String FILER_SHEET_NAME;
    @Value("${googleSheetsapi.filersheetrange}")
    private String FILER_SHEET_RANGE;
    @Value("${googleSheetsapi.dashboardSheetName:13F-Data-dev}")
    private String DASHBOARD_SHEET_NAME;
    @Value("${googleSheetsapi.dashboardsheetrange}")
    private String DASHBOARD_SHEET_RANGE;
    @Value("${googleSheetsapi.cikCellRange}")
    private String CIK_CELL_RANGE;

    /**
     * 구글시트 서비스 빌더.
     */
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

    /**
     * 특정 범위의 셀 내용 가져오기.
     */
    public List<List<Object>> getData(String spreadsheetId, String range) throws Exception {

        Sheets service = getSheets();
        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        return response.getValues();
    }

    /**
     * 특정 범위의 셀 내용 삭제.
     */
    private void clearSheetRange(String range) throws Exception {
        Sheets service = getSheets();
        ClearValuesRequest clearRequest = new ClearValuesRequest();

        service.spreadsheets().values()
                .clear(SHEET_ID, range, clearRequest)
                .execute();
    }

    /**
     * 특정 범위에 데이터를 쓰기.
     */
    private void writeData(String range, List<List<Object>> values) throws Exception {
        Sheets service = getSheets();
        ValueRange body = new ValueRange().setValues(values);

        service.spreadsheets().values()
                .update(SHEET_ID, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    /**
     * 특정 시트의 열 서식을 텍스트(@)로 지정.
     */
    private void setColumnFormatAsText(String sheetName, int startColumnZeroIndexed, int endColumnZeroIndexed) throws Exception {

        // 1. 시트 ID(gid) 찾기
        Sheets service = getSheets();
        Integer sheetId = service.spreadsheets().get(SHEET_ID).execute()
                .getSheets().stream()
                .filter(s -> s.getProperties().getTitle().equals(sheetName))
                .map(s -> s.getProperties().getSheetId())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("시트 GID를 찾을 수 없음: " + sheetName));

        // 2. 텍스트 서식('@') 요청 생성
        Request formatRequest = new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(new GridRange()
                        .setSheetId(sheetId)
                        .setStartColumnIndex(startColumnZeroIndexed)
                        .setEndColumnIndex(endColumnZeroIndexed)
                )
                .setCell(new CellData().setUserEnteredFormat(
                        new CellFormat().setNumberFormat(
                                new NumberFormat().setType("TEXT")
                        )
                ))
                .setFields("userEnteredFormat.numberFormat")
        );

        // 3. BatchUpdate API로 요청 전송
        BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(Collections.singletonList(formatRequest));

        service.spreadsheets().batchUpdate(SHEET_ID, batchRequest).execute();
    }

    /**
     * 보유종목 데이터 가져오기.
     */
    public List<List<Object>> getSheetsData() throws Exception {

        return getData(SHEET_ID, DASHBOARD_SHEET_RANGE);
    }

    /**
     * 선택된 기관의 cik 값 셀 가져오기.
     */
    public String getActiveMonitoredCik() throws Exception {

        try {
            List<List<Object>> values = getData(SHEET_ID, CIK_CELL_RANGE);
            List<Object> row = values.get(0);
            return row.get(0).toString().trim();

        } catch (Exception e) {
            log.error("[ERROR] 구글 시트에서 CIK 값을 가져오는 중 오류 발생. Range: ", e);
            throw e; // 오류 전파
        }
    }

    /**
     * 구글시트의 기관 목록 업데이트
     * 앱스크립트 함수( updateFilerList )
     */
    public void updateFilerList(List<Filer> filerList) throws Exception {
        if(filerList == null || filerList.isEmpty()) return;

        log.warn("[Filer Update] 기관 목록 새로고침 작업 시작...");

        // 1. 셀 범위 초기화.
        clearSheetRange(FILER_SHEET_RANGE);

        // 2. B열 서식을 텍스트(@)로 지정.
        setColumnFormatAsText(FILER_SHEET_NAME, 1, 2);

        // 3. 데이터 본문 포맷팅. (List<PortfolioHolding> -> List<List<Object>>)
        List<List<Object>> values = filerList.stream()
                .map(h -> Arrays.asList(
                        (Object)(h.companyName())
                        ,h.cik()
                ))
                .collect(Collectors.toList());

        // 4. 데이터 본문 쓰기.
        String dataRange = String.format("%s!A1", FILER_SHEET_NAME);

        writeData(dataRange, values);

        log.info("[Filer Update] '{}' 기관 목록 시트에 {}개 갱신 완료.", FILER_SHEET_NAME, filerList.size());
    }

    /**
     * 구글시트의 기관 보유 종목 데이터 업데이트
     * 앱스크립트 함수( fetchHoldingsData )
     */
    public void updateHoldingsData(List<PortfolioHolding> data) throws Exception {

        // 1. 셀 범위 초기화.
        clearSheetRange(DASHBOARD_SHEET_RANGE);

        if (data == null || data.isEmpty()) {
            // 2-A. 데이터가 없으면 메시지 출력
            List<Object> emptyMessage = Collections.singletonList("보유 종목 데이터가 없습니다.");
            writeData(DASHBOARD_SHEET_NAME + "!E2", Collections.singletonList(emptyMessage));
            return;
        }

        // 2-B. 헤더 쓰기. (E2:I2)
        List<Object> headers = Arrays.asList("티커", "종목명", "보유 주식 수", "가치 (USD)", "비중 (%)");
        writeData(DASHBOARD_SHEET_NAME + "!E2:I2", Collections.singletonList(headers));
        // (참고: 헤더 굵게 처리는 별도 API(batchUpdate)가 필요하나, 우선 값만 씁니다)

        // 3. 데이터 본문 포맷팅. (List<PortfolioHolding> -> List<List<Object>>)
        List<List<Object>> values = data.stream()
                .map(h -> Arrays.asList(
                        h.ticker(),
                        h.nameOfIssuer(),
                        h.totalShares(),
                        h.totalValue(),
                        (Object) (h.totalPercentage() == 0.0 ? "-" : String.format("%.2f%%", h.totalPercentage()))
                ))
                .collect(Collectors.toList());

        // 4. 데이터 본문 쓰기. (E3부터 시작)
        String dataRange = String.format("%s!E3:%c%d",
                DASHBOARD_SHEET_NAME,
                'E' + headers.size() - 1, // 'I'
                values.size() + 2 // 데이터 행 수 + 헤더(1) + 시작행(2)
        );
        writeData(dataRange, values);

        log.info("[Sheets API] '{}' 시트에 {}개 종목 갱신 완료.", DASHBOARD_SHEET_NAME, data.size());
    }
}
