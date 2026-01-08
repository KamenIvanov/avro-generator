package com.pe.plugin.avro;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.avro.AvroMapper;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import com.fasterxml.jackson.dataformat.avro.jsr310.AvroJavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.pe.plugin.avro.schema.CustomAvroSchemaGenerator;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Mojo(name = "generate",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public class AvroGenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    @Parameter(defaultValue = AvroPluginConstants.DEFAULT_SCHEMA_DIR)
    String outputDir;

    @Parameter(required = true)
    String schemaFileName;

    @Parameter(required = true)
    String className;

    @Override
    public void execute() throws MojoExecutionException {
        final AvroMapper avroMapper = newAvroMapper();
        final Path outputDirPath = Paths.get(outputDir);
        final Path outputFile = outputDirPath.resolve(schemaFileName);

        try {
            final Class<?> dtoClass = getClassLoader(project).loadClass(className);
            final AvroSchema schema = generateSchema(avroMapper, dtoClass);
            final String schemaJson = schema.getAvroSchema().toString(true);

            Files.createDirectories(outputDirPath);
            Files.writeString(outputFile, schemaJson);
            getLog().info("Wrote " + outputFile);
        } catch (ReflectiveOperationException | IOException e) {
            throw new MojoExecutionException("Failed to write Avro schema " + outputFile, e);
        }
    }

    private ClassLoader getClassLoader(MavenProject project) throws MojoExecutionException {
        try {
            final List<String> classpathElements = project.getCompileClasspathElements();
            classpathElements.add(project.getBuild().getOutputDirectory());

            final URL[] urls = classpathElements.stream()
                    .map(element -> {
                        try {
                            return Paths.get(element).toUri().toURL();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toArray(URL[]::new);

            return new URLClassLoader(urls, getClass().getClassLoader());
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Unable to resolve project class loader", e);
        }
    }

    private AvroMapper newAvroMapper() {
        return AvroMapper.builder()
                .addModule(new AvroJavaTimeModule())
                .addModule(new ParameterNamesModule())
                .withConfigOverride(UUID.class, mutableConfigOverride -> {
                    mutableConfigOverride.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
                })
                .build();
    }

    private AvroSchema generateSchema(AvroMapper avroMapper, Class<?> clazz) throws JsonMappingException {
        final var gen = new CustomAvroSchemaGenerator();
        avroMapper.acceptJsonFormatVisitor(clazz, gen);
        return gen.getGeneratedSchema();
    }
}
