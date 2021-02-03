package ru.vas.resourceservice.integration.file;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.kafka.support.converter.KafkaMessageHeaders;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit4.SpringRunner;
import ru.vas.resourceservice.schedule.download.DownloadRegistryScheduler;
import ru.vas.resourceservice.service.DownloadService;
import ru.vas.resourceservice.service.FileService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(SpringRunner.class)
@SpringBootTest
@SpringIntegrationTest(noAutoStartup = "fileInboundChannelAdapter")
@EmbeddedKafka(partitions = 3, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class FileProcessingIntegrationFlowTest {
    @Autowired
    private SourcePollingChannelAdapter fileInboundChannelAdapter;
    @MockBean(name = "downloadScheduler")
    private DownloadRegistryScheduler downloadRegistryScheduler;
    @MockBean(name = "gitDownloadService")
    private DownloadService downloadService;
    @MockBean(name = IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
    private DirectChannel directChannel;
    @Autowired
    @Qualifier(value = KafkaMessageHeaders.REPLY_CHANNEL)
    private DirectChannel replyChannel;
    @SpyBean
    FileService fileService;

    @Value("${resource-service.registry.file-location}")
    private String tmpIn;
    @Value("${resource-service.registry.file-location}${resource-service.registry.processed-location}")
    private String tmpOut;

    private static final String FILE_SOURCE =
            "103.224.182.250 | 101.224.182.250;;;тестовое инфо 1;��-�17-079-210;2018-01-11\n" +
                    "103.246.200.0/22;;;тестовое инфо 2;9-Restricting;2017-04-28\n" +
                    "104.16.10.218;;;тестовое инфо 3;2-946/13;2013-06-10\n" +
                    "104.16.c100.59;;;тестовое инфо 4;2-6-27/2016-02-12-22-��;2016-04-11";

    @BeforeEach
    public void setUp() throws IOException {
        tearDown();
        this.fileInboundChannelAdapter.start();
    }

    @AfterEach
    public void tearDown() throws IOException {
        File outDir = new File(tmpOut);
        if (outDir.exists()) {
            FileUtils.cleanDirectory(outDir);
        }
        File inDir = new File(tmpIn);
        if (inDir.exists()) {
            FileUtils.cleanDirectory(inDir);
        }
    }

    @Test
    void fileReadingFlow() throws IOException, InterruptedException {
        //given
        ArgumentCaptor<Message<?>> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        File in = new File(tmpIn, "foo");
        FileOutputStream fos = new FileOutputStream(in);
        fos.write(FILE_SOURCE.getBytes());
        fos.close();
        //when
        in.renameTo(new File(tmpIn, "foo.csv"));
        Thread.sleep(5000);
        final Optional<Path> any = Files.walk(Paths.get(tmpOut))
                .filter(Files::isRegularFile)
                .filter(path -> path.toFile().getName().endsWith(".tmp"))
                .findAny();
        //then
        assertTrue(any.isPresent());
        final Path path = any.get();
        assertTrue(Files.exists(path));
        Mockito.verify(fileService).renameFile(argumentCaptor.capture(), anyString());
        final Message<?> message = argumentCaptor.getValue();
        assertTrue(message.getPayload() instanceof List);
        assertEquals(((List<String>) message.getPayload()).size(), 4);
    }

}