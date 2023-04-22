package net.trellisframework.message.task;

import net.trellisframework.message.payload.FireBaseConfiguration;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import net.trellisframework.context.task.Task1;
import net.trellisframework.core.log.Logger;
import net.trellisframework.core.message.Messages;
import net.trellisframework.http.exception.BadRequestException;
import net.trellisframework.http.exception.ServiceUnavailableException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class InitializeFireBaseTask extends Task1<FirebaseApp, FireBaseConfiguration> {

    @Override
    public FirebaseApp execute(FireBaseConfiguration configuration) {
        if (StringUtils.isBlank(configuration.getCredential()))
            throw new BadRequestException(Messages.CREDENTIAL_IS_REQUIRED);
        try {
            FirebaseApp app = FirebaseApp.getApps().stream().filter(x -> x.getName().equalsIgnoreCase(configuration.getName())).findFirst().orElse(null);
            if (app != null)
                return app;
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(IOUtils.toInputStream(configuration.getCredential(), StandardCharsets.UTF_8))).build();
            return FirebaseApp.initializeApp(options, configuration.getName());
        } catch (Exception e) {
            Logger.error("InitializeFireBase", e.getMessage());
            throw new ServiceUnavailableException(Messages.SERVICE_UNAVAILABLE);
        }
    }
}
