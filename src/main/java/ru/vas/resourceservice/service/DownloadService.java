package ru.vas.resourceservice.service;

import java.io.File;
import java.io.IOException;

public interface DownloadService {
    File downloadFile() throws IOException;
}
