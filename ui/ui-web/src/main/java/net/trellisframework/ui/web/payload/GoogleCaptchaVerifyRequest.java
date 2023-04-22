package net.trellisframework.ui.web.payload;

import net.trellisframework.core.payload.Payload;

public class GoogleCaptchaVerifyRequest implements Payload {
    private String secret;

    private String response;

    private String remoteip;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getRemoteip() {
        return remoteip;
    }

    public void setRemoteip(String remoteip) {
        this.remoteip = remoteip;
    }

    public GoogleCaptchaVerifyRequest() {
    }

    public GoogleCaptchaVerifyRequest(String secret, String response, String remoteip) {
        this.secret = secret;
        this.response = response;
        this.remoteip = remoteip;
    }

    @Override
    public String toString() {
        return "GoogleCaptchaVerifyRequest{" +
                "secret='" + secret + '\'' +
                ", response='" + response + '\'' +
                ", remoteip='" + remoteip + '\'' +
                '}';
    }
}
