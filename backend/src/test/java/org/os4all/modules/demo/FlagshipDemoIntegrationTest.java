package org.os4all.modules.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlagshipDemoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Demo Reset: POST /api/v1/demo/reset seeds 45 days of stable observations and returns STABLE state")
    void testDemoResetToStableBaseline() throws Exception {
        mockMvc.perform(post("/api/v1/demo/reset")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.demoUserEmail").value("demo.patient@os4all.test"))
                .andExpect(jsonPath("$.data.demoUserName").value("Demo User — Synthetic Data"))
                .andExpect(jsonPath("$.data.aggregateState").value("STABLE"))
                .andExpect(jsonPath("$.data.detectedAnomalies", hasSize(0)))
                .andExpect(jsonPath("$.data.evaluatedSignals", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$.data.baselineMetrics", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$.data.syntheticDataNotice", containsString("DEMO DATA — NOT A REAL PATIENT")))
                .andExpect(jsonPath("$.data.progressionStages", hasSize(1)))
                .andExpect(jsonPath("$.data.progressionStages[0].status").value("STABLE"));
    }

    @Test
    @DisplayName("Health Drift Demo: POST /api/v1/demo/run-health-drift deterministically demonstrates STABLE -> DRIFT -> MULTI-SIGNAL ANOMALY")
    void testDemoHealthDriftProgression() throws Exception {
        mockMvc.perform(post("/api/v1/demo/run-health-drift")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.demoUserEmail").value("demo.patient@os4all.test"))
                .andExpect(jsonPath("$.data.demoUserName").value("Demo User — Synthetic Data"))
                .andExpect(jsonPath("$.data.aggregateState").value(anyOf(equalTo("ANOMALY"), equalTo("DRIFT"))))
                // Evaluated multi-signal measurements (RHR, HRV, Sleep, SpO2, Activity)
                .andExpect(jsonPath("$.data.evaluatedSignals", hasSize(greaterThanOrEqualTo(5))))
                // Multi-signal anomaly detected
                .andExpect(jsonPath("$.data.detectedAnomalies", hasSize(greaterThanOrEqualTo(1))))
                // Progression stages: STABLE -> DRIFT -> MULTI-SIGNAL ANOMALY
                .andExpect(jsonPath("$.data.progressionStages", hasSize(3)))
                .andExpect(jsonPath("$.data.progressionStages[0].status").value("STABLE"))
                .andExpect(jsonPath("$.data.progressionStages[1].status").value("DRIFT"))
                .andExpect(jsonPath("$.data.progressionStages[2].status").value("MULTI-SIGNAL ANOMALY"))
                // Execution chain
                .andExpect(jsonPath("$.data.executionChain.step1Data").value(containsString("DATA:")))
                .andExpect(jsonPath("$.data.executionChain.step2Baseline").value(containsString("BASELINE:")))
                .andExpect(jsonPath("$.data.executionChain.step3Drift").value(containsString("DRIFT:")))
                .andExpect(jsonPath("$.data.executionChain.step4MultiSignalPattern").value(containsString("MULTI-SIGNAL PATTERN:")))
                .andExpect(jsonPath("$.data.executionChain.step5NemotronReasoning").value(containsString("NEMOTRON REASONING:")))
                // AI health insight
                .andExpect(jsonPath("$.data.aiHealthInsight.summary").isNotEmpty())
                .andExpect(jsonPath("$.data.aiHealthInsight.disclaimer", containsString("DO NOT constitute a medical diagnosis")))
                // Synthetic notice
                .andExpect(jsonPath("$.data.syntheticDataNotice", containsString("DEMO DATA — NOT A REAL PATIENT")));
    }

    @Test
    @DisplayName("Flagship OS4All End-to-End Demonstration executes the complete 9-step chain reproducibly")
    void testFlagshipDemonstrationScenario() throws Exception {
        mockMvc.perform(post("/api/v1/demo/run-flagship-scenario")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.demoUserEmail").value("demo.patient@os4all.test"))
                .andExpect(jsonPath("$.data.demoUserName").value("Demo User — Synthetic Data"))
                .andExpect(jsonPath("$.data.aggregateState").value(anyOf(equalTo("ANOMALY"), equalTo("DRIFT"))))
                // Visual Execution Chain verification
                .andExpect(jsonPath("$.data.executionChain.step1Data").value(containsString("DATA:")))
                .andExpect(jsonPath("$.data.executionChain.step2Baseline").value(containsString("BASELINE:")))
                .andExpect(jsonPath("$.data.executionChain.step3Drift").value(containsString("DRIFT:")))
                .andExpect(jsonPath("$.data.executionChain.step4MultiSignalPattern").value(containsString("MULTI-SIGNAL PATTERN:")))
                .andExpect(jsonPath("$.data.executionChain.step5NemotronReasoning").value(containsString("NEMOTRON REASONING:")))
                .andExpect(jsonPath("$.data.executionChain.step6TavilyEvidence").value(containsString("TAVILY EVIDENCE:")))
                .andExpect(jsonPath("$.data.executionChain.step7Os4AllInsight").value(containsString("OS4All INSIGHT:")))
                .andExpect(jsonPath("$.data.executionChain.step8RecommendedAction").value(containsString("ACTION:")))
                .andExpect(jsonPath("$.data.executionChain.step9TimelineUpdate").value(containsString("TIMELINE UPDATE:")))
                // Personal baseline metrics verification
                .andExpect(jsonPath("$.data.baselineMetrics", hasSize(greaterThanOrEqualTo(3))))
                // Evaluated signals & multi-signal anomaly verification
                .andExpect(jsonPath("$.data.evaluatedSignals", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data.detectedAnomalies", hasSize(greaterThanOrEqualTo(1))))
                // AI Health Insight verification
                .andExpect(jsonPath("$.data.aiHealthInsight.summary").isNotEmpty())
                .andExpect(jsonPath("$.data.aiHealthInsight.observations", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.aiHealthInsight.possibleInterpretations", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.aiHealthInsight.evidence", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.aiHealthInsight.recommendedActions", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.aiHealthInsight.disclaimer", containsString("DO NOT constitute a medical diagnosis")))
                // Timeline events update verification
                .andExpect(jsonPath("$.data.timelineEvents", hasSize(greaterThanOrEqualTo(5))))
                // Synthetic notice
                .andExpect(jsonPath("$.data.syntheticDataNotice", containsString("DEMO DATA NOTICE")));
    }
}
