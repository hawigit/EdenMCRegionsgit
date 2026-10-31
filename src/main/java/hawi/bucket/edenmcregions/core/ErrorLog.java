package hawi.bucket.edenmcregions.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A persistent error history, separate from Minecraft's per-session latest.log. */
public final class ErrorLog {
    private static final Logger LOG = LoggerFactory.getLogger("EdenMCRegions");
    private static final long MAX_BYTES = 1024 * 1024;
    private final Path file;

    public ErrorLog(Path file) { this.file = file; }

    public synchronized void report(String operation, Throwable error) {
        LOG.warn(operation, error);
        StringWriter details = new StringWriter();
        try (PrintWriter writer = new PrintWriter(details)) {
            writer.println("[" + Instant.now() + "] " + operation);
            error.printStackTrace(writer);
            writer.println();
        }
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(file) && Files.size(file) >= MAX_BYTES) {
                Files.move(file, file.resolveSibling("edenmcregions-errors.previous.txt"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(file, details.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception loggingError) {
            // A read-only or full disk must never prevent the mod from continuing.
            LOG.warn("Could not write EdenMCRegions error log to {}", file, loggingError);
        }
    }
}
