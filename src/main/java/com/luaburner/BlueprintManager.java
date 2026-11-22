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

    /** A single file available for burning. {@code data} is the raw bytes;
     *  {@code content} is a text preview (binary bytes shown as escape text). */
    public record Blueprint(String name, long sizeBytes, String content, int lineCount, byte[] data) {}

    /** Ensure the blueprint folder exists and return its path. */
    public static Path ensureFolder(MinecraftServer server) {
        Path dir = server.getServerDirectory().resolve(FOLDER);
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir;
    }

    /** Build a text preview from raw bytes. Invalid/binary bytes become dots so
     *  the preview never breaks, while the raw bytes are preserved separately. */
    private static String preview(byte[] bytes) {
        String s = new String(bytes, StandardCharsets.UTF_8);
        // Replace control chars (except \n and \t) with '.' for a safe preview.
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\t') out.append(c);
            else if (c < 0x20 || c == 0xFFFD) out.append('.');
            else out.append(c);
        }
        return out.toString();
    }

    /** List all regular files (any extension), sorted by name. */
    public static List<Blueprint> list(MinecraftServer server) {
        Path dir = ensureFolder(server);
        List<Blueprint> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) return result;

        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();

            for (Path p : files) {
                try {
                    byte[] bytes = Files.readAllBytes(p);
                    String content = preview(bytes);
                    int lines = content.isEmpty() ? 0 : content.split("\n", -1).length;
                    result.add(new Blueprint(p.getFileName().toString(), bytes.length,
                            content, lines, bytes));
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
            String content = preview(bytes);
            int lines = content.isEmpty() ? 0 : content.split("\n", -1).length;
            return new Blueprint(name, bytes.length, content, lines, bytes);
        } catch (IOException e) {
            return null;
        }
    }
}
