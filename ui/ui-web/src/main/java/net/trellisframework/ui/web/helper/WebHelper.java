package net.trellisframework.ui.web.helper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.trellisframework.ui.web.payload.ClientInfo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Optional;

public interface WebHelper {
    default HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession();
    }

    default String getIp() {
        HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
        String ip = request.getHeader("X-FORWARDED-FOR");
        ip =  StringUtils.isBlank(ip) ? request.getRemoteAddr() : ip;
        String[] ips = StringUtils.split(ip, ",");
        return ObjectUtils.isNotEmpty(ips) ? ips[0].trim() : ip;
    }

    default ClientInfo getClientInfo() {
        return new ClientInfo(this.getIp(), Optional.ofNullable(this.getSession()).map(HttpSession::getId).orElse(null));
    }
}

