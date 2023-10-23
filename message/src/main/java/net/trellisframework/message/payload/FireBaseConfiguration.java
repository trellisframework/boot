package net.trellisframework.message.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.trellisframework.core.payload.Payload;
import net.trellisframework.util.environment.EnvironmentUtil;
import org.apache.commons.lang3.StringUtils;


@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ToString
public class FireBaseConfiguration implements Payload {
    private String credential;

    private String name;

    public static FireBaseConfiguration getFromApplicationConfig() {
        String credential = EnvironmentUtil.getPropertyValue("info.fcm.credential", StringUtils.EMPTY);
        String name = EnvironmentUtil.getPropertyValue("info.fcm.name", "[DEFAULT]");
        return new FireBaseConfiguration(credential, name);
    }
}
