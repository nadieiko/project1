package edu.kai.stud;

import java.io.File;
import java.nio.file.Path;

public class Resource {
    private final String name;
    private final Path path;
    private final SecurityLevel securityLevel;
    private final ResourceType type;

    public Resource(String name, Path path, SecurityLevel securityLevel, ResourceType type) {
        this.name = name;
        this.path = path;
        this.securityLevel = securityLevel;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public ResourceType getType() {
        return type;
    }

    public boolean exists() {
        return new File(path.toString()).exists();
    }

    public enum ResourceType {
        TEXT_FILE,
        EXECUTABLE,
        IMAGE
    }
} 