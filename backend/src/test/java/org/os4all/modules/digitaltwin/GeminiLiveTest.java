package org.os4all.modules.digitaltwin;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

public class GeminiLiveTest {
    @Test
    void testLiveGeminiModels() {
        String key = resolveApiKey();
        if (key.isBlank()) return;
        List<String> candidateModels = List.of(
                "gemini-2.5-flash",
                "gemini-2.0-flash",
                "gemini-1.5-flash",
                "gemini-1.5-flash-latest",
                "gemini-1.5-pro",
                "gemini-3.8-flash"
        );
        Client client = Client.builder().apiKey(key).build();
        for (String model : candidateModels) {
            try {
                System.out.println(">>> Querying model: " + model);
                GenerateContentResponse resp = client.models.generateContent(model, "ping", null);
                System.out.println(">>> SUCCESS with " + model + ": " + resp.text().trim());
                return;
            } catch (Exception e) {
                System.out.println(">>> Result for " + model + ": " + e.getMessage());
            }
        }
    }

    private static String resolveApiKey() {
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isBlank()) return envKey.trim();
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            try {
                Process p = new ProcessBuilder("powershell", "-NoProfile", "-Command",
                        "[Environment]::GetEnvironmentVariable('GEMINI_API_KEY', 'User')").start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && !line.trim().isBlank()) return line.trim();
                }
            } catch (Exception ignored) {}
        }
        return "";
    }
}
