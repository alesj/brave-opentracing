/**
 * Copyright 2016-2017 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package brave.opentracing;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;

import io.opentracing.BaseSpan;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.tag.Tags;

/**
 * TODO
 */
public abstract class AbstractBraveSpan<S extends BaseSpan> implements BaseSpan<S>, Closeable {

  protected final brave.Span delegate;
  private final SpanContext context;

  protected AbstractBraveSpan(brave.Span delegate) {
    this.delegate = delegate;
    this.context = BraveSpanContext.wrap(delegate.context());
  }

  /**
   * Cast this to S.
   *
   * @return this as type S
   */
  protected abstract S cast();

  /**
   * {@inheritDoc}
   */
  @Override public SpanContext context() {
    return context;
  }

  /**
   * {@inheritDoc}
   */
  @Override public void close() {
    delegate.finish();
  }

  /**
   * {@inheritDoc}
   */
  @Override public S setTag(String key, String value) {
    delegate.tag(key, value);

    if (Tags.SPAN_KIND.getKey().equals(key) && Tags.SPAN_KIND_CLIENT.equals(value)) {
      delegate.kind(brave.Span.Kind.CLIENT);
    } else if (Tags.SPAN_KIND.getKey().equals(key) && Tags.SPAN_KIND_SERVER.equals(value)) {
      delegate.kind(brave.Span.Kind.SERVER);
    }
    return cast();
  }

  /**
   * {@inheritDoc}
   */
  @Override public S setTag(String key, boolean value) {
    return setTag(key, Boolean.toString(value));
  }

  /**
   * {@inheritDoc}
   */
  @Override public S setTag(String key, Number value) {
    return setTag(key, value.toString());
  }

  /**
   * {@inheritDoc}
   */
  @Override public S log(Map<String, ?> fields) {
    if (fields.isEmpty()) return cast();
    // in real life, do like zipkin-go-opentracing: "key1=value1 key2=value2"
    return log(toAnnotation(fields));
  }

  /**
   * {@inheritDoc}
   */
  @Override public S log(long timestampMicroseconds, Map<String, ?> fields) {
    if (fields.isEmpty()) return cast();
    // in real life, do like zipkin-go-opentracing: "key1=value1 key2=value2"
    return log(timestampMicroseconds, toAnnotation(fields));
  }

  /**
   * Converts a map to a string of form: "key1=value1 key2=value2"
   */
  static String toAnnotation(Map<String, ?> fields) {
    // special-case the "event" field which is similar to the semantics of a zipkin annotation
    Object event = fields.get("event");
    if (event != null && fields.size() == 1) return event.toString();

    return joinOnEqualsSpace(fields);
  }

  static String joinOnEqualsSpace(Map<String, ?> fields) {
    if (fields.isEmpty()) return "";

    StringBuilder result = new StringBuilder();
    for (Iterator<? extends Map.Entry<String, ?>> i = fields.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry<String, ?> next = i.next();
      result.append(next.getKey()).append('=').append(next.getValue());
      if (i.hasNext()) result.append(' ');
    }
    return result.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override public S log(String event) {
    delegate.annotate(event);
    return cast();
  }

  /**
   * {@inheritDoc}
   */
  @Override public S log(long timestampMicroseconds, String event) {
    delegate.annotate(timestampMicroseconds, event);
    return cast();
  }

  /**
   * {@inheritDoc}
   */
  @Override public S log(String eventName, Object ignored) {
    return log(eventName);
  }

  /**
   * {@inheritDoc}
   */
  @Override public S log(long timestampMicroseconds, String eventName, Object ignored) {
    return log(timestampMicroseconds, eventName);
  }

  /**
   * This is a NOOP as neither <a href="https://github.com/openzipkin/b3-propagation">B3</a>
   * nor Brave include baggage support.
   */
  // OpenTracing could one day define a way to plug-in arbitrary baggage handling similar to how
  // it has feature-specific apis like active-span
  @Override public S setBaggageItem(String key, String value) {
    // brave does not support baggage
    return cast();
  }

  /**
   * Returns null as neither <a href="https://github.com/openzipkin/b3-propagation">B3</a>
   * nor Brave include baggage support.
   */
  // OpenTracing could one day define a way to plug-in arbitrary baggage handling similar to how
  // it has feature-specific apis like active-span
  @Override public String getBaggageItem(String key) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override public S setOperationName(String operationName) {
    delegate.name(operationName);
    return cast();
  }
}
