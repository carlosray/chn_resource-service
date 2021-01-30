package ru.vas.resourceservice.service;

import org.springframework.messaging.Message;
import ru.vas.resourceservice.model.FileProcessingInfo;

import java.io.File;

public interface FileService {
    /**
     * Получить кол-во строк в файле
     * @param file файл
     * @return кол-во строк
     */
    long countOfLines(File file);

    /**
     * Переименовать (переместить файл)
     * @param message сообщение с файлом в хедерах
     * @param processedDir куда переместить
     * @return инфо
     */
    FileProcessingInfo renameFile(Message<?> message, String processedDir);
}
