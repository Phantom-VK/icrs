package com.college.icrs.evaluation;

import com.college.icrs.config.IcrsProperties;
import com.college.icrs.dto.CategoryResponseDTO;
import com.college.icrs.dto.GrievanceResponseDTO;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Role;
import com.college.icrs.model.User;
import com.college.icrs.rag.RagService;
import com.college.icrs.repository.GrievanceRepository;
import com.college.icrs.repository.UserRepository;
import com.college.icrs.responses.LoginResponse;
import com.college.icrs.tools.GrievanceVectorImportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OperationalEvaluationService {

    private final OperationalEvaluationDatasetValidator datasetValidator;
    private final OperationalEvaluationMetricsCalculator metricsCalculator;
    private final OperationalEvaluationCsvWriter csvWriter;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GrievanceRepository grievanceRepository;
    private final GrievanceVectorImportService grievanceVectorImportService;
    private final RagService ragService;
    private final JdbcTemplate jdbcTemplate;
    private final IcrsProperties icrsProperties;
    private final VectorStore vectorStore;

    public OperationalEvaluationReport run(OperationalEvaluationConfig config) throws IOException {
        LocalDateTime startedAt = LocalDateTime.now();
        OperationalEvaluationDatasets datasets = datasetValidator.validate(config.historicalFile(), config.liveFile());

        RestClient restClient = RestClient.builder()
                .baseUrl(config.backendBaseUrl())
                .build();

        OperationalEvaluationRunSummary.EnvironmentHealth environmentHealth = checkEnvironment(restClient);
        User evaluationStudent = ensureStudentAccount(config);
        OperationalEvaluationRunSummary.CleanupSummary cleanupSummary = cleanupExperimentData(
                evaluationStudent,
                datasets.historicalCases().stream().map(OperationalEvaluationHistoricalCase::documentId).toList()
        );

        grievanceVectorImportService.importFile(config.historicalFile(), true);

        List<OperationalEvaluationResult> results = new ArrayList<>();
        boolean stoppedEarly = false;
        String stopReason = null;

        for (int index = 0; index < datasets.liveCases().size(); index++) {
            OperationalEvaluationLiveCase liveCase = datasets.liveCases().get(index);
            String bearerToken = login(restClient, config);
            results.add(runSingleCase(restClient, bearerToken, liveCase, config.perCaseTimeout()));

            if (shouldStopEarly(results, config)) {
                stoppedEarly = true;
                stopReason = "Stopped after early AI degradation threshold was reached.";
                break;
            }

            if (index < datasets.liveCases().size() - 1) {
                sleep(config.submissionPause());
            }
        }

        OperationalEvaluationMetrics metrics = metricsCalculator.calculate(results);
        OperationalEvaluationRunSummary summary = new OperationalEvaluationRunSummary(
                config.backendBaseUrl(),
                config.historicalFile().toString(),
                config.liveFile().toString(),
                config.submissionPause().toMillis(),
                config.perCaseTimeout().toMillis(),
                icrsProperties.getAi().isEnabled(),
                icrsProperties.getAi().getSentiment().isEnabled(),
                datasets.historicalCases().size(),
                datasets.liveCases().size(),
                stoppedEarly,
                stopReason,
                environmentHealth,
                cleanupSummary,
                startedAt,
                LocalDateTime.now()
        );

        writeArtifacts(config.outputDir(), summary, metrics, results);
        return new OperationalEvaluationReport(summary, metrics, results);
    }

    private OperationalEvaluationResult runSingleCase(
            RestClient restClient,
            String bearerToken,
            OperationalEvaluationLiveCase liveCase,
            Duration timeout
    ) {
        LocalDateTime submittedAt = LocalDateTime.now();
        try {
            GrievanceResponseDTO created = submitGrievance(restClient, bearerToken, liveCase);
            submittedAt = created.getCreatedAt() != null ? created.getCreatedAt() : submittedAt;
            GrievanceResponseDTO latest = pollForCompletion(restClient, bearerToken, created.getId(), timeout);

            if (latest == null) {
                return buildTimeoutResult(liveCase, created.getId(), submittedAt, "Grievance not visible to student polling flow.");
            }

            if (latest.getAiDecisionAt() == null && icrsProperties.getAi().isEnabled()) {
                return buildTimeoutResult(liveCase, latest.getId(), submittedAt, "Timed out waiting for AI decision.");
            }

            return buildCompletedResult(liveCase, latest, submittedAt);
        } catch (Exception ex) {
            return new OperationalEvaluationResult(
                    liveCase.caseId(),
                    liveCase.title(),
                    liveCase.category(),
                    liveCase.subcategory(),
                    liveCase.sensitiveCategory(),
                    null,
                    "failed_submission",
                    ex.getMessage(),
                    submittedAt,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    null,
                    null,
                    null,
                    null,
                    List.of()
            );
        }
    }

    private OperationalEvaluationResult buildCompletedResult(
            OperationalEvaluationLiveCase liveCase,
            GrievanceResponseDTO latest,
            LocalDateTime submittedAt
    ) {
        List<OperationalEvaluationRetrievedReference> references = retrievedReferences(liveCase, latest.getId());
        return new OperationalEvaluationResult(
                liveCase.caseId(),
                liveCase.title(),
                liveCase.category(),
                liveCase.subcategory(),
                liveCase.sensitiveCategory(),
                latest.getId(),
                "completed",
                null,
                submittedAt,
                latest.getAiDecisionAt(),
                latencyMillis(submittedAt, latest.getAiDecisionAt()),
                latest.getStatus() != null ? latest.getStatus().name() : null,
                latest.getAssignedToName(),
                latest.getPriority() != null ? latest.getPriority().name() : null,
                latest.getSentiment() != null ? latest.getSentiment().name() : null,
                latest.isAiResolved(),
                latest.getAiConfidence(),
                latest.getAiTitle(),
                latest.getAiResolutionText(),
                latest.getAiModelName(),
                references
        );
    }

    private OperationalEvaluationResult buildTimeoutResult(
            OperationalEvaluationLiveCase liveCase,
            long grievanceId,
            LocalDateTime submittedAt,
            String reason
    ) {
        return new OperationalEvaluationResult(
                liveCase.caseId(),
                liveCase.title(),
                liveCase.category(),
                liveCase.subcategory(),
                liveCase.sensitiveCategory(),
                grievanceId,
                "timed_out",
                reason,
                submittedAt,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    private GrievanceResponseDTO submitGrievance(RestClient restClient, String bearerToken, OperationalEvaluationLiveCase liveCase) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", liveCase.title());
        payload.put("description", liveCase.description());
        payload.put("category", liveCase.category());
        payload.put("subcategory", liveCase.subcategory());
        payload.put("registrationNumber", liveCase.registrationNumber());

        return restClient.post()
                .uri("/grievances")
                .header(HttpHeaders.AUTHORIZATION, bearer(bearerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(GrievanceResponseDTO.class);
    }

    private GrievanceResponseDTO pollForCompletion(RestClient restClient, String bearerToken, long grievanceId, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        GrievanceResponseDTO lastSeen = null;

        while (System.currentTimeMillis() < deadline) {
            lastSeen = findGrievance(restClient, bearerToken, grievanceId);
            if (lastSeen != null && isAiComplete(lastSeen)) {
                return lastSeen;
            }
            sleep(Duration.ofSeconds(1));
        }

        return lastSeen;
    }

    private GrievanceResponseDTO findGrievance(RestClient restClient, String bearerToken, long grievanceId) {
        GrievanceResponseDTO[] grievances = restClient.get()
                .uri("/grievances/student/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(bearerToken))
                .retrieve()
                .body(GrievanceResponseDTO[].class);

        if (grievances == null) {
            return null;
        }

        for (GrievanceResponseDTO grievance : grievances) {
            if (grievance != null && grievance.getId() == grievanceId) {
                return grievance;
            }
        }
        return null;
    }

    private boolean isAiComplete(GrievanceResponseDTO grievance) {
        return grievance != null && (!icrsProperties.getAi().isEnabled() || grievance.getAiDecisionAt() != null);
    }

    private List<OperationalEvaluationRetrievedReference> retrievedReferences(OperationalEvaluationLiveCase liveCase, long grievanceId) {
        Optional<Grievance> grievance = grievanceRepository.findById(grievanceId);
        if (grievance.isEmpty()) {
            return List.of();
        }
        return ragService.retrieveSimilar(grievance.get()).stream()
                .map(reference -> new OperationalEvaluationRetrievedReference(
                        reference.getReferenceId(),
                        reference.getSource(),
                        reference.getCategory(),
                        reference.getSubcategory(),
                        reference.getSimilarityScore(),
                        normalize(reference.getCategory()).equals(normalize(liveCase.category()))
                ))
                .toList();
    }

    private OperationalEvaluationRunSummary.EnvironmentHealth checkEnvironment(RestClient restClient) {
        boolean backendReachable = false;
        int categoryCount = 0;
        try {
            CategoryResponseDTO[] categories = restClient.get()
                    .uri("/categories")
                    .retrieve()
                    .body(CategoryResponseDTO[].class);
            backendReachable = true;
            categoryCount = categories != null ? categories.length : 0;
        } catch (Exception ignored) {
            backendReachable = false;
        }

        boolean databaseReachable = false;
        Integer vectorCount = null;
        try {
            Integer result = jdbcTemplate.queryForObject("select count(*) from vector_store", Integer.class);
            databaseReachable = true;
            vectorCount = result;
        } catch (Exception ignored) {
            databaseReachable = false;
        }

        boolean sentimentHealthy = !icrsProperties.getAi().isEnabled()
                || !icrsProperties.getAi().getSentiment().isEnabled()
                || isSentimentServiceHealthy(
                icrsProperties.getAi().getSentiment().getBaseUrl(),
                Math.max(icrsProperties.getAi().getSentiment().getTimeoutMs(), 1000)
        );

        return new OperationalEvaluationRunSummary.EnvironmentHealth(
                backendReachable,
                categoryCount,
                databaseReachable,
                vectorCount,
                sentimentHealthy,
                icrsProperties.getAi().getSentiment().getBaseUrl()
        );
    }

    @Transactional
    protected User ensureStudentAccount(OperationalEvaluationConfig config) {
        User user = userRepository.findByEmail(config.studentEmail()).orElseGet(User::new);
        user.setUsername(config.studentName());
        user.setEmail(config.studentEmail());
        user.setPassword(passwordEncoder.encode(config.studentPassword()));
        user.setRole(Role.STUDENT);
        user.setEnabled(true);
        user.setDepartment(config.studentDepartment());
        user.setStudentId(config.studentId());
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        return userRepository.save(user);
    }

    @Transactional
    protected OperationalEvaluationRunSummary.CleanupSummary cleanupExperimentData(User student, List<String> historicalDocumentIds) {
        List<Grievance> existingGrievances = grievanceRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        List<String> liveVectorIds = existingGrievances.stream()
                .map(grievance -> String.valueOf(grievance.getId()))
                .toList();

        if (!liveVectorIds.isEmpty()) {
            vectorStore.delete(liveVectorIds);
        }
        if (!historicalDocumentIds.isEmpty()) {
            vectorStore.delete(historicalDocumentIds);
        }
        if (!existingGrievances.isEmpty()) {
            grievanceRepository.deleteAll(existingGrievances);
            grievanceRepository.flush();
        }

        return new OperationalEvaluationRunSummary.CleanupSummary(
                student.getEmail(),
                existingGrievances.size(),
                liveVectorIds.size(),
                historicalDocumentIds.size()
        );
    }

    private String login(RestClient restClient, OperationalEvaluationConfig config) {
        LoginResponse response = restClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "email", config.studentEmail(),
                        "password", config.studentPassword()
                ))
                .retrieve()
                .body(LoginResponse.class);

        if (response == null || response.getToken() == null || response.getToken().isBlank()) {
            throw new IllegalStateException("Login did not return a bearer token.");
        }
        return response.getToken();
    }

    private void writeArtifacts(
            java.nio.file.Path outputDir,
            OperationalEvaluationRunSummary summary,
            OperationalEvaluationMetrics metrics,
            List<OperationalEvaluationResult> results
    ) throws IOException {
        Files.createDirectories(outputDir);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(outputDir.resolve("results.json").toFile(), new OperationalEvaluationResultsFile(summary, results));
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(outputDir.resolve("metrics.json").toFile(), new OperationalEvaluationMetricsFile(summary, metrics));
        csvWriter.write(outputDir.resolve("results.csv"), results);
    }

    private boolean shouldStopEarly(List<OperationalEvaluationResult> results, OperationalEvaluationConfig config) {
        if (results.size() < Math.max(config.earlyFailureWindow(), 1)) {
            return false;
        }
        List<OperationalEvaluationResult> recent = results.subList(results.size() - config.earlyFailureWindow(), results.size());
        long failures = recent.stream()
                .filter(result -> !"completed".equalsIgnoreCase(result.experimentState()))
                .count();
        return failures >= Math.max(config.earlyFailureThreshold(), 1);
    }

    private Long latencyMillis(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start, end).toMillis();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isSentimentServiceHealthy(String baseUrl, int timeoutMs) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return false;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl.replaceAll("/+$", "") + "/health")
                    .toURL()
                    .openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.connect();
            int status = connection.getResponseCode();
            connection.disconnect();
            return status >= 200 && status < 300;
        } catch (Exception ex) {
            return false;
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(Math.max(duration.toMillis(), 0L));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting during operational evaluation.", ex);
        }
    }
}
