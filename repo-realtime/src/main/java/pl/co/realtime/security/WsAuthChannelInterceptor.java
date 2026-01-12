package pl.co.realtime.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/**
 * Runs on STOMP frames (CONNECT, SUBSCRIBE, SEND…).
 * It’s invoked after the WS connection is open, on every client frame.
 * Typically, CONNECT is checked first; putting logic here covers tokens in STOMP headers.
 */
@Component
public class WsAuthChannelInterceptor implements ChannelInterceptor {

    private static final List<String> PROTECTED_PREFIXES = List.of(
            "/topic/notifications",
            "/topic/admin",
            "/queue/secure"
    );

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand cmd = accessor.getCommand();
        if (cmd == StompCommand.SUBSCRIBE || cmd == StompCommand.SEND) {
            String dest = accessor.getDestination();
            if (dest != null && PROTECTED_PREFIXES.stream().anyMatch(dest::startsWith)) {
                Principal user = accessor.getUser();
                if (user == null || user.getName() == null || user.getName().startsWith("anon-")) {
                    throw new AccessDeniedException("Authentication required for protected topics");
                }
            }
        }
        return message;
    }
}
