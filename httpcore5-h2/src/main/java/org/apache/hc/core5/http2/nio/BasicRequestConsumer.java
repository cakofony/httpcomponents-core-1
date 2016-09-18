/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.hc.core5.http2.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.util.Args;

/**
 * @since 5.0
 */
public class BasicRequestConsumer<T> implements AsyncRequestConsumer<Message<HttpRequest, T>> {

    private final AsyncEntityConsumer<T> dataConsumer;

    public BasicRequestConsumer(final AsyncEntityConsumer<T> dataConsumer) {
        this.dataConsumer = dataConsumer;
    }

    @Override
    public void consumeRequest(
            final HttpRequest request,
            final boolean enclosedBody,
            final FutureCallback<Message<HttpRequest, T>> resultCallback) throws HttpException, IOException {
        Args.notNull(request, "Request");
        Args.notNull(resultCallback, "Result callback");
        if (enclosedBody) {
            final Header header = request.getFirstHeader(HttpHeaders.CONTENT_TYPE);
            dataConsumer.streamStart(header != null ? header.getValue() : null, new FutureCallback<T>() {

                @Override
                public void completed(final T result) {
                    resultCallback.completed(new Message<>(request, result));
                }

                @Override
                public void failed(final Exception ex) {
                    resultCallback.failed(ex);
                }

                @Override
                public void cancelled() {
                    resultCallback.cancelled();
                }

            });
        } else {
            resultCallback.completed(new Message<>(request, (T) null));
        }
    }

    @Override
    public void updateCapacity(final CapacityChannel capacityChannel) throws IOException {
        dataConsumer.updateCapacity(capacityChannel);
    }

    @Override
    public void consume(final ByteBuffer src) throws IOException {
        dataConsumer.consume(src);
    }

    @Override
    public void streamEnd(final List<Header> trailers) throws HttpException, IOException {
        dataConsumer.streamEnd(trailers);
    }

    @Override
    public void releaseResources() {
        dataConsumer.releaseResources();
    }

}
