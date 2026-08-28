package com.abcbank.filestorage.services;

import com.abcbank.filestorage.config.RabbitMQConfig;
import com.abcbank.filestorage.events.FileEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FileEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public FileEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUpload(Long fileId, String originalName, long size) {
        FileEvent event = new FileEvent(
            UUID.randomUUID().toString(),
            "FILE_UPLOADED",
            fileId,
            originalName,
            size,
            LocalDateTime.now()
        );
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.FILE_EXCHANGE,
            RabbitMQConfig.UPLOAD_ROUTING_KEY,
            event
        );
    }

    public void publishDelete(Long fileId) {
        FileEvent event = new FileEvent(
            UUID.randomUUID().toString(),
            "FILE_DELETED",
            fileId,
            null,
            0,
            LocalDateTime.now()
        );
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.FILE_EXCHANGE,
            RabbitMQConfig.DELETE_ROUTING_KEY,
            event
        );
    }
}
