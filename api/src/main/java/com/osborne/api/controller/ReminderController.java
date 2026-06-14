package com.osborne.api.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.osborne.api.dto.ReminderResponse;
import com.osborne.api.enums.ReminderStatus;
import com.osborne.api.service.ReminderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @GetMapping
    public ResponseEntity<Page<ReminderResponse>> getReminders(
            @RequestParam(required = false) ReminderStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(reminderService.getReminders(status, pageable));
    }

    @GetMapping("/pending/count")
    public ResponseEntity<Long> getPendingCount() {
        return ResponseEntity.ok(reminderService.getPendingCount());
    }

    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<ReminderResponse> acknowledge(@PathVariable UUID id) {
        return ResponseEntity.ok(reminderService.acknowledge(id));
    }

    @PutMapping("/{id}/dismiss")
    public ResponseEntity<ReminderResponse> dismiss(@PathVariable UUID id) {
        return ResponseEntity.ok(reminderService.dismiss(id));
    }
}
