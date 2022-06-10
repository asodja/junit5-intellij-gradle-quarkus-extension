package org.acme;

import com.google.common.collect.ImmutableMap;
import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultSourceDir;
import io.quarkus.bootstrap.workspace.DefaultWorkspaceModule;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathCollection;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class modifies Quarkus Model.
 * It replaces Gradle classes location (build/ folder) to IntelliJ classes location (out/ folder).
 */
public class IdeGradleModelBuilder {

    private static final String INTELLIJ_MAIN_CLASSES = "out/production/classes".replaceAll("/", File.separator);
    private static final String INTELLIJ_TEST_CLASSES = "out/test/classes".replaceAll("/", File.separator);
    private static final String INTELLIJ_FIXTURES_CLASSES = "out/testFixtures/classes".replaceAll("/", File.separator);
    // We use src/main/resources for IntelliJ since sometimes it doesn't copy resources to out/production/resources
    private static final String INTELLIJ_MAIN_RESOURCES = "src/main/resources".replaceAll("/", File.separator);
    private static final String INTELLIJ_TEST_RESOURCES = "src/test/resources".replaceAll("/", File.separator);
    private static final String INTELLIJ_FIXTURES_RESOURCES = "src/testFixtures/resources".replaceAll("/", File.separator);

    private static final String GRADLE_MAIN_CLASSES = "build/classes/java/main".replaceAll("/", File.separator);
    private static final String GRADLE_TEST_CLASSES = "build/classes/java/test".replaceAll("/", File.separator);
    private static final String GRADLE_TEST_FIXTURES_CLASSES = "build/classes/java/testFixtures".replaceAll("/", File.separator);
    private static final String GRADLE_MAIN_RESOURCES = "build/resources/main".replaceAll("/", File.separator);
    private static final String GRADLE_TEST_RESOURCES = "build/resources/test".replaceAll("/", File.separator);
    private static final String GRADLE_TEST_FIXTURES_RESOURCES = "build/resources/testFixtures".replaceAll("/", File.separator);

    private static final Map<Object, Object> SOURCE_DIR_DATA = ImmutableMap.of("compiler", "compileJava");
    private static final Map<String, String> CLASSES_MAPPING;
    static {
        CLASSES_MAPPING = new LinkedHashMap<>();
        CLASSES_MAPPING.put(GRADLE_MAIN_CLASSES, INTELLIJ_MAIN_CLASSES);
        CLASSES_MAPPING.put(GRADLE_MAIN_CLASSES.replace("java", "kotlin"), INTELLIJ_MAIN_CLASSES);
        CLASSES_MAPPING.put(GRADLE_MAIN_RESOURCES, INTELLIJ_MAIN_RESOURCES);
        // test-fixtures must be before test otherwise replace will first grab test for build/classes/java/testFixtures
        CLASSES_MAPPING.put(GRADLE_TEST_FIXTURES_CLASSES, INTELLIJ_FIXTURES_CLASSES);
        CLASSES_MAPPING.put(GRADLE_TEST_FIXTURES_CLASSES.replace("java", "kotlin"), INTELLIJ_FIXTURES_CLASSES);
        CLASSES_MAPPING.put(GRADLE_TEST_FIXTURES_RESOURCES, INTELLIJ_FIXTURES_RESOURCES);
        CLASSES_MAPPING.put(GRADLE_TEST_CLASSES, INTELLIJ_TEST_CLASSES);
        CLASSES_MAPPING.put(GRADLE_TEST_CLASSES.replace("java", "kotlin"), INTELLIJ_TEST_CLASSES);
        CLASSES_MAPPING.put(GRADLE_TEST_RESOURCES, INTELLIJ_TEST_RESOURCES);
    }

    private static final String INTELLIJ_MODEL_PREFIX = "my.quarkus-test-model";
    private static final long INTELLIJ_MODEL_MAX_AGE_MS = TimeUnit.DAYS.toMillis(14);
    private static final String MODEL_VERSION = "2.10.x";

    public boolean isRunFromIdea(Path path) {
        return path.endsWith("out" + File.separator + "production" + File.separator + "classes");
    }

