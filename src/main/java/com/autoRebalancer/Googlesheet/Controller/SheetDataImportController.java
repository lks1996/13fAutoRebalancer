package com.autoRebalancer.Googlesheet.Controller;

import com.autoRebalancer.Common.ApiResponse;
import com.autoRebalancer.Googlesheet.Service.SheetDataImportService;
import com.autoRebalancer._13f.Service.FilingPersistenceService;
import com.autoRebalancer._13f.Service.FilingProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sheet")
public class SheetDataImportController {

    private final SheetDataImportService sheetDataImportService;
    private final FilingProcessService filingProcessService;
    private final FilingPersistenceService persistenceService;

    @Autowired
    public SheetDataImportController(SheetDataImportService sheetDataImportService
            , FilingProcessService filingProcessService
            , FilingPersistenceService persistenceService) {
        this.sheetDataImportService = sheetDataImportService;
        this.filingProcessService = filingProcessService;
        this.persistenceService = persistenceService;
    }

    /**
     * 구글시트의 포트폴리오 데이터 임포트.
     */
    @GetMapping("/dataImport")
    public ResponseEntity<ApiResponse<List<List<Object>>>> getDomesticStockBalance() throws Exception{
        List<List<Object>> result = sheetDataImportService.getSheetsData();
        return new ResponseEntity<>(ApiResponse.success(result), HttpStatus.OK);
    }

    /**
     * 구글시트의 특정 셀(cik) 조회.
     */
    @GetMapping("/activeMonitoredCik")
    public ResponseEntity<ApiResponse<String>> getActiveMonitoredCik() throws Exception{
        String result = sheetDataImportService.getActiveMonitoredCik();
        return new ResponseEntity<>(ApiResponse.success(result), HttpStatus.OK);
    }

    /**
     * 선택한 기관의 CIK 값을 기준으로 가징 최신의 13f 데이터로 구글시트 최신화.
     */
    @GetMapping("/sheetRefresh")
    public void triggerSheetRefresh(String selectedCik) throws Exception {
        sheetDataImportService.updateFilerList(persistenceService.findAllFilers());
    }

    /**
     * 선택한 기관의 CIK 값을 기준으로 가징 최신의 13f 데이터로 구글시트 최신화.
     */
    @GetMapping("/holdingsRefresh")
    public void triggerHoldingsRefresh(String selectedCik) throws Exception {
        sheetDataImportService.updateHoldingsData(filingProcessService.getOrFetchHoldingsByCik(selectedCik));
    }
}
