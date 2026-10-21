package com.ohcp.cropagent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiagnosisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void diagnosisEndToEndReturnsAdviceAndSources() throws Exception {
        String body = """
                {
                  "crop": "水稻",
                  "part": "下部老叶",
                  "symptoms": "叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少。",
                  "growthStage": "分蘖期",
                  "soilType": "沙质土",
                  "environment": "近期降雨较多"
                }
                """;

        mockMvc.perform(post("/api/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].symbol").value("N"))
                .andExpect(jsonPath("$.advice.chemicalPlan").isArray())
                .andExpect(jsonPath("$.risk.level").isString())
                .andExpect(jsonPath("$.sources[0].title").isString())
                .andExpect(jsonPath("$.civicNote").isString());
    }

    @Test
    void validationReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"crop\":\"\",\"part\":\"\",\"symptoms\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void healthAndKnowledgeEndpointsAreAvailable() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.coreEngine").value("LOCAL_RAG"));

        mockMvc.perform(get("/api/knowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryCount").value(8));
    }
}
