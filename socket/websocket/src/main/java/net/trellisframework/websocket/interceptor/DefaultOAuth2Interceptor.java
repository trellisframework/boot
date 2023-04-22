package net.trellisframework.websocket.interceptor;

import net.trellisframework.context.provider.ProcessContextProvider;
import net.trellisframework.http.exception.TokenException;
import net.trellisframework.websocket.action.GetPrincipleByBearerAction;
import net.trellisframework.websocket.constant.Messages;
import net.trellisframework.websocket.payload.Principal;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DefaultOAuth2Interceptor implements ChannelInterceptor, ProcessContextProvider {

    private final static String AUTHORIZATION = "Authorization";
    private final static String BEARER = "Bearer";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (Optional.ofNullable(accessor).map(StompHeaderAccessor::getCommand).map(StompCommand.CONNECT::equals).orElse(false)) {
            String header = StringUtils.replaceIgnoreCase(Optional.of(accessor).map(x -> x.getFirstNativeHeader(AUTHORIZATION)).orElse(StringUtils.EMPTY), BEARER, StringUtils.EMPTY).trim();
            try {
                accessor.setUser(Principal.of(call(GetPrincipleByBearerAction.class, header)));
            } catch (Throwable e) {
                throw new TokenException(Messages.TOKEN_NOT_VALID);
            }
        }
        return message;
    }

}
