# azure-application-map-example
Application sending Distributed Tracing to Azure Application Insights to build a Applcatin Map

## Download prerequisites
```
curl -o applicationinsights-agent-3.4.18.jar https://repo1.maven.org/maven2/com/microsoft/azure/applicationinsights-agent/3.4.18/applicationinsights-agent-3.4.18.jar
```

## Build
```
mvn clean package
```

## Configure
TODO need more documentation from Azure Application Insights GUI.
Edit the file `applicationinsights.json` to match your Application Insights Instrumentation Key.
Edit the file `local.propererties` with connection strings. 

## Run
```
./run_application.sh
```

