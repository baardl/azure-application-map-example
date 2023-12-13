package no.baardl.devops.applicationmap.iothub;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageSentCallback;
import com.microsoft.azure.sdk.iot.device.MessageType;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

public class TelemetryMessageDistributor implements Runnable {
    private static final Logger log = getLogger(TelemetryMessageDistributor.class);
    private final String iotHubConnectionString;
    private final String instrumentationConnectionString;

    public TelemetryMessageDistributor(String iotHubConnectionString, String instrumentationConnectionString) {
        this.iotHubConnectionString = iotHubConnectionString;
        this.instrumentationConnectionString = instrumentationConnectionString;
    }

    @Override
    public void run() {
//        runIotHubClient();
        runIotHubClientWithParent();
    }

    private void runIotHubClientWithParent() {


        log.info("Starting TelemetryMessageDistributor");
        /*
        AutoConfiguredOpenTelemetrySdkBuilder sdkBuilder = AutoConfiguredOpenTelemetrySdk.builder();
        new AzureMonitorExporterBuilder()
                .connectionString(instrumentationConnectionString)
                .install(sdkBuilder);

        OpenTelemetry openTelemetry = sdkBuilder.build().getOpenTelemetrySdk();

        log.info("OpenTelemetrySdk: {}", openTelemetry);
        */
//        Tracer tracer = GlobalOpenTelemetry.getTracer("AzureApplcationMap");
        final Tracer tracer = GlobalOpenTelemetry.getTracer("no.messom.lekerseg");
//        Tracer tracer = openTelemetry.getTracer("AzureApplcationMap");
        log.info("Tracer: {}", tracer);
//        Span span = tracer.spanBuilder("IotHubSendTelemetry").setSpanKind(SpanKind.CLIENT).startSpan();
        Span parentSpan = tracer.spanBuilder("IotHub").startSpan();
        try (Scope scope = parentSpan.makeCurrent()) {
            AzureDeviceClient azureDeviceClient = new AzureDeviceClient(iotHubConnectionString);
            azureDeviceClient.openConnection();
            for (int i = 0; i < 5; i++) {
                Span childSpan = tracer.spanBuilder("SendTelemetry").setSpanKind(SpanKind.CLIENT).startSpan();
                long startTime = System.currentTimeMillis();

                childSpan.setAttribute("appName", "IoTHubClient");
//            span.setAttribute("http.request.method", "GET");
//                childSpan.setAttribute("component", "PC");
//                childSpan.setAttribute("type", "PC");
                childSpan.setAttribute("server.address", "cludconnectorhub-test-west.azure-devices.net");
                childSpan.setAttribute("http.method", "GET");
                childSpan.setAttribute("http.url", "https://cludconnectorhub-test-west.azure-devices.net/");

                try(Scope childScope = childSpan.makeCurrent()) {
                    Message telemetryMessage = buildTelemetryMessage("TestSensor", i);

                    azureDeviceClient.sendEventAsync(telemetryMessage, new MessageSentCallback() {
                        @Override
                        public void onMessageSent(Message message, IotHubClientException e, Object o) {
                            long elapsedTime = System.currentTimeMillis() - startTime;
                            Duration duration = new Duration(elapsedTime);
                            RemoteDependencyTelemetry dependencyTelemetry = new RemoteDependencyTelemetry("IotHub", "SendEventAsync", duration, true);
                            dependencyTelemetry.setTarget("IotHubNorway.messom.no");
                            dependencyTelemetry.setType("PC"); //--> Device_Type=PC, need Client_Type=PC
                            //koble denne målingen til en eller annen "parent"
                            if (e != null) {
                                dependencyTelemetry.setSuccess(false);
                                childSpan.setStatus(StatusCode.ERROR, e.getMessage());
                                childSpan.setAttribute("http.response.status_code", "500");
                                childSpan.addEvent("Failed to send message");
                                log.error("Error sending message", e);
                            } else {
                                //telemetryClient.TrackDependency("myDependencyType", "myDependencyCall", "myDependencyData",  startTime, timer.Elapsed, success);
                                log.info("Message sent: {}", message);
                                childSpan.setStatus(StatusCode.OK, "Message sent");
                                childSpan.setAttribute("http.response.status_code", "200");
                                childSpan.addEvent("Message sent");
                            }
//                    telemetryClient.trackDependency(dependencyTelemetry);

//                            childSpan.end();
//                            scope.close();
                        }
                    });
                } finally {
                    childSpan.end();
                }
                try {
                    log.info("Sleeping 1 second");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.info("Interrupted", e);
                }
            }
            azureDeviceClient.closeConnection();
        } finally {
            parentSpan.end();
        }
//        telemetryClient.flush();
        try {
            log.info("Flushed, now sleeping 2 seconds");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            log.info("Interrupted", e);
        }

//        span.end();
    }

    private void runIotHubClient() {
        TelemetryClient telemetryClient = new TelemetryClient();
        AzureDeviceClient azureDeviceClient = new AzureDeviceClient(iotHubConnectionString);
        azureDeviceClient.openConnection();
        for (int i = 0; i < 5; i++) {
            long startTime = System.currentTimeMillis();
            Message telemetryMessage = buildTelemetryMessage("TestSensor", i);
            azureDeviceClient.sendEventAsync(telemetryMessage, new MessageSentCallback() {
                @Override
                public void onMessageSent(Message message, IotHubClientException e, Object o) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    Duration duration = new Duration(elapsedTime);
                    RemoteDependencyTelemetry dependencyTelemetry = new RemoteDependencyTelemetry("IotHub", "SendEventAsync", duration, true);
                    dependencyTelemetry.setTarget("IotHubNorway.messom.no");
                    dependencyTelemetry.setType("PC"); //--> Device_Type=PC, need Client_Type=PC
                    //koble denne målingen til en eller annen "parent"
                    if (e != null) {
                        dependencyTelemetry.setSuccess(false);
                        telemetryClient.trackDependency(dependencyTelemetry);
                        log.error("Error sending message", e);
                    } else {
                        //telemetryClient.TrackDependency("myDependencyType", "myDependencyCall", "myDependencyData",  startTime, timer.Elapsed, success);
                        telemetryClient.trackDependency(dependencyTelemetry);
                        log.info("Message sent: {}", message);
                    }
                }
            });
            try {
                log.info("Sleeping 1 second");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.info("Interrupted", e);
            }
        }
        telemetryClient.flush();
        try {
            log.info("Flushed, now sleeping 2 seconds");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            log.info("Interrupted", e);
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
}
