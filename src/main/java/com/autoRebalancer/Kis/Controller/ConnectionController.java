package com.autoRebalancer.Kis.Controller;

import com.autoRebalancer.Kis.Token.TokenHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConnectionController {
    private final TokenHolder tokenHolder;

    @Autowired
    public ConnectionController(TokenHolder tokenHolder) {
        this.tokenHolder = tokenHolder;
    }

    /**
     * 접근토큰 확인 및 갱신
     */
    @GetMapping()
    public void requestAccessToken() {
        tokenHolder.getAccessToken();
    }
}
