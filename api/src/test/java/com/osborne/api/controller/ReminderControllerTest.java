package com.osborne.api.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.osborne.api.config.SecurityConfig;
import com.osborne.api.dto.ReminderResponse;
import com.osborne.api.enums.ReminderStatus;
import com.osborne.api.enums.ReminderType;
import com.osborne.api.security.JwtUtil;
import com.osborne.api.service.ReminderService;

import jakarta.persistence.EntityNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReminderController.class)
@Import(SecurityConfig.class)
class ReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReminderService reminderService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private ReminderResponse buildReminderResponse() {
        return new ReminderResponse(
            UUID.randomUUID(),
            "Test reminder message",
            ReminderStatus.PENDING,
            ReminderType.BILL_MISMATCH,
            UUID.randomUUID(),
            UUID.randomUUID(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    @Nested
    class GetReminders {

        @Test
        @WithMockUser
        void shouldReturnPaginatedReminders() throws Exception {
            var reminder = buildReminderResponse();
            var page = new PageImpl<>(List.of(reminder), PageRequest.of(0, 20), 1);
            when(reminderService.getReminders(any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].message").value("Test reminder message"))
                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetPendingCount {

        @Test
        @WithMockUser
        void shouldReturnPendingCount() throws Exception {
            when(reminderService.getPendingCount()).thenReturn(3L);

            mockMvc.perform(get("/api/reminders/pending/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/reminders/pending/count"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class AcknowledgeReminder {

        @Test
        @WithMockUser
        void shouldAcknowledgeReminder() throws Exception {
            var reminder = new ReminderResponse(
                UUID.randomUUID(), "Acknowledged", ReminderStatus.ACKNOWLEDGED,
                ReminderType.BILL_MISMATCH, UUID.randomUUID(), null,
                LocalDateTime.now(), LocalDateTime.now()
            );
            when(reminderService.acknowledge(reminder.id())).thenReturn(reminder);

            mockMvc.perform(put("/api/reminders/" + reminder.id() + "/acknowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFound() throws Exception {
            var id = UUID.randomUUID();
            when(reminderService.acknowledge(id))
                .thenThrow(new EntityNotFoundException("Reminder not found"));

            mockMvc.perform(put("/api/reminders/" + id + "/acknowledge"))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotOwner() throws Exception {
            var id = UUID.randomUUID();
            when(reminderService.acknowledge(id))
                .thenThrow(new AccessDeniedException("Access denied"));

            mockMvc.perform(put("/api/reminders/" + id + "/acknowledge"))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(put("/api/reminders/" + UUID.randomUUID() + "/acknowledge"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class DismissReminder {

        @Test
        @WithMockUser
        void shouldDismissReminder() throws Exception {
            var reminder = new ReminderResponse(
                UUID.randomUUID(), "Dismissed", ReminderStatus.DISMISSED,
                ReminderType.GOAL_MILESTONE, UUID.randomUUID(), null,
                LocalDateTime.now(), LocalDateTime.now()
            );
            when(reminderService.dismiss(reminder.id())).thenReturn(reminder);

            mockMvc.perform(put("/api/reminders/" + reminder.id() + "/dismiss"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));
        }

        @Test
        @WithMockUser
        void shouldReturnNotFound() throws Exception {
            var id = UUID.randomUUID();
            when(reminderService.dismiss(id))
                .thenThrow(new EntityNotFoundException("Reminder not found"));

            mockMvc.perform(put("/api/reminders/" + id + "/dismiss"))
                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        void shouldReturnForbiddenWhenNotOwner() throws Exception {
            var id = UUID.randomUUID();
            when(reminderService.dismiss(id))
                .thenThrow(new AccessDeniedException("Access denied"));

            mockMvc.perform(put("/api/reminders/" + id + "/dismiss"))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(put("/api/reminders/" + UUID.randomUUID() + "/dismiss"))
                .andExpect(status().isUnauthorized());
        }
    }
}
