package com.osborne.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osborne.api.dto.AddGoalUserRequest;
import com.osborne.api.dto.UserResponse;
import com.osborne.api.service.GoalService;
import com.osborne.api.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/goals/{goalId}/users")
@RequiredArgsConstructor
public class GoalUserController {

    private final GoalService goalService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(@PathVariable UUID goalId) {
        List<UserResponse> users = goalService.getGoalUsers(goalId).stream()
            .map(userService::toResponse)
            .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<Void> addUser(
            @PathVariable UUID goalId,
            @Valid @RequestBody AddGoalUserRequest request) {
        goalService.addUserToGoal(goalId, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeUser(
            @PathVariable UUID goalId,
            @PathVariable UUID userId) {
        goalService.removeUserFromGoal(goalId, userId);
        return ResponseEntity.noContent().build();
    }
}
