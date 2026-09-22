package net.trellisframework.workflow.temporal.compatibility;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class WorkflowCompatibility {

    public static final String BASE_PROPERTY = "workflow.base";
    public static final String BASE_ENV = "WORKFLOW_BASE";

    private static final String NEEDS_CHANGE_ID = """
            %s
              Temporal commands or the decisions around them changed without a new change ID, e.g. if (isVersion("my-change-v2", 1)) { ... }
            """;
    private static final String NEEDS_SAFE_CHANGE = """
            %s
              @Workflow class changed without touching its Temporal commands; mark it @TemporalSafeChange(reason = "...") if replay-safe
            """;
    private static final String NOT_VERIFIABLE = """
            %s
              Could not validate workflow: %s
            """;

    private WorkflowCompatibility() {
    }

    public static void verify() {
        verify(System.getProperty(BASE_PROPERTY, System.getenv().getOrDefault(BASE_ENV, "")));
    }

    public static void verify(String baseRef) {
        List<String> violations = violations(baseRef);
        if (!violations.isEmpty())
            throw new AssertionError("Workflow compatibility validation failed:\n" + String.join("\n", violations));
    }

    public static List<String> violations(String baseRef) {
        return violations(GitRepository.open(), baseRef);
    }

    static List<String> violations(GitRepository repository, String baseRef) {
        String base = !baseRef.isBlank() ? baseRef : repository.changedJavaFiles("HEAD").isEmpty() ? "HEAD~1" : "HEAD";
        return repository.changedJavaFiles(base).stream()
                .flatMap(file -> verify(repository, base, file).stream())
                .toList();
    }

    private static Optional<String> verify(GitRepository repository, String baseRef, Path file) {
        try {
            return verify(repository.show(baseRef, file), Files.readString(file)).map(message -> message.formatted(file));
        } catch (Exception e) {
            return Optional.of(NOT_VERIFIABLE.formatted(file, e.getMessage()));
        }
    }

    static Optional<String> verify(Optional<String> baseSource, String currentSource) {
        WorkflowSource current = WorkflowSource.parse(currentSource);
        if (!current.isWorkflow() || baseSource.isEmpty())
            return Optional.empty();
        WorkflowSource base = WorkflowSource.parse(baseSource.get());
        if (!base.isWorkflow())
            return Optional.empty();
        if (!base.skeleton().equals(current.skeleton()))
            return current.advancesVersionsOf(base) ? Optional.empty() : Optional.of(NEEDS_CHANGE_ID);
        return current.safeChangeReason().isPresent() ? Optional.empty() : Optional.of(NEEDS_SAFE_CHANGE);
    }
}
