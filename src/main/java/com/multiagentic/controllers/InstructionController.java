package com.multiagentic.controllers;

import com.multiagentic.dto.InstructionRequest;
import com.multiagentic.services.WorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InstructionController {

    private final WorkflowService workflowService;

    public InstructionController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/process-instruction")
    public ResponseEntity<String> processInstruction(@RequestBody InstructionRequest instructionRequest) {
        String instruction = instructionRequest.getInstruction();

        if (instruction == null || instruction.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("No instruction provided.");
        }

        try {
            String response = workflowService.generateScript(instruction);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while processing your request: " + e.getMessage());
        }
    }
}
