package org.os4all.modules.ocr;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.core.security.JwtService;
import org.os4all.modules.auth.dto.RegisterRequest;
import org.os4all.modules.auth.service.AuthService;
import org.os4all.modules.lab.repository.LabReportRepository;
import org.os4all.modules.ocr.dto.ConfirmReportItemRequest;
import org.os4all.modules.ocr.dto.ConfirmReportRequest;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportIngestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LabReportRepository labReportRepository;

    @Autowired
    private JwtService jwtService;

    private String token;
    private User testUser;

    @BeforeEach
    void setUp() {
        String testEmail = "ocr_test_" + System.currentTimeMillis() + "@os4all.test";
        authService.register(new RegisterRequest(testEmail, "SecurePass123!", "OCR Test User"), "127.0.0.1");
        testUser = userRepository.findByEmail(testEmail).orElseThrow();
        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        token = "Bearer " + jwtService.generateToken(userDetails, testUser.getId());
    }

    private byte[] createSyntheticPdfReport(String contentText) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.newLineAtOffset(50, 700);

                String[] lines = contentText.split("\n");
                for (String line : lines) {
                    cs.showText(line);
                    cs.newLineAtOffset(0, -18);
                }
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("Upload valid PDF lab report -> extracted biomarkers, confidence scores, and review required flag")
    void shouldUploadAndParsePdfLabReport() throws Exception {
        String reportBody = "DEMO DATA: Quest Diagnostics Health Lab Report\n" +
                "Date: 2026-09-15\n" +
                "Fasting Blood Sugar: 94.5 mg/dL (Reference: 70 - 99)\n" +
                "Hemoglobin: 14.8 g/dL (Reference: 12.0 - 17.5)\n" +
                "ALT/SGPT: 28.0 U/L (Reference: 7.0 - 56.0)\n" +
                "Serum Creatinine: 0.95 mg/dL (Reference: 0.6 - 1.3)";

        byte[] pdfBytes = createSyntheticPdfReport(reportBody);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "synthetic_blood_panel.pdf",
                "application/pdf",
                pdfBytes
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/reports/upload")
                        .file(file)
                        .param("title", "DEMO DATA: Annual Health Screen PDF")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.reportTitle", is("DEMO DATA: Annual Health Screen PDF")))
                .andExpect(jsonPath("$.data.ocrStatus", is("COMPLETED")))
                .andExpect(jsonPath("$.data.results", hasSize(4)))
                .andExpect(jsonPath("$.data.results[?(@.biomarker =~ /.*Blood Sugar.*/i)].value", hasItem(94.5)))
                .andExpect(jsonPath("$.data.results[?(@.biomarker =~ /.*Hemoglobin.*/i)].standardUnit", hasItem("g/dL")))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String reportId = json.get("data").get("id").asText();

        // Verify GET /api/v1/reports/{id}
        mockMvc.perform(get("/api/v1/reports/" + reportId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(reportId)))
                .andExpect(jsonPath("$.data.results", hasSize(4)));

        // Verify GET /api/v1/reports list
        mockMvc.perform(get("/api/v1/reports")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Upload file with invalid magic bytes (e.g. text/exe spoofed as .pdf) -> rejected with 400 Bad Request")
    void shouldRejectInvalidMagicBytes() throws Exception {
        byte[] fakePdf = "NOT A REAL PDF HEADER JUST PLAIN TEXT".getBytes();

        MockMultipartFile spoofedFile = new MockMultipartFile(
                "file",
                "malicious_spoofed.pdf",
                "application/pdf",
                fakePdf
        );

        mockMvc.perform(multipart("/api/v1/reports/upload")
                        .file(spoofedFile)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported file format")));
    }

    @Test
    @DisplayName("Upload report with uncertain/noisy OCR -> review_required=true, is_confirmed=false, excluded from timeline until confirmed")
    void shouldFlagLowConfidenceExtractionForUserReview() throws Exception {
        // Line with noisy text and missing unit
        String noisyReport = "DEMO DATA: Noisy Lab Scan\n" +
                "Date: 2026-09-20\n" +
                "Total Bilirubin ?? 1.25\n" + // missing unit and has ? symbols -> low confidence!
                "Platelets 250.0 10^3/uL";

        byte[] pdfBytes = createSyntheticPdfReport(noisyReport);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "noisy_scan.pdf",
                "application/pdf",
                pdfBytes
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/reports/upload")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewRequired", is(true)))
                .andExpect(jsonPath("$.data.results[?(@.biomarker =~ /.*Bilirubin.*/i)].reviewRequired", hasItem(true)))
                .andExpect(jsonPath("$.data.results[?(@.biomarker =~ /.*Bilirubin.*/i)].isConfirmed", hasItem(false)))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String reportId = json.get("data").get("id").asText();
        String bilirubinResultId = null;

        for (JsonNode resNode : json.get("data").get("results")) {
            if (resNode.get("biomarker").asText().toLowerCase().contains("bilirubin")) {
                bilirubinResultId = resNode.get("id").asText();
                break;
            }
        }
        assertNotNull(bilirubinResultId);

        // Verify Timeline before confirmation: unconfirmed results must NEVER silently enter timeline!
        // Bilirubin had reviewRequired=true, so it must not be in the timeline biomarkers
        mockMvc.perform(get("/api/v1/health/timeline")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.title == 'noisy_scan.pdf')].details.biomarkers[?(@.biomarker =~ /.*Bilirubin.*/i)]", hasSize(0)));

        // User confirms and clarifies the low-confidence biomarker
        ConfirmReportRequest confirmRequest = new ConfirmReportRequest();
        confirmRequest.setReportTitle("DEMO DATA: Verified Bilirubin Scan");

        ConfirmReportItemRequest itemUpdate = new ConfirmReportItemRequest();
        itemUpdate.setId(UUID.fromString(bilirubinResultId));
        itemUpdate.setBiomarker("Total Bilirubin");
        itemUpdate.setValue(new BigDecimal("1.10"));
        itemUpdate.setUnit("mg/dL");
        itemUpdate.setIsConfirmed(true);
        confirmRequest.setItems(Collections.singletonList(itemUpdate));

        mockMvc.perform(post("/api/v1/reports/" + reportId + "/confirm")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewRequired", is(false)))
                .andExpect(jsonPath("$.data.results[?(@.id == '" + bilirubinResultId + "')].isConfirmed", hasItem(true)))
                .andExpect(jsonPath("$.data.results[?(@.id == '" + bilirubinResultId + "')].value", hasItem(1.10)))
                .andExpect(jsonPath("$.data.results[?(@.id == '" + bilirubinResultId + "')].unit", hasItem("mg/dL")));

        // Verify Timeline after confirmation: now safely included in patient timeline
        mockMvc.perform(get("/api/v1/health/timeline")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.title == 'DEMO DATA: Verified Bilirubin Scan')]", hasSize(1)));
    }

    @Test
    @DisplayName("Upload valid PNG image report with PNG magic bytes -> accepted and processed")
    void shouldUploadPngImageReport() throws Exception {
        // PNG magic bytes: 0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A followed by payload
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        String metadataText = "DEMO DATA: Fasting Glucose 98.0 mg/dL\nHbA1c 5.4 %\n";
        byte[] textBytes = metadataText.getBytes();
        byte[] fullPng = new byte[pngHeader.length + textBytes.length];
        System.arraycopy(pngHeader, 0, fullPng, 0, pngHeader.length);
        System.arraycopy(textBytes, 0, fullPng, pngHeader.length, textBytes.length);

        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "synthetic_report.png",
                "image/png",
                fullPng
        );

        mockMvc.perform(multipart("/api/v1/reports/upload")
                        .file(pngFile)
                        .param("title", "DEMO DATA: Synthetic PNG Report")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.mimeType", is("image/png")))
                .andExpect(jsonPath("$.data.fileName", is("synthetic_report.png")));
    }
}
