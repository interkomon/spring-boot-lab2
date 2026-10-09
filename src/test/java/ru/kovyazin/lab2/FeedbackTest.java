package ru.kovyazin.lab2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.kovyazin.lab2.controller.MyController;
import ru.kovyazin.lab2.service.ValidationService;

import java.io.InputStream;
import java.time.Instant;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FeedbackTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    static Stream<Arguments> cases() throws Exception {
        try (InputStream in = FeedbackTest.class.getResourceAsStream("/cases.json")) {
            JsonNode data = new ObjectMapper().readTree(in);
            return StreamSupport.stream(data.spliterator(), false)
                    .map(c -> Arguments.of(c.get("name").asText(), c));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void feedbackCases(String name, JsonNode testCase) throws Exception {
        int status = testCase.get("status").asInt();
        MvcResult result = mvc.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCase.get("body").asText()))
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
        JsonNode body = mapper.readTree(result.getResponse().getContentAsByteArray());
        for (String field : new String[]{"uid", "operationUid", "systemTime",
                "code", "errorCode", "errorMessage"}) {
            assertTrue(body.has(field), "Отсутствует поле " + field);
            assertTrue(body.get(field).isTextual(), "Неверный тип " + field);
        }
        assertEquals(status == 200 ? "success" : "failed", body.get("code").asText());
        assertEquals(testCase.get("errorCode").asText(), body.get("errorCode").asText());
        Instant.parse(body.get("systemTime").asText());
        for (JsonNode field : testCase.get("fields")) {
            assertTrue(body.get("errorMessage").asText().contains(field.asText()));
        }
        if (status == 200) {
            JsonNode request = mapper.readTree(testCase.get("body").asText());
            assertEquals(request.get("uid").asText(), body.get("uid").asText());
            assertEquals(request.get("operationUid").asText(), body.get("operationUid").asText());
            assertEquals("", body.get("errorMessage").asText());
        } else {
            assertFalse(body.get("errorMessage").asText().isBlank());
        }
    }

    @Test
    void serverTimeIsGeneratedInsteadOfCopied() throws Exception {
        Instant before = Instant.now().minusSeconds(1);
        MvcResult result = mvc.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"uid":"1","operationUid":"op-1",
                                 "systemTime":"1999-01-01T00:00:00Z","communicationId":1}
                                """))
                .andExpect(status().isOk()).andReturn();
        Instant time = Instant.parse(mapper.readTree(result.getResponse()
                .getContentAsByteArray()).get("systemTime").asText());
        assertFalse(time.isBefore(before));
        assertFalse(time.isAfter(Instant.now().plusSeconds(1)));
    }

    @Test
    void unexpectedServiceFailureReturns500() throws Exception {
        ValidationService brokenService = result -> {
            throw new IllegalStateException("Тестовая внутренняя ошибка");
        };
        MockMvc isolated = MockMvcBuilders.standaloneSetup(
                new MyController(brokenService)).build();
        isolated.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"uid":"1","operationUid":"op-1",
                                 "systemTime":"2026-10-08T10:00:00Z","communicationId":1}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("failed"))
                .andExpect(jsonPath("$.errorCode").value("UnknownException"));
    }
}
