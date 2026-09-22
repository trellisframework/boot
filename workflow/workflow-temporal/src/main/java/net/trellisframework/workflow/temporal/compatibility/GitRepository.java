package net.trellisframework.workflow.temporal.compatibility;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

record GitRepository(Path root) {

    static GitRepository open() {
        return new GitRepository(Path.of(git(null, "rev-parse", "--show-toplevel").require().trim()).toAbsolutePath().normalize());
    }

    List<Path> changedJavaFiles(String baseRef) {
        return git(root, "diff", "--name-only", "--diff-filter=d", baseRef, "--", "*.java").require().lines()
                .filter(line -> !line.isBlank())
                .map(root::resolve)
                .filter(Files::isRegularFile)
                .toList();
    }

    Optional<String> show(String ref, Path file) {
        String path = root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
        Result result = git(root, "show", ref + ":" + path);
        return result.exitCode() == 0 ? Optional.of(result.output()) : Optional.empty();
    }

    private static Result git(Path directory, String... arguments) {
        List<String> command = Stream.concat(Stream.of("git"), Stream.of(arguments)).toList();
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            if (directory != null)
                builder.directory(directory.toFile());
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            return new Result(command, process.waitFor(), output, error);
        } catch (IOException e) {
            throw new UncheckedIOException("Git command failed: " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Git command interrupted: " + String.join(" ", command), e);
        }
    }

    private record Result(List<String> command, int exitCode, String output, String error) {

        String require() {
            if (exitCode != 0)
                throw new IllegalStateException("Git command failed: " + String.join(" ", command) + "\n" + error);
            return output;
        }
    }
}
