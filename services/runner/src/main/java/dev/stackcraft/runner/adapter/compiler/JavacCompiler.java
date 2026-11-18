package dev.stackcraft.runner.adapter.compiler;

import dev.stackcraft.runner.port.Compiler;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Adapter: compiles with the JDK's in-process javac ({@link ToolProvider}). In-process is about 10x faster than
 * spawning {@code javac}. Annotation processing is off ({@code -proc:none}) so a snippet cannot run code at compile time.
 */
public class JavacCompiler implements Compiler {

    private final JavaCompiler javac;

    public JavacCompiler() {
        this.javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            throw new IllegalStateException("No system Java compiler: run the runner on a JDK, not a JRE");
        }
    }

    @Override
    public CompileOutcome compile(Path workDir, String className, String source) {
        Path file = workDir.resolve(className + ".java");
        try {
            Files.writeString(file, source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = javac.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            List<String> options = List.of("-d", workDir.toString(), "-proc:none", "-encoding", "UTF-8",
                    "-implicit:none", "-Xlint:none", "-nowarn");
            Boolean ok = javac.getTask(null, files, diagnostics, options, null, files.getJavaFileObjects(file)).call();
            return Boolean.TRUE.equals(ok) ? CompileOutcome.ok() : CompileOutcome.failed(format(className, diagnostics));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Formats like javac's CLI but without the temp-folder path, e.g. {@code Main.java:3: error: ';' expected}. */
    private static String format(String className, DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder sb = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            if (d.getKind() != Diagnostic.Kind.ERROR) continue;
            sb.append(className).append(".java:").append(d.getLineNumber()).append(": error: ")
              .append(d.getMessage(Locale.ROOT)).append('\n');
        }
        return sb.toString();
    }
}
