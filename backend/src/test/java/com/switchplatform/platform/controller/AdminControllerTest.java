package com.switchplatform.platform.controller;

import com.switchplatform.platform.model.BinTable;
import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.RoutingRule;
import com.switchplatform.platform.service.MonitoringService;
import com.switchplatform.platform.service.ParticipantService;
import com.switchplatform.platform.service.routing.BinTableService;
import com.switchplatform.platform.service.routing.RoutingRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private ParticipantService participantService;

    @Mock
    private RoutingRuleService routingRuleService;

    @Mock
    private BinTableService binTableService;

    @Mock
    private MonitoringService monitoringService;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(participantService, routingRuleService,
                binTableService, monitoringService);
    }

    @Test
    void dashboard_shouldReturnStats() {
        when(monitoringService.getDashboardStats()).thenReturn(Map.of("status", "ok"));

        var response = controller.dashboard();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ok", response.getBody().get("status"));
    }

    @Test
    void listParticipants_shouldReturnAll() {
        when(participantService.findAll()).thenReturn(List.of(new Participant()));

        var response = controller.listParticipants();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createParticipant_shouldReturnCreated() {
        Participant input = new Participant();
        when(participantService.create(any())).thenReturn(input);

        var response = controller.createParticipant(input);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getParticipant_shouldReturnById() {
        UUID id = UUID.randomUUID();
        Participant p = new Participant();
        when(participantService.findById(id)).thenReturn(p);

        var response = controller.getParticipant(id);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void updateParticipant_shouldReturnUpdated() {
        UUID id = UUID.randomUUID();
        Participant input = new Participant();
        when(participantService.update(eq(id), any())).thenReturn(input);

        var response = controller.updateParticipant(id, input);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void deleteParticipant_shouldReturnNoContent() {
        UUID id = UUID.randomUUID();

        var response = controller.deleteParticipant(id);

        assertEquals(204, response.getStatusCode().value());
        verify(participantService).delete(id);
    }

    @Test
    void listRoutingRules_shouldReturnAll() {
        when(routingRuleService.findAll()).thenReturn(List.of(new RoutingRule()));

        var response = controller.listRoutingRules();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createRoutingRule_shouldReturnCreated() {
        RoutingRule input = new RoutingRule();
        when(routingRuleService.create(any())).thenReturn(input);

        var response = controller.createRoutingRule(input);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void deleteRoutingRule_shouldReturnNoContent() {
        UUID id = UUID.randomUUID();

        var response = controller.deleteRoutingRule(id);

        assertEquals(204, response.getStatusCode().value());
        verify(routingRuleService).delete(id);
    }

    @Test
    void listBinTables_shouldReturnAll() {
        when(binTableService.findAll()).thenReturn(List.of(new BinTable()));

        var response = controller.listBinTables();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createBinTable_shouldReturnCreated() {
        BinTable input = new BinTable();
        when(binTableService.create(any())).thenReturn(input);

        var response = controller.createBinTable(input);

        assertEquals(200, response.getStatusCode().value());
    }
}
