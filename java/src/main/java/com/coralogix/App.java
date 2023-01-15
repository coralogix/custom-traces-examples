package com.coralogix;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public class App 
{
  public static void main( String[] args ) throws Exception
  {
    String endpoint = System.getenv("CX_ENDPOINT");
    if (!endpoint.startsWith("https://")) {
      endpoint = "https://" + endpoint;
    }

    String token = System.getenv("CX_TOKEN");

    Resource resource = Resource.getDefault()
      .merge(Resource.create(Attributes.builder()
      .put(ResourceAttributes.SERVICE_NAME, "logical-service-name")
      .put("cx.application.name", "app1")
      .put("cx.subsystem.name", "sub1").build()));
      
    SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(
          OtlpGrpcSpanExporter.builder()
          .setEndpoint(endpoint)
          .addHeader("Authorization", "Bearer " + token)
          .build())
        .build())
      .setResource(resource)
      .build();

    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
      .setTracerProvider(sdkTracerProvider)
      .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
      .buildAndRegisterGlobal();
      
    Tracer tracer =
      openTelemetry.getTracer("custom-tracing-java", "1.0.0");
    
    Span span = tracer.spanBuilder("span-main").startSpan();

    // Make the span the current span
    try (Scope scp = span.makeCurrent()) {
      
      span.addEvent("in scope");
      System.out.println("in scope!");
      
      Thread.sleep(100l);

    } finally {
      span.end();
    }

    sdkTracerProvider.shutdown();

    Thread.sleep(2000l);

    System.out.println("done");
  }
}
