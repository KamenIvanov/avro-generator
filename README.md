# Avro Maven Plugin

This Maven plugin generates Avro schemas (`.avsc`) from Java classes. It uses Jackson's Avro dataformat module and supports Java 8 date/time types and standard Avro logical types.

## Features

- Generate Avro schemas from POJOs.
- Supports `java.time` types (via `AvroJavaTimeModule`).
- Correct handling of `UUID` using the standard Avro `uuid` logical type.
- Support for parameter names.

## Usage

Add the plugin to your `pom.xml`:

```xml
<plugin>
    <groupId>com.pe.plugin</groupId>
    <artifactId>avro-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <className>com.example.MyDataTransferObject</className>
                <schemaFileName>my-schema.avsc</schemaFileName>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Configuration

| Parameter | Type | Required | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- |
| `className` | `String` | Yes | - | Fully qualified name of the Java class to generate the schema from. |
| `schemaFileName` | `String` | Yes | - | Name of the generated schema file (e.g., `user.avsc`). |
| `outputDir` | `String` | No | `${basedir}/target/generated-sources/avsc` | Directory where the generated schema file will be saved. |

### Goals

- `generate`: Generates an Avro schema from a specified Java class. By default, it runs during the `process-classes` phase.

## Requirements

- Java 25 or higher.
- Maven 3.8.0 or higher.
