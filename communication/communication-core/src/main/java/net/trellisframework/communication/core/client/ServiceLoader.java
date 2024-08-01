package net.trellisframework.communication.core.client;

import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.core.message.Messages;
import net.trellisframework.http.exception.ServiceUnavailableException;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import java.util.Optional;

public class ServiceLoader {

    private static LoadBalancerClient client;

    public static LoadBalancerClient getClient() {
        if (client == null)
            client = ApplicationContextProvider.context.getBean(LoadBalancerClient.class);
        return client;
    }

    public ServiceInstance load(String serviceId) {
        ServiceInstance instance = Optional.ofNullable(getClient()).map(x -> x.choose(serviceId)).orElse(null);
        if (instance == null)
            throw new ServiceUnavailableException(Messages.SERVICE_UNAVAILABLE);
        return instance;
    }

}
