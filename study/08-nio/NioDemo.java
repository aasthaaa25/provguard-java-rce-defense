import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * TOPIC: java.nio.file (Path, Files), reading/writing text, try-with-resources
 * over a stream.
 *
 * WHY IT MATTERS: ProvGuard's planned TraceWriter persists provenance events
 * to disk using exactly this API (Path + Files), not the older java.io File.
 */
public class NioDemo {

    public static void main(String[] args) throws IOException {
        Path tempFile = Files.createTempFile("provguard-nio-demo-", ".txt");
        try {
            List<String> lines = List.of(
                    "sinkType=PROCESS_EXECUTION",
                    "thread=main",
                    "stack=DemoMain#main -> ProcessBuilder#start"
            );

            Files.write(tempFile, lines, StandardCharsets.UTF_8);
            System.out.println("wrote " + lines.size() + " lines to " + tempFile.getFileName());

            // try-with-resources over a Stream<String> - must be closed to release the file handle.
            try (var stream = Files.lines(tempFile, StandardCharsets.UTF_8)) {
                stream.forEach(line -> System.out.println("read: " + line));
            }

            System.out.println("file size: " + Files.size(tempFile) + " bytes");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
