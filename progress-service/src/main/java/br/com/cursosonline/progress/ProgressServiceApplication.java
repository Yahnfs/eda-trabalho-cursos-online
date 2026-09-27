package br.com.cursosonline.progress;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProgressServiceApplication {

    public static void main(String[] args) throws IOException {
        // O driver SQLite não cria diretórios: garantimos que a pasta do banco exista.
        Files.createDirectories(Path.of(System.getenv().getOrDefault("DATA_DIR", "data")));
        SpringApplication.run(ProgressServiceApplication.class, args);
    }
}
