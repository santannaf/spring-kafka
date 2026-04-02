package io.github.santannaf.kafka.configuration.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SslCertificateResolver {

  private static final Logger LOG = LoggerFactory.getLogger(SslCertificateResolver.class);
  private static final Path TEMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "kafka-ssl");

  /**
   * Modo interno: monta o nome do certificado a partir do certificateType
   * e extrai do classpath da lib para um temp file.
   *
   * @param certificateType tipo do certificado (ex: "minha_bu")
   * @param environment     ambiente (ex: "stg", "qa", "prod")
   * @param storeKind       "keystore" ou "truststore"
   */
  public static String resolveFromType(String certificateType, String environment, String storeKind) {
    var resourceName = String.format("%s.%s.xyz.ccc.%s.p12", certificateType, environment, storeKind);
    return extractFromClasspath(resourceName);
  }

  /**
   * Modo externo (open-source): resolve o path do cliente.
   * - Path absoluto (/opt/certs/x.p12) -> usa direto (filesystem)
   * - classpath:xxx -> extrai do classpath do cliente para temp file
   * - Nome simples -> tenta classpath, senao assume filesystem
   */
  public static String resolveFromPath(String path) {
    if (path == null) return null;

    if (path.startsWith("/") || path.startsWith("file:")) {
      return path.replace("file:", "");
    }

    if (path.startsWith("classpath:")) {
      return extractFromClasspath(path.replace("classpath:", ""));
    }

    try (var stream = Thread.currentThread()
      .getContextClassLoader()
      .getResourceAsStream(path)) {
      if (stream != null) {
        return extractFromClasspath(path);
      }
    }
    catch (IOException ignored) {
    }

    return path;
  }

  private static String extractFromClasspath(String resourcePath) {
    try (var inputStream = Thread.currentThread()
      .getContextClassLoader()
      .getResourceAsStream(resourcePath)) {

      if (inputStream == null) {
        throw new IllegalArgumentException("Certificate not found in classpath: " + resourcePath);
      }

      if (Files.notExists(TEMP_DIR)) {
        Files.createDirectories(TEMP_DIR);
      }

      var prefix = resourcePath.contains("truststore") ? "truststore" : "keystore";
      var tempFile = Files.createTempFile(TEMP_DIR, prefix, ".p12");

      try (OutputStream out = Files.newOutputStream(tempFile)) {
        out.write(inputStream.readAllBytes());
      }

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          Files.deleteIfExists(tempFile);
        }
        catch (IOException ignored) {
        }
      }));

      LOG.debug("SSL certificate '{}' extracted to '{}'", resourcePath, tempFile);
      return tempFile.toString();
    }
    catch (IOException e) {
      throw new UncheckedIOException("Failed to extract certificate: " + resourcePath, e);
    }
  }
}
