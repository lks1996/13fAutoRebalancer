package com.autoRebalancer.Googlesheet.Controller;

import com.autoRebalancer.Common.ApiResponse;
import com.autoRebalancer.Googlesheet.Service.SheetDataImportService;
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

    @Autowired
    public SheetDataImportController(SheetDataImportService sheetDataImportService) {
        this.sheetDataImportService = sheetDataImportService;
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
}
