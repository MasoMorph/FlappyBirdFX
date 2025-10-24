package com.fbo.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ResourceUtils {
    private ResourceUtils() {}

    public static Path copyResourceToTemp(String resourcePath, String suffix) {
        if (resourcePath == null) return null;
        try (InputStream is = ResourceUtils.class.getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            String suf = (suffix == null || suffix.isEmpty()) ? ".tmp" : suffix;
            Path tmp = Files.createTempFile("flappyfx-res-", suf);
            Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().deleteOnExit();
            return tmp;
        } catch (IOException e) {
            System.err.println("Failed to copy resource to temp: " + resourcePath + " -> " + e.getMessage());
            return null;
        }
    }
}
