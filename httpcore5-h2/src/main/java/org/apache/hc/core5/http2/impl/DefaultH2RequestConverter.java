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

package org.apache.hc.core5.http2.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http2.H2MessageConverter;
import org.apache.hc.core5.http2.H2PseudoRequestHeaders;
import org.apache.hc.core5.util.TextUtils;

/**
 * HTTP/2 request converter.
 *
 * @since 5.0
 */
public final class DefaultH2RequestConverter implements H2MessageConverter<HttpRequest> {

    public final static DefaultH2RequestConverter INSTANCE = new DefaultH2RequestConverter();

    @Override
    public HttpRequest convert(final List<Header> headers) throws HttpException {
        String method = null;
        String scheme = null;
        String authority = null;
        String path = null;
        final List<Header> messageHeaders = new ArrayList<>();

        for (int i = 0; i < headers.size(); i++) {
            final Header header = headers.get(i);
            final String name = header.getName();
            final String value = header.getValue();

            for (int n = 0; n < name.length(); n++) {
                final char ch = name.charAt(n);
                if (Character.isAlphabetic(ch) && !Character.isLowerCase(ch)) {
                    throw new ProtocolException("Header name '" + name + "' is invalid (header name contains uppercase characters)");
                }
            }

            if (name.startsWith(":")) {
                if (!messageHeaders.isEmpty()) {
                    throw new ProtocolException("Invalid sequence of headers (pseudo-headers must precede message headers)");
                }

                if (name.equals(H2PseudoRequestHeaders.METHOD)) {
                    if (method != null) {
                        throw new ProtocolException("Multiple '" + name + "' request headers are illegal");
                    }
                    method = value;
                } else if (name.equals(H2PseudoRequestHeaders.SCHEME)) {
                    if (scheme != null) {
                        throw new ProtocolException("Multiple '" + name + "' request headers are illegal");
                    }
                    scheme = value;
                } else if (name.equals(H2PseudoRequestHeaders.PATH)) {
                    if (path != null) {
                        throw new ProtocolException("Multiple '" + name + "' request headers are illegal");
                    }
                    path = value;
                } else if (name.equals(H2PseudoRequestHeaders.AUTHORITY)) {
                    authority = value;
                } else {
                    throw new ProtocolException("Unsupported request header '" + name + "'");
                }
            } else {
                if (name.equalsIgnoreCase(HttpHeaders.CONNECTION)) {
                    throw new ProtocolException("Header '" + header.getName() + ": " + header.getValue() + "' is illegal for HTTP/2 messages");
                }
                messageHeaders.add(header);
            }
        }
        if (method == null) {
            throw new ProtocolException("Mandatory request header ':method' not found");
        }
        if (method.equalsIgnoreCase("CONNECT")) {
            if (authority == null) {
                throw new ProtocolException("Header ':authority' is mandatory for CONNECT request");
            }
            if (scheme != null) {
                throw new ProtocolException("Header ':scheme' must not be set for CONNECT request");
            }
            if (path != null) {
                throw new ProtocolException("Header ':path' must not be set for CONNECT request");
            }
        } else {
            if (scheme == null) {
                throw new ProtocolException("Mandatory request header ':scheme' not found");
            }
            if (path == null) {
                throw new ProtocolException("Mandatory request header ':path' not found");
            }
        }

        final HttpRequest httpRequest = new BasicHttpRequest(method, path);
        httpRequest.setVersion(HttpVersion.HTTP_2);
        httpRequest.setScheme(scheme);
        httpRequest.setAuthority(authority);
        httpRequest.setPath(path);
        for (int i = 0; i < messageHeaders.size(); i++) {
            httpRequest.addHeader(messageHeaders.get(i));
        }
        return httpRequest;
    }

    @Override
    public List<Header> convert(final HttpRequest message) throws HttpException {
        if (TextUtils.isBlank(message.getMethod())) {
            throw new ProtocolException("Request method is empty");
        }
        final boolean optionMethod = "CONNECT".equalsIgnoreCase(message.getMethod());
        if (optionMethod) {
            if (TextUtils.isBlank(message.getAuthority())) {
                throw new ProtocolException("CONNECT request authority is not set");
            }
            if (message.getPath() != null) {
                throw new ProtocolException("CONNECT request path must be null");
            }
        } else {
            if (TextUtils.isBlank(message.getScheme())) {
                throw new ProtocolException("Request scheme is not set");
            }
            if (TextUtils.isBlank(message.getPath())) {
                throw new ProtocolException("Request path is not set");
            }
        }
        final List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader(H2PseudoRequestHeaders.METHOD, message.getMethod(), false));
        if (optionMethod) {
            headers.add(new BasicHeader(H2PseudoRequestHeaders.AUTHORITY, message.getAuthority(), false));
        }  else {
            headers.add(new BasicHeader(H2PseudoRequestHeaders.SCHEME, message.getScheme(), false));
            if (message.getAuthority() != null) {
                headers.add(new BasicHeader(H2PseudoRequestHeaders.AUTHORITY, message.getAuthority(), false));
            }
            headers.add(new BasicHeader(H2PseudoRequestHeaders.PATH, message.getPath(), false));
        }

        for (final Iterator<Header> it = message.headerIterator(); it.hasNext(); ) {
            final Header header = it.next();
            final String name = header.getName();
            final String value = header.getValue();
            if (name.startsWith(":")) {
                throw new ProtocolException("Header name '" + name + "' is invalid");
            }
            if (name.equalsIgnoreCase(HttpHeaders.CONNECTION)) {
                throw new ProtocolException("Header '" + name + ": " + value + "' is illegal for HTTP/2 messages");
            }
            headers.add(new BasicHeader(name.toLowerCase(Locale.ROOT), value));
        }

        return headers;
    }

}