    public void loadModelForIdea(Path projectRoot) throws IOException, AppModelResolverException {
        System.setProperty(BootstrapConstants.SERIALIZED_TEST_APP_MODEL, loadModel(projectRoot));
    }

    private String loadModel(Path projectRoot) throws IOException, AppModelResolverException {
        // Out directory is IntelliJ build directory
        String outPath = projectRoot.toString() + File.separator + "out";
        removeOldModels(outPath);

        String workspaceId = getWorkspaceId();
        String modelPath = outPath + File.separator + INTELLIJ_MODEL_PREFIX + workspaceId + ".dat";
        if (modelExistsAndIsUpToDate(modelPath)) {
            // Cache model, so it is not recreated every time
            return modelPath;
        }

        ApplicationModel model = buildModel(projectRoot);
        serializeAppModel(model, modelPath);
        return modelPath;
    }

    private boolean modelExistsAndIsUpToDate(String modelPath) {
        return Files.exists(Paths.get(modelPath));
    }

    private ApplicationModel buildModel(Path projectRoot) throws IOException, AppModelResolverException {
        // BuildToolHelper calls Gradle and collects all dependencies. We also call gradle built-in task `dependencies`
        // that resolves all classpaths, since ApplicationModel requires resolved classpaths.
        ApplicationModel model = BuildToolHelper.enableGradleAppModel(projectRoot, "TEST", Collections.emptyList(), "dependencies");
        // Change all paths of dependency modules from Gradle to IntelliJ paths
        // First rewrite current module
        ResolvedDependency currentModule = rewriteCurrentModule(model);
        // Then rewrite dependencies
        // Gradle returns app dependencies as jars. So for example every module is referenced as jar.
        // But we want that it uses out/ folder (where IntelliJ compiles classes). So here for dependencies
        // that are modules we replace Jar references to module references, so to out/production/classes, out/production/resources
        // out/test/classes, out/test/resources, out/testFixtures/classes, out/testFixtures/resources
        String projectGroupId = currentModule.getGroupId();
        List<ResolvedDependency> dependencies = new ArrayList<>();
        for (ResolvedDependency dependency : model.getDependencies()) {
            if (isThisAnotherModule(projectGroupId, dependency)) {
                dependency = rewriteDependency(dependency);
            }
            dependencies.add(dependency);
        }
        ApplicationModelBuilder builder = new ApplicationModelBuilder()
                .addDependencies(dependencies)
                .setAppArtifact(currentModule)
                .setPlatformImports(model.getPlatforms());
        model.getExtensionCapabilities().forEach(builder::addExtensionCapabilities);
        model.getParentFirst().forEach(builder::addParentFirstArtifact);
        model.getLowerPriorityArtifacts().forEach(builder::addLesserPriorityArtifact);
        model.getRunnerParentFirst().forEach(builder::addRunnerParentFirstArtifact);
        model.getReloadableWorkspaceDependencies().forEach(builder::addReloadableWorkspaceModule);
        return builder.build();
    }

    private ResolvedDependency rewriteCurrentModule(ApplicationModel model) {
        return rewriteDependency(model.getAppArtifact());
    }

    private ResolvedDependency rewriteDependency(ResolvedDependency dependency) {
        WorkspaceModule module = dependency.getWorkspaceModule() != null
                ? rewriteModule(dependency.getWorkspaceModule())
                : null;
        PathCollection resolvedPaths = module == null
                ? dependency.getResolvedPaths()
                : collectCompiledPaths(module, dependency);
        return ResolvedDependencyBuilder.newInstance()
                .setResolvedPaths(resolvedPaths)
                .setGroupId(dependency.getGroupId())
                .setArtifactId(dependency.getArtifactId())
                .setClassifier(dependency.getClassifier())
                .setVersion(dependency.getVersion())
                .setScope(dependency.getScope())
                .setFlags(dependency.getFlags())
                .setWorkspaceModule(module)
                .build();
    }

    private PathCollection collectCompiledPaths(WorkspaceModule module, ResolvedDependency dependency) {
        ArtifactSources sources = module.getSources(dependency.getClassifier());
        PathsCollection.Builder paths = PathsCollection.builder();
        if (sources != null) {
            sources.getSourceDirs().stream()
                    .filter(SourceDir::isOutputAvailable)
                    .forEach(it -> paths.add(it.getOutputDir()));
            sources.getResourceDirs().stream()
                    .filter(SourceDir::isOutputAvailable)
                    .forEach(it -> paths.add(it.getOutputDir()));
        }
        return paths.build();
    }

    private WorkspaceModule rewriteModule(WorkspaceModule original) {
        WorkspaceModule.Mutable newModule = WorkspaceModule.builder()
                .setModuleId(original.getId())
                .setModuleDir(original.getModuleDir().toPath())
                .setBuildDir(new File(original.getModuleDir(), "out").toPath());
        original.getSourceClassifiers().forEach(it -> {
            ArtifactSources source = original.getSources(it);
            newModule.addArtifactSources(new DefaultArtifactSources(
                    source.getClassifier(),
                    rewriteSources(source.getSourceDirs()),
                    rewriteSources(source.getResourceDirs())
            ));
        });
        if (original.getDirectDependencies() != null) {
            newModule.setDependencies(new ArrayList<>(original.getDirectDependencies()));
        }
        if (original.getDirectDependencyConstraints() != null) {
            newModule.setDependencyConstraints(new ArrayList<>(original.getDirectDependencyConstraints()));
        }
        if (original.getTestClasspathDependencyExclusions() != null) {
            newModule.setTestClasspathDependencyExclusions(new ArrayList<>(original.getTestClasspathDependencyExclusions()));
        }
        if (original.getAdditionalTestClasspathElements() != null) {
            newModule.setAdditionalTestClasspathElements(new ArrayList<>(original.getAdditionalTestClasspathElements()));
        }
        if (!original.getBuildFiles().isEmpty()) {
            newModule.setBuildFile(original.getBuildFiles().getSinglePath());
        }
        return newModule;
    }

    private Collection<SourceDir> rewriteSources(Collection<SourceDir> sources) {
        // SOURCE_DIR_DATA is never used, but let's set it anyway just for the sake of completeness
        return sources.stream()
                .map(it -> new DefaultSourceDir(it.getDir(), rewritePath(it.getOutputDir()), SOURCE_DIR_DATA))
                .collect(Collectors.toList());
    }

    private Path rewritePath(Path path) {
        String destinationDir = path.toAbsolutePath().toString();
        for (Map.Entry<String, String> entry : CLASSES_MAPPING.entrySet()) {
            if (destinationDir.contains(entry.getKey())) {
                return Paths.get(destinationDir.replace(entry.getKey(), entry.getValue()));
            }
        }
        return path;
    }

    private boolean isThisAnotherModule(String groupId, ResolvedDependency dependency) {
        return dependency.getGroupId().equals(groupId) && dependency.getWorkspaceModule() != null;
    }

    private void serializeAppModel(ApplicationModel model, String outPath) throws IOException {
        final Path serializedModel = new File(outPath).toPath();
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(serializedModel))) {
            out.writeObject(BootstrapUtils.convert(model));
        }
    }

    private void removeOldModels(String outPath) {
        File outFile = new File(outPath);
        if (outFile.list() != null) {
            Arrays.stream(Objects.requireNonNull(new File(outPath).list()))
                    .filter(name -> name.startsWith(INTELLIJ_MODEL_PREFIX))
                    .map(File::new)
                    .filter(f -> new Date().getTime() - f.lastModified() > INTELLIJ_MODEL_MAX_AGE_MS)
                    .forEach(File::delete);
        }
    }

    private String getWorkspaceId() {
        // Classpath hash, so we can detect if dependencies has changed (some dependency was added/removed/changed)
        int classPathHash = (System.getProperty("java.version") + System.getProperty("java.class.path")).hashCode();
        return "version=" + MODEL_VERSION + "-workspaceHash=" + classPathHash;
    }
}
