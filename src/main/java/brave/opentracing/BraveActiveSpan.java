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

import brave.Span;
import brave.Tracer;
import io.opentracing.ActiveSpan;

/**
 *
 */
public final class BraveActiveSpan extends AbstractBraveSpan<ActiveSpan> implements ActiveSpan {

  private final brave.Tracer tracer;
  private final Tracer.SpanInScope spanInScope;

  public BraveActiveSpan(brave.Tracer tracer, Tracer.SpanInScope spanInScope, brave.Span delegate) {
    super(delegate);
    this.tracer = tracer;
    this.spanInScope = spanInScope;
  }

  @Override
  protected ActiveSpan cast() {
    return this;
  }

  @Override
  public void deactivate() {
    try {
      super.close();
    } finally {
      spanInScope.close();
    }
  }

  @Override
  public void close() {
    deactivate();
  }

  @Override
  public Continuation capture() {
    return () -> {
      Span span = tracer.nextSpan();
      return BraveTracer.makeActive(tracer, BraveSpan.wrap(span));
    };
  }
}
