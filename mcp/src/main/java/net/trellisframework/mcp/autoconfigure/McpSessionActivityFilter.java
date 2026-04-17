package net.trellisframework.mcp.autoconfigure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class McpSessionActivityFilter extends OncePerRequestFilter {
    static final String SESSION_HEADER = "Mcp-Session-Id";
    static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    private final McpSessionCleanupScheduler scheduler;
    private final boolean interceptUnauthorized;

    public McpSessionActivityFilter(McpSessionCleanupScheduler scheduler, boolean interceptUnauthorized) {
        this.scheduler = scheduler;
        this.interceptUnauthorized = interceptUnauthorized;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String sessionId = request.getHeader(SESSION_HEADER);
        boolean hasSession = sessionId != null && !sessionId.isBlank();
        if (hasSession)
            scheduler.touch(sessionId);

        if (!interceptUnauthorized) {
            chain.doFilter(request, response);
            return;
        }
        chain.doFilter(request, new SafeResponse(response, hasSession));
    }

    static int translateStatus(int status, boolean hasSession) {
        if (!hasSession)
            return status;
        if (status == HttpServletResponse.SC_UNAUTHORIZED || status == HttpServletResponse.SC_BAD_REQUEST)
            return HttpServletResponse.SC_NOT_FOUND;
        return status;
    }

    static class SafeResponse extends HttpServletResponseWrapper {
        private final boolean hasSession;

        SafeResponse(HttpServletResponse response, boolean hasSession) {
            super(response);
            this.hasSession = hasSession;
        }

        @Override
        public void setStatus(int sc) {
            super.setStatus(translateStatus(sc, hasSession));
        }

        @Override
        public void sendError(int sc) throws IOException {
            int translated = translateStatus(sc, hasSession);
            if (translated == sc)
                super.sendError(sc);
            else
                super.setStatus(translated);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            int translated = translateStatus(sc, hasSession);
            if (translated == sc)
                super.sendError(sc, msg);
            else
                super.setStatus(translated);
        }

        @Override
        public void setHeader(String name, String value) {
            if (WWW_AUTHENTICATE.equalsIgnoreCase(name))
                return;
            super.setHeader(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            if (WWW_AUTHENTICATE.equalsIgnoreCase(name))
                return;
            super.addHeader(name, value);
        }
    }
}
