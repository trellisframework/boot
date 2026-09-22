package net.trellisframework.workflow.temporal.compatibility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitRepositoryTest {
    private static final String BASE = """
            @Workflow
            public class OrderFlow implements BaseWorkflowAction {
                public void run(String orderId) {
                    call(LoadOrder.class, orderId);
                }
            }
            """;
    private static final String CHANGED = BASE.replace("orderId);", "orderId);\n        call(ChargeCard.class, orderId);");

    @TempDir
    Path root;
    Path workflow;
    GitRepository repository;

    @BeforeEach
    void commitBaseline() throws Exception {
        workflow = root.resolve("src/OrderFlow.java");
        Files.createDirectories(workflow.getParent());
        Files.writeString(workflow, BASE);
        git("init", "-q");
        commit("baseline");
        repository = new GitRepository(root.toRealPath());
    }

    @Test
    void listsChangedJavaFilesAndShowsBaseline() throws IOException {
        assertTrue(repository.changedJavaFiles("HEAD").isEmpty());
        Files.writeString(workflow, CHANGED);
        Files.writeString(root.resolve("README.md"), "changed");
        Files.writeString(root.resolve("untracked.txt"), "ignored");

        assertEquals(List.of(workflow.toRealPath()), repository.changedJavaFiles("HEAD"));
        assertEquals(BASE, repository.show("HEAD", workflow).orElseThrow());
        assertTrue(repository.show("HEAD", root.resolve("src/Missing.java")).isEmpty());
    }

    @Test
    void reportsUncommittedUnversionedWorkflowChange() throws IOException {
        Files.writeString(workflow, CHANGED);

        List<String> violations = WorkflowCompatibility.violations(repository, "");
        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().contains("change ID"));
    }

    @Test
    void reportsCommittedUnversionedWorkflowChangeAgainstPreviousCommit() throws Exception {
        Files.writeString(workflow, CHANGED);
        commit("unversioned change");
        Files.writeString(root.resolve("untracked.txt"), "ignored");

        assertEquals(1, WorkflowCompatibility.violations(repository, "").size());
        assertTrue(WorkflowCompatibility.violations(repository, "HEAD").isEmpty());
    }

    private void commit(String message) throws Exception {
        git("-c", "user.name=test", "-c", "user.email=test@example.com", "add", ".");
        git("-c", "user.name=test", "-c", "user.email=test@example.com", "commit", "-q", "-m", message);
    }

    private void git(String... arguments) throws Exception {
        List<String> command = Stream.concat(Stream.of("git"), Stream.of(arguments)).toList();
        Process process = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertEquals(0, process.waitFor(), output);
    }
}
