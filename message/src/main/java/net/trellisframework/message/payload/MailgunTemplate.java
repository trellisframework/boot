package net.trellisframework.message.payload;

public interface MailgunTemplate {
    String getName();

    String getDomain();

    String getFrom();

    String getSubject();
}
