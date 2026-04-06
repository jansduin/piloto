package com.piloto.cdi.kernel.tools.impl;

import com.piloto.cdi.kernel.tools.BaseTool;
import com.piloto.cdi.kernel.tools.ToolCapability;
import com.piloto.cdi.kernel.tools.ToolDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogAnalyzerTool extends BaseTool {

    private static final String DEFAULT_LOG_PATH = ".piloto-data/logs/piloto.log";

    public LogAnalyzerTool() {
        super(ToolDefinition.create(
                "log_analyzer",
                "1.0",
                "Analiza los logs del sistema para detectar errores, excepciones y trazas de ejecución.",
                List.of(ToolCapability.READ, ToolCapability.SYSTEM),
                Map.of(
                        "lines", "Integer: Número de líneas a leer desde el final (default: 100)",
                        "filter", "String: Palabra clave para filtrar (ej: 'ERROR', 'EXCEPTION')"),
                Map.of(
                        "logs", "String: Contenido de los logs encontrados",
                        "error_count", "Integer: Número de errores detectados en la muestra"),
                false // No requiere aprobación humana por ser de lectura y diagnóstico interno
        ));
    }

    @Override
    public CompletableFuture<Map<String, Object>> execute(Map<String, Object> payload) {
        return CompletableFuture.supplyAsync(() -> {
            int linesToRead = (payload.containsKey("lines")) ? ((Number) payload.get("lines")).intValue() : 100;
            String filter = (String) payload.get("filter");

            Path logPath = Paths.get(DEFAULT_LOG_PATH);

            if (!Files.exists(logPath)) {
                return Map.of("error", "Log file not found at " + DEFAULT_LOG_PATH, "logs", "");
            }

            try (Stream<String> stream = Files.lines(logPath)) {
                List<String> allLines = stream.collect(Collectors.toList());
                int size = allLines.size();
                int start = Math.max(0, size - linesToRead);

                List<String> selectedLines = allLines.subList(start, size);

                if (filter != null && !filter.isBlank()) {
                    String finalFilter = filter.toUpperCase();
                    selectedLines = selectedLines.stream()
                            .filter(line -> line.toUpperCase().contains(finalFilter))
                            .collect(Collectors.toList());
                }

                long errorCount = selectedLines.stream()
                        .filter(line -> line.toUpperCase().contains("ERROR")
                                || line.toUpperCase().contains("EXCEPTION"))
                        .count();

                StringBuilder sb = new StringBuilder();
                for (String line : selectedLines) {
                    sb.append(line).append("\n");
                }

                Map<String, Object> result = new HashMap<>();
                result.put("logs", sb.toString());
                result.put("error_count", (int) errorCount);
                result.put("total_lines_scanned", selectedLines.size());

                return result;

            } catch (IOException e) {
                return Map.of("error", "Error reading log file: " + e.getMessage());
            }
        });
    }
}
