package net.trellisframework.message.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.trellisframework.core.payload.Payload;

import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ToString
public class SendMailRequest implements Payload {
    private String email;

    private String subject;

    private String body;

    private String imageName;

    private byte[] image;

    private String pdfName;

    private byte[] pdf;

    private List<EmbeddedData> embeddedData;

    public static SendMailRequest of(String email, String subject, String body) {
        return of(email, subject, body, null, null, null, null, null);
    }

    public static SendMailRequest of(String email, String subject, String body, String imageName, byte[] image) {
        return of(email, subject, body, imageName, image, null, null, null);
    }

    public static SendMailRequest of(String email, String subject, String body, String imageName, byte[] image, String pdfName, byte[] pdf) {
        return of(email, subject, body, imageName, image, pdfName, pdf, null);
    }
}
