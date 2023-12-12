package no.baardl.devops.applicationmap.iothub;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class AzureDeviceClient {
    private static final Logger log = getLogger(AzureDeviceClient.class);

    private final DeviceClient deviceClient;

    private boolean connectionEstablished = false;
    private boolean retryConnection;
    private String iotHubHostname = "";

    public AzureDeviceClient(String iotHubConnectionString) {
        ClientOptions clientOptions = ClientOptions.builder().keepAliveInterval(30).build();
        deviceClient = new DeviceClient(iotHubConnectionString, IotHubClientProtocol.MQTT_WS, clientOptions);
        if (deviceClient != null && deviceClient.getConfig() != null) {
            iotHubHostname = deviceClient.getConfig().getIotHubHostname();
        }
    }

    /*
        Intended for testing
         */
    protected AzureDeviceClient(DeviceClient deviceClient) {
        this.deviceClient = deviceClient;
        if (deviceClient != null && deviceClient.getConfig() != null) {
            iotHubHostname = deviceClient.getConfig().getIotHubHostname();
        }
    }

    public void openConnection() {
        try {
            deviceClient.open(retryConnection);
            connectionEstablished = true;
        } catch (IotHubClientException e) {
            //FIXME handle open connection errors.
            connectionEstablished = false;
            throw new RuntimeException(e);
        }
    }

    public void closeConnection() {
        if (deviceClient != null) {
            deviceClient.close();
            connectionEstablished = false;
        }
    }


    public void sendEventAsync(Message message, MessageSentCallback messageSentCallback) {
        try {
            deviceClient.sendEventAsync(message, messageSentCallback, message);
        } catch (Exception e) {
            log.error("Failed to send message to Azure IoT Hub", e);
            throw e;

        }
    }

    public boolean isConnectionEstablished() {
        return connectionEstablished;
    }

    public static void main(String[] args) {

    }
}
