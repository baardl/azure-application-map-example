package no.baardl.devops.applicationmap.postmanapi;

import com.azure.monitor.opentelemetry.exporter.AzureMonitorExporterBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.slf4j.LoggerFactory.getLogger;

public class ExternalRestSimulator implements Runnable {
    private static final Logger log = getLogger(ExternalRestSimulator.class);

    private final String instrumentationConnectionString;

    public ExternalRestSimulator(String instrumentationConnectionString) {
        this.instrumentationConnectionString = instrumentationConnectionString;
    }


    @Override
    public void run() {
        AutoConfiguredOpenTelemetrySdkBuilder sdkBuilder = AutoConfiguredOpenTelemetrySdk.builder();
        new AzureMonitorExporterBuilder()
                .connectionString(instrumentationConnectionString)
                .install(sdkBuilder);

        OpenTelemetry openTelemetry = sdkBuilder.build().getOpenTelemetrySdk();

        Tracer tracer = openTelemetry.getTracer("OpenTelemetrySample");
        Meter meter = openTelemetry.getMeter("OpenTelemetrySample");
        LongCounter echoCounter = meter.counterBuilder("PostmanEchoGet").build();

        for (int i = 0; i < 5; i++) {


            Span span = tracer.spanBuilder("ExernalRestSimulatorSpan").startSpan();
            final Scope scope = span.makeCurrent();
            try {
                span.setAttribute("appName", "sample-app");
                span.setAttribute("http.request.method", "GET");
                span.setAttribute("component", "http");
                // Thread bound (sync) calls will automatically pick up the parent span and you don't need to pass it explicitly.
                HttpRequest request = HttpRequest.newBuilder()
                        .header("Content-Type", "application/json")
                        .uri(new URI("https://postman-echo.com/get"))
                        .GET()
                        .build();

                echoCounter.add(1);

                HttpClient client = HttpClient.newBuilder().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                span.setAttribute("http.response.status_code", response.statusCode());
//                counter.add(1);
//                System.out.println(response.body());
                span.end();
                scope.close();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                span.end();
                scope.close();
            }
            try {
                log.debug("Sleeping for 1 second");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            log.debug("Sleeping for 10 seconds");
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
