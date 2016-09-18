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

package org.apache.hc.core5.http.impl.nio;

import java.io.IOException;

import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.apache.hc.core5.http.nio.HttpAsyncExchange;
import org.apache.hc.core5.http.nio.HttpAsyncRequestConsumer;
import org.apache.hc.core5.http.nio.HttpAsyncRequestHandler;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Args;

/**
 * Basic implementation of {@link HttpAsyncRequestHandler} that delegates
 * the process of request handling to a {@link HttpRequestHandler}. Please note
 * that this handler buffers request content in memory and should be used for
 * relatively small request messages.
 *
 * @since 4.2
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE_CONDITIONAL)
public class BasicAsyncRequestHandler implements HttpAsyncRequestHandler<ClassicHttpRequest> {

    private final HttpRequestHandler handler;

    public BasicAsyncRequestHandler(final HttpRequestHandler handler) {
        super();
        Args.notNull(handler, "Request handler");
        this.handler = handler;
    }

    @Override
    public HttpAsyncRequestConsumer<ClassicHttpRequest> processRequest(final ClassicHttpRequest request,
                                                                       final HttpContext context) {
        return new BasicAsyncRequestConsumer();
    }

    @Override
    public void handle(
            final ClassicHttpRequest request,
            final HttpAsyncExchange httpexchange,
            final HttpContext context) throws HttpException, IOException {
        this.handler.handle(request, httpexchange.getResponse(), context);
        httpexchange.submitResponse();
    }

}
