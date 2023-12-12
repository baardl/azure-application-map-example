package no.baardl.devops.applicationmap;

import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageSentCallback;
import com.microsoft.azure.sdk.iot.device.MessageType;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import no.baardl.devops.applicationmap.azuretable.AzureTableClient;
import no.baardl.devops.applicationmap.iothub.AzureDeviceClient;
import no.baardl.devops.applicationmap.postmanapi.ExternalRestSimulator;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Hello world!
 */
public class Application {
    private static final Logger log = getLogger(Application.class);


    public static void main(String[] args) {
        //Read local.properties into properties
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("./local.properties")) {
            properties.load(input);
        } catch (Exception e) {
            log.error("Could not load local.properties", e);
            System.exit(1);
        }
        Application application = new Application();
        String instrumentationConnectionString = properties.getProperty("instrumentation.connectionString");
        if (hasValue(instrumentationConnectionString)) {
            application.runExternalRestSimulator(instrumentationConnectionString);
        } else {
            log.warn("Missing instrumentation.connectionString in local.properties. Will not run ExternalRestSimulator");
        }

        String azureTableConnectionString = properties.getProperty("azureTable.connectionString");
        String tableName = properties.getProperty("azureTable.tableName");
        if (hasValue(azureTableConnectionString) && hasValue(tableName)) {
            application.importAzureTable(azureTableConnectionString, tableName);
        } else {
            log.warn("Missing azureTable.connectionString or azureTable.tableName in local.properties. Will not import AzureTable");
        }

        String iotHubConnectionString = properties.getProperty("iotHub.connectionString");
        if (hasValue(iotHubConnectionString)) {
            application.runIotHubClient(iotHubConnectionString);
        } else {
            log.warn("Missing iotHub.connectionString in local.properties. Will not run IotHubClient");
        }

        log.info("Metrics are sent to Azure every minute as default. Will sleep for 2 minutes.");
        int i = 0;
        do {
            try {
                Thread.sleep(10000);
                i++;
                log.info("Still here... {}/12. Ensuring metrics has been sent to Azure.", i);
            } catch (InterruptedException e) {
                log.error("Interrupted", e);
            }
        } while (i < 12);

        log.info("Done");
    }

    private void runIotHubClient(String iotHubConnectionString) {
        AzureDeviceClient azureDeviceClient = new AzureDeviceClient(iotHubConnectionString);
        azureDeviceClient.openConnection();
        for (int i = 0; i < 5; i++) {
            Message telemetryMessage = buildTelemetryMessage("TestSensor", i);
            azureDeviceClient.sendEventAsync(telemetryMessage, new MessageSentCallback() {
                @Override
                public void onMessageSent(Message message, IotHubClientException e, Object o) {
                    if (e != null) {
                        log.error("Error sending message", e);
                    } else {
                        log.info("Message sent: {}", message);
                    }
                }
            });
            try {
                log.info("Sleeping 1 second");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Interrupted", e);
            }
        }
        azureDeviceClient.closeConnection();
    }

    private Message buildTelemetryMessage(String sensorId, int i) {
        Integer temperature = getRandomNumber(15, 25);
        String observationJson = "{\"runNumber\": " + i + "," +
                " \"sensorId\": \"" + sensorId + "\"," +
                " \"temperature\": " + temperature + "}";
        Message telemetryMessage = new Message(observationJson);
        String messageId = UUID.randomUUID().toString();
        telemetryMessage.setMessageId(messageId);
        telemetryMessage.setMessageType(MessageType.DEVICE_TELEMETRY);
        telemetryMessage.setContentEncoding(StandardCharsets.UTF_8.name());
        telemetryMessage.setContentType("application/json");
        return telemetryMessage;
    }

    public int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    public void runExternalRestSimulator(String instrumentationConnectionString) {
        ExternalRestSimulator externalRestSimulator = new ExternalRestSimulator(instrumentationConnectionString);
        Thread thread = new Thread(externalRestSimulator);
        thread.start();
    }

    public void importAzureTable(String azureTableConnectionString, String tableName) {
        AzureTableClient tableClient = new AzureTableClient(azureTableConnectionString, tableName);
        List<Map<String, String>> rows = tableClient.listRows("1");
        for (Map<String, String> row : rows) {
            log.trace("Row: {}", row);
        }
    }

    public static boolean hasValue(String value) {
        return value != null && !value.isEmpty();
    }
}
