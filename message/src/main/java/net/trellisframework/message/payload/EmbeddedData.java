package net.trellisframework.message.payload;

import net.trellisframework.core.payload.Payload;

public class EmbeddedData implements Payload {
    private String name;

    private byte[] data;

    private String mimeType;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public EmbeddedData() {
    }

    public EmbeddedData(String name, byte[] data, String mimeType) {
        this.name = name;
        this.data = data;
        this.mimeType = mimeType;
    }
}