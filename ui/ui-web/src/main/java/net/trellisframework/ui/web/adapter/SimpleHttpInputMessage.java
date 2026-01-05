package net.trellisframework.ui.web.adapter;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

import java.io.InputStream;

class SimpleHttpInputMessage implements HttpInputMessage {
    private final InputStream body;
    private final HttpHeaders headers;
    
    SimpleHttpInputMessage(InputStream body, HttpHeaders headers) {
        this.body = body;
        this.headers = headers != null ? headers : new HttpHeaders();
    }
    
    @NotNull
    @Override
    public InputStream getBody() {
        return body;
    }
    
    @NotNull
    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }
}

