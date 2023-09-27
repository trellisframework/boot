package net.trellisframework.message.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.trellisframework.core.payload.Payload;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ToString
public class SendMailByMailgunRequest implements Payload {
    private String apiKey;
    private String domain;
    private String from;
    private String to;
    private String subject;
    private String template;
    private Map<String, Object> variables;

    public static SendMailByMailgunRequest of(String apiKey, MailgunTemplate template, String to, Map<String, Object> variables) {
        return of(apiKey, template.getDomain(), template.getFrom(), to, template.getSubject(), template.getName(), variables);
    }
}
