package io.github.santannaf.kafka.annotation;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;

import java.lang.annotation.Annotation;

public class TunedKafkaListener implements KafkaListener {
  private final String id;
  private final String containerFactory;
  private final String[] topics;
  private final String topicPattern;
  private final TopicPartition[] topicPartitions;
  private final String containerGroup;
  private final String errorHandler;
  private final String groupId;
  private final boolean idIsGroup;
  private final String clientIdPrefix;
  private final String beanRef;
  private final String concurrency;
  private final String autoStartup;
  private final String[] properties;
  private final boolean splitIterables;
  private final String contentTypeConverter;
  private final String batch;
  private final String filter;
  private final String info;

  public TunedKafkaListener(KafkaListener kafkaListener) {
    this.id = kafkaListener.id();
    this.containerFactory = kafkaListener.containerFactory();
    this.topics = kafkaListener.topics();
    this.topicPattern = kafkaListener.topicPattern();
    this.topicPartitions = kafkaListener.topicPartitions();
    this.containerGroup = kafkaListener.containerGroup();
    this.errorHandler = kafkaListener.errorHandler();
    this.groupId = kafkaListener.groupId();
    this.idIsGroup = kafkaListener.idIsGroup();
    this.clientIdPrefix = kafkaListener.clientIdPrefix();
    this.beanRef = kafkaListener.beanRef();
    this.concurrency = kafkaListener.concurrency();
    this.autoStartup = kafkaListener.autoStartup();
    this.properties = kafkaListener.properties();
    this.splitIterables = kafkaListener.splitIterables();
    this.contentTypeConverter = kafkaListener.contentTypeConverter();
    this.batch = kafkaListener.batch();
    this.filter = kafkaListener.filter();
    this.info = kafkaListener.info();
  }

  @Override
  public String id() {
    return this.id;
  }

  @Override
  public String containerFactory() {
    return this.containerFactory;
  }

  @Override
  public String[] topics() {
    return this.topics;
  }

  @Override
  public String topicPattern() {
    return this.topicPattern;
  }

  @Override
  public TopicPartition[] topicPartitions() {
    return this.topicPartitions;
  }

  @Override
  public String containerGroup() {
    return this.containerGroup;
  }

  @Override
  public String errorHandler() {
    return this.errorHandler;
  }

  @Override
  public String groupId() {
    return this.groupId;
  }

  @Override
  public boolean idIsGroup() {
    return this.idIsGroup;
  }

  @Override
  public String clientIdPrefix() {
    return this.clientIdPrefix;
  }

  @Override
  public String beanRef() {
    return this.beanRef;
  }

  @Override
  public String concurrency() {
    return this.concurrency;
  }

  @Override
  public String autoStartup() {
    return this.autoStartup;
  }

  @Override
  public String[] properties() {
    return this.properties;
  }

  @Override
  public boolean splitIterables() {
    return this.splitIterables;
  }

  @Override
  public String contentTypeConverter() {
    return this.contentTypeConverter;
  }

  @Override
  public String batch() {
    return this.batch;
  }

  @Override
  public String filter() {
    return this.filter;
  }

  @Override
  public String info() {
    return this.info;
  }

  @Override
  public String containerPostProcessor() {
    return "";
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return KafkaListener.class;
  }
}
