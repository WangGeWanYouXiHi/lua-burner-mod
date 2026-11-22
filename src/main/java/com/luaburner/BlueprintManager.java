package com.luaburner;

import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scans the world save's {@code lua-blueprints/} folder for .lua files and
 * provides their contents for burning onto disks.
 */
public final class BlueprintManager {
    public static final String FOLDER = "lua-blueprints";

    /** A single blueprint file available for burning. */
    public record Blueprint(String name, long sizeBytes, String content, int lineCount) {}

    /** Ensure the blueprint folder exists and return its path. */
    public static Path ensureFolder(MinecraftServer server) {
        Path dir = server.getServerDirectory().resolve(FOLDER);
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir;
    }

    /** List all .lua blueprints, sorted by name. Each entry carries its full content. */
    public static List<Blueprint> list(MinecraftServer server) {
        Path dir = ensureFolder(server);
        List<Blueprint> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) return result;

        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".lua"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();

            for (Path p : files) {
                try {
                    byte[] bytes = Files.readAllBytes(p);
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    int lines = content.isEmpty() ? 0 : content.split("\n", -1).length;
                    result.add(new Blueprint(p.getFileName().toString(), bytes.length,
                            content, lines));
                } catch (IOException e) {
                    // Skip unreadable files.
                }
            }
        } catch (IOException e) {
            // Folder unreadable - return empty list.
        }
        return result;
    }

    /** Get a single blueprint by file name, or null if missing. */
    public static Blueprint get(MinecraftServer server, String name) {
        Path file = ensureFolder(server).resolve(name);
        if (!Files.isRegularFile(file)) return null;
        try {
            byte[] bytes = Files.readAllBytes(file);
            String content = new String(bytes, StandardCharsets.UTF_8);
            int lines = content.isEmpty() ? 0 : content.split("\n", -1).length;
            return new Blueprint(name, bytes.length, content, lines);
        } catch (IOException e) {
            return null;
        }
    }
}
