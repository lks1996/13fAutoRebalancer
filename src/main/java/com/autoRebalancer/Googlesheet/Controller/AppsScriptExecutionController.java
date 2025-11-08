package com.autoRebalancer.Googlesheet.Controller;

import com.autoRebalancer.Googlesheet.Service.AppsScriptExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/appScript")
public class AppsScriptExecutionController {

    private final AppsScriptExecutionService appsScriptExecutionService;

    @Autowired
    public AppsScriptExecutionController(AppsScriptExecutionService appsScriptExecutionService) {
        this.appsScriptExecutionService = appsScriptExecutionService;
    }

    /**
     * 앱스크립트 함수 원격 실행.
     */
    @GetMapping("/triggerSheetRefresh")
    public void triggerSheetRefresh(String functionName) throws Exception {
        appsScriptExecutionService.triggerSheetRefresh(functionName);
    }
}
