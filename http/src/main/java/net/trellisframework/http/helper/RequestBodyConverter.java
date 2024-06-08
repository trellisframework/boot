package net.trellisframework.http.helper;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;

public final class RequestBodyConverter<T> implements Converter<T, RequestBody> {
    static final RequestBodyConverter<Object> PLAIN_TEXT_INSTANCE = new RequestBodyConverter<>(MediaType.get("text/plain; charset=UTF-8"));
    static final RequestBodyConverter<Object> JSON_INSTANCE = new RequestBodyConverter<>(MediaType.get("application/json; charset=UTF-8"));
    private final MediaType mediaType;

    private RequestBodyConverter(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public RequestBody convert(T value) {
        return RequestBody.create(mediaType, String.valueOf(value));
    }
}