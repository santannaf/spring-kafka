package io.github.santannaf.kafka.annotation;

import jakarta.annotation.Nonnull;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class KafkaAnalysisImportSelector implements ImportSelector {

  private static final String AUTO_CONFIGURATION_CLASS = "io.github.santannaf.kafka.autoconfigure.TunedKafkaAutoConfiguration";

  @Override
  @Nonnull
  public String[] selectImports(@Nonnull AnnotationMetadata metadata) {
    return new String[]{AUTO_CONFIGURATION_CLASS};
  }
}
