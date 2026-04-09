package com.adam.agri.planner.agent.execution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Actuator for file system operations in code generation.
 * Implements PhysicalExecutor.Actuator interface.
 */
public class FileActuator implements PhysicalExecutor.Actuator {
    private final Path codebaseRoot;
    private final boolean createIntermediateDirs;

    public FileActuator(Path codebaseRoot) {
        this(codebaseRoot, true);
    }

    public FileActuator(Path codebaseRoot, boolean createIntermediateDirs) {
        this.codebaseRoot = codebaseRoot;
        this.createIntermediateDirs = createIntermediateDirs;
    }

    @Override
    public String getType() {
        return "file_actuator";
    }

    @Override
    public boolean supports(String actionType) {
        return actionType.equals("create_package") ||
               actionType.equals("write_code") ||
               actionType.equals("create_directory") ||
               actionType.equals("write_file");
    }

    @Override
    public PhysicalExecutor.ActuatorResult execute(PhysicalExecutor.ActuatorCommand command, long timeoutMillis) {
        String actionType = command.getActuatorType();
        Object params = command.parameters();

        try {
            // Handle typed file commands
            if (params instanceof FileActuatorCommand fileCmd) {
                return switch (fileCmd.getOperation()) {
                    case CREATE_PACKAGE -> executeCreatePackage(fileCmd.getPackageName());
                    case WRITE_CODE -> executeWriteCode(
                        fileCmd.getTargetPath(),
                        fileCmd.getContent(),
                        fileCmd.isCreateIntermediateDirs()
                    );
                    case CREATE_DIRECTORY -> executeCreateDirectory(fileCmd.getTargetPath());
                };
            }

            // Fallback: try to extract from generic parameters
            return switch (actionType) {
                case "create_package" -> executeCreatePackage(extractPackageName(params));
                case "write_code" -> executeWriteCode(extractPath(params), extractContent(params), true);
                default -> new PhysicalExecutor.ActuatorResult(false, "Unknown operation: " + actionType, null);
            };

        } catch (Exception e) {
            return new PhysicalExecutor.ActuatorResult(false, "Execution failed: " + e.getMessage(), null);
        }
    }

    @Override
    public void emergencyStop() {
        // Nothing to stop for file operations
    }

    private PhysicalExecutor.ActuatorResult executeCreatePackage(String packageName) throws IOException {
        if (packageName == null || packageName.isEmpty()) {
            return new PhysicalExecutor.ActuatorResult(false, "Package name is null or empty", null);
        }

        String packagePath = packageName.replace('.', '/');
        Path targetDir = codebaseRoot.resolve(packagePath);

        Files.createDirectories(targetDir);

        return new PhysicalExecutor.ActuatorResult(true, null, targetDir.toString());
    }

    private PhysicalExecutor.ActuatorResult executeWriteCode(String filePath, String content, boolean createDirs) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            return new PhysicalExecutor.ActuatorResult(false, "File path is null or empty", null);
        }

        Path targetPath = codebaseRoot.resolve(filePath);

        if (createDirs) {
            Path parent = targetPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        }

        Files.writeString(targetPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return new PhysicalExecutor.ActuatorResult(true, null, targetPath.toString());
    }

    private PhysicalExecutor.ActuatorResult executeCreateDirectory(String dirPath) throws IOException {
        if (dirPath == null || dirPath.isEmpty()) {
            return new PhysicalExecutor.ActuatorResult(false, "Directory path is null or empty", null);
        }

        Path targetPath = codebaseRoot.resolve(dirPath);
        Files.createDirectories(targetPath);

        return new PhysicalExecutor.ActuatorResult(true, null, targetPath.toString());
    }

    // Helper methods to extract from parameters
    private String extractPackageName(Object params) {
        if (params instanceof String pkg) {
            return pkg;
        }
        return null;
    }

    private String extractPath(Object params) {
        if (params instanceof String path) {
            return path;
        }
        return null;
    }

    private String extractContent(Object params) {
        return "";
    }

    /**
     * Create a command for package creation.
     */
    public static FileActuatorCommand createPackageCommand(String packageName) {
        return new FileActuatorCommand(FileOperation.CREATE_PACKAGE, packageName, null, null, true);
    }

    /**
     * Create a command for writing code to a file.
     */
    public static FileActuatorCommand writeCodeCommand(String filePath, String content) {
        return new FileActuatorCommand(FileOperation.WRITE_CODE, null, filePath, content, true);
    }

    /**
     * Create a command for directory creation.
     */
    public static FileActuatorCommand createDirectoryCommand(String dirPath) {
        return new FileActuatorCommand(FileOperation.CREATE_DIRECTORY, null, dirPath, null, false);
    }

    /**
     * Command types for file operations.
     */
    public enum FileOperation {
        CREATE_PACKAGE,
        WRITE_CODE,
        CREATE_DIRECTORY
    }

    /**
     * Typed command for file actuator operations.
     */
    public static class FileActuatorCommand {
        private final FileOperation operation;
        private final String packageName;
        private final String targetPath;
        private final String content;
        private final boolean createIntermediateDirs;

        public FileActuatorCommand(FileOperation operation, String packageName,
                                   String targetPath, String content,
                                   boolean createIntermediateDirs) {
            this.operation = operation;
            this.packageName = packageName;
            this.targetPath = targetPath;
            this.content = content;
            this.createIntermediateDirs = createIntermediateDirs;
        }

        public FileOperation getOperation() {
            return operation;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getTargetPath() {
            return targetPath;
        }

        public String getContent() {
            return content;
        }

        public boolean isCreateIntermediateDirs() {
            return createIntermediateDirs;
        }
    }
}
