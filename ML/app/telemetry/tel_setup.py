from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
import os

_tracer_initialized = False

def setup_telemetry():
    global _tracer_initialized
    if _tracer_initialized:
        return trace.get_tracer(__name__)

    endpoint = os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://otel-collector:4318/v1/traces")
    provider = TracerProvider()
    trace.set_tracer_provider(provider)

    exporter = OTLPSpanExporter(endpoint=endpoint)
    processor = BatchSpanProcessor(exporter)
    provider.add_span_processor(processor)

    _tracer_initialized = True
    return trace.get_tracer(__name__)
