package com.abcbank.filestorage.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String FILE_EXCHANGE = "file.events.exchange";
    public static final String UPLOAD_QUEUE = "file.upload.queue";
    public static final String DELETE_QUEUE = "file.delete.queue";
    public static final String UPLOAD_ROUTING_KEY = "file.uploaded";
    public static final String DELETE_ROUTING_KEY = "file.deleted";

    @Bean
    public TopicExchange fileEventsExchange() {
        return new TopicExchange(FILE_EXCHANGE);
    }

    @Bean
    public Queue uploadQueue() {
        return new Queue(UPLOAD_QUEUE, true);
    }

    @Bean
    public Queue deleteQueue() {
        return new Queue(DELETE_QUEUE, true);
    }

    @Bean
    public Binding uploadBinding() {
        return BindingBuilder.bind(uploadQueue())
                .to(fileEventsExchange())
                .with(UPLOAD_ROUTING_KEY);
    }

    @Bean
    public Binding deleteBinding() {
        return BindingBuilder.bind(deleteQueue())
                .to(fileEventsExchange())
                .with(DELETE_ROUTING_KEY);
    }
}
