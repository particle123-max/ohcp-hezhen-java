package com.ohcp.cropagent.controller;

import com.ohcp.cropagent.domain.DiagnosisRequest;
import com.ohcp.cropagent.domain.DiagnosisResponse;
import com.ohcp.cropagent.service.DiagnosisAgentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DiagnosisController {

    private final DiagnosisAgentService diagnosisAgentService;

    public DiagnosisController(DiagnosisAgentService diagnosisAgentService) {
        this.diagnosisAgentService = diagnosisAgentService;
    }

    @PostMapping(value = "/diagnose", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DiagnosisResponse diagnose(@Valid @RequestBody DiagnosisRequest request) {
        return diagnosisAgentService.diagnose(request);
    }
}
