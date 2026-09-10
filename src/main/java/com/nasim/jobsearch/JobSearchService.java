package com.nasim.jobsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;

@Service
public class JobSearchService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    @Value("${anthropic.model:claude-sonnet-4-6}")
    private String model;

    @Value("${anthropic.api-key}")
    private String apiKey;

    @Value("${jobsearch.resume-path:resume.txt}")
    private String resumePath;

    @Value("${jobsearch.output-dir:job-results}")
    private String outputDir;

    @Value("${jobsearch.search-brief:senior backend/Java/Spring Boot roles in Stockholm or remote in Sweden}")
    private String searchBrief;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Runs automatically every day at 08:00 server time.
     * Cron format: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void runDailySearch() {
        try {
            String result = searchJobs();
            saveResult(result);
            System.out.println("Job search completed and saved for " + LocalDate.now());
        } catch (Exception e) {
           System.err.println("Job search run failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Builds the request, calls the Claude API with the built-in web_search tool,
     * and returns the model's final text answer.
     */
    public String searchJobs() throws IOException, InterruptedException {
        String resume = Files.readString(Path.of(resumePath), StandardCharsets.UTF_8);

        String prompt = """
                You are a job-search assistant. Below is my resume, followed by what I'm looking for.

                Search the web for current, real, still-open job postings that genuinely match my
                background. For each match, give: job title, company, location, a direct link to the
                posting, and one sentence on why it fits my resume. Skip anything that isn't a real,
                currently open posting. List at most 15 results, best matches first.

                WHAT I'M LOOKING FOR:
                %s

                MY RESUME:
                %s
                """.formatted(searchBrief, resume);

        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 4000);

        ArrayNode tools = mapper.createArrayNode();
        ObjectNode webSearchTool = mapper.createObjectNode();
        webSearchTool.put("type", "web_search_20250305");
        webSearchTool.put("name", "web_search");
        webSearchTool.put("max_uses", 10);
        tools.add(webSearchTool);
        body.set("tools", tools);

        ArrayNode messages = mapper.createArrayNode();
        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        body.set("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .header("content-type", "application/json")
                .timeout(Duration.ofMinutes(3))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Claude API returned " + response.statusCode() + ": " + response.body());
        }

        return extractText(response.body());
    }

    /**
     * The API can return a mix of block types (text, web search results/citations, etc).
     * This pulls out and concatenates just the plain text blocks.
     */
    private String extractText(String responseBody) throws IOException {
        JsonNode root = mapper.readTree(responseBody);
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : root.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                sb.append(block.path("text").asText());
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void saveResult(String result) throws IOException {
        Path dir = Path.of(outputDir);
        Files.createDirectories(dir);
        Path file = dir.resolve("jobs-" + LocalDate.now() + ".md");
        Files.writeString(file, result, StandardCharsets.UTF_8);
    }
}
