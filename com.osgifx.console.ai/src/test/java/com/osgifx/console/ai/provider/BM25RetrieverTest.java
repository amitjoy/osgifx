package com.osgifx.console.ai.provider;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.osgifx.console.ai.GogoExample;

public class BM25RetrieverTest {

    private static final String EXAMPLES_FILE = "com.osgifx.console.ai/src/main/resources/gogo-examples.txt";

    public static void main(final String[] args) throws Exception {
        final var retriever = new SimpleGogoRetriever();

        // Manually parse examples and inject them, then build the index
        final var examplesField = SimpleGogoRetriever.class.getDeclaredField("examples");
        examplesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final var examples = (java.util.List<GogoExample>) examplesField.get(retriever);

        try (final var reader = new BufferedReader(new FileReader(EXAMPLES_FILE, StandardCharsets.UTF_8))) {
            String    line;
            String    currentPrompt = null;
            final var commandLines  = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#"))
                    continue;
                if (line.startsWith("P: ")) {
                    if (currentPrompt != null && !commandLines.isEmpty()) {
                        examples.add(new GogoExample(currentPrompt, commandLines.toString().trim()));
                    }
                    currentPrompt = line.substring(3).trim();
                    commandLines.setLength(0);
                } else if (line.startsWith("A: ")) {
                    commandLines.append(line.substring(3).trim());
                } else if (!line.isEmpty() && currentPrompt != null) {
                    commandLines.append("\n").append(line);
                }
            }
            if (currentPrompt != null && !commandLines.isEmpty()) {
                examples.add(new GogoExample(currentPrompt, commandLines.toString().trim()));
            }
        }

        System.out.println("Loaded " + examples.size() + " examples");

        // Build the BM25 index
        final Method buildIndex = SimpleGogoRetriever.class.getDeclaredMethod("buildIndex");
        buildIndex.setAccessible(true);
        buildIndex.invoke(retriever);

        // Test queries that previously failed or were tricky
        final String[] queries = { "find the configuration that has abc in its PID", "show all bundles",
                "find all scr components", "list all services containing EventAdmin", "delete a configuration",
                "show all system properties", "stop bundle with id 10", "find bundles related to http and restart them",
                "show all packages exported by bundle 5", "list the configurations whose PID contains xyz" };

        for (final var query : queries) {
            final var results = retriever.retrieve(query, 5);
            System.out.println("=== Query: \"" + query + "\" ===");
            for (var i = 0; i < results.size(); i++) {
                final var ex = results.get(i);
                System.out.printf("  %d. [%s] -> %s%n", i + 1, ex.intent(),
                        ex.commands().lines().collect(Collectors.joining(" | ")));
            }
            System.out.println();
        }
    }
}
