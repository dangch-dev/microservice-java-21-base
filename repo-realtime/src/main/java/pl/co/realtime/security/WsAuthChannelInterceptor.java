package pl.co.realtime.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Enforces auth only for protected destinations (notification), keeps chat public.
 */
@Component
public class WsAuthChannelInterceptor implements ChannelInterceptor {

    private static final String PROTECTED_PREFIX = "/topic/notifications";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand cmd = accessor.getCommand();
        if (cmd == StompCommand.SUBSCRIBE || cmd == StompCommand.SEND) {
            String dest = accessor.getDestination();
            if (dest != null && dest.startsWith(PROTECTED_PREFIX)) {
                Principal user = accessor.getUser();
                if (user == null || user.getName() == null || user.getName().startsWith("anon-")) {
                    throw new AccessDeniedException("Authentication required for " + PROTECTED_PREFIX);
                }
            }
        }
        return message;
    }
}
