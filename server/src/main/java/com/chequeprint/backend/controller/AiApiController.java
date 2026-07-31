package com.chequeprint.backend.controller;

import com.chequeprint.backend.dto.AiPromptRequest;
import com.chequeprint.backend.dto.AiPromptResponse;
import com.chequeprint.backend.service.AiBackendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiApiController {

    private final AiBackendService aiService;

    @Autowired
    public AiApiController(AiBackendService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/ask")
    public ResponseEntity<AiPromptResponse> askAi(@RequestBody AiPromptRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            return ResponseEntity.badRequest().body(AiPromptResponse.fail("Prompt text is required."));
        }
        AiPromptResponse response = aiService.generateAiResponse(request.getPrompt());
        return ResponseEntity.ok(response);
    }
}
