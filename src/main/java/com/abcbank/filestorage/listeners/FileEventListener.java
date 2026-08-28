package com.abcbank.filestorage.listeners;

import com.abcbank.filestorage.events.FileEvent;
import com.abcbank.filestorage.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FileEventListener {

    @RabbitListener(queues = RabbitMQConfig.UPLOAD_QUEUE)
    public void handleFileUploaded(FileEvent event) {
        System.out.println("Received file uploaded event: " + event);
    }

    @RabbitListener(queues = RabbitMQConfig.DELETE_QUEUE)
    public void handleFileDeleted(FileEvent event) {
        System.out.println("Received file deleted event: " + event);
    }
}
