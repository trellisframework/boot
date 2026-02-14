package net.trellisframework.util.url;

import com.google.common.net.InternetDomainName;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class URLUtil {

    private final static String pattern = "^(https?:\\/\\/)?([\\w.-]+\\.[a-z]{2,6})(:[0-9]{1,5})?(\\/[^\\s]*)?$";

    public static String getBaseUrl(String url) {
        return url(url).map(x -> {
            StringBuilder builder = new StringBuilder(x.getProtocol()).append("://").append(x.getHost());
            if (x.getPort() != -1 && x.getPort() != 80 && x.getPort() != 443) {
                builder.append(":").append(x.getPort());
            }
            return builder.toString();
        }).orElse(null);
    }

    public static String getProtocol(String url) {
        return url(url).map(URL::getProtocol).orElse(null);
    }

    public static String getHost(String url) {
        return url(url).map(URL::getHost).orElse(null);
    }

    public static Integer getPort(String url) {
        return url(url).map(URL::getPort).orElse(null);
    }

    public static String getPath(String url) {
        return url(url).map(URL::getPath).orElse(null);
    }

    public static String getAuthority(String url) {
        return url(url).map(URL::getAuthority).orElse(null);
    }

    public static String getFile(String url) {
        return url(url).map(URL::getFile).orElse(null);
    }

    public static String getQuery(String url) {
        return url(url).map(URL::getQuery).orElse(null);
    }

    public static Map<String, String> getQueryMap(String url) {
        if (!URLUtil.isValidURL(url) || StringUtils.isBlank(getQuery(url)))
            return Map.of();
        return Arrays.stream(getQuery(url).split("&"))
                .map(x -> x.split("=", 2))
                .collect(Collectors.toMap(
                        x -> URLDecoder.decode(x[0], StandardCharsets.UTF_8),
                        x -> x.length > 1 ? URLDecoder.decode(x[1], StandardCharsets.UTF_8) : "",
                        (existing, replacement) -> replacement
                ));
    }

    public static boolean isValidURL(String url) {
        return Pattern.compile(pattern).matcher(url).matches();
    }

    public static Optional<URL> url(String url) {
        try {
            if (isValidURL(url)) {
                url = !url.startsWith("http://") && !url.startsWith("https://") ? "https://" + url : url;
                return Optional.ofNullable(new URL(url));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static String uriEncode(String input) {
        StringBuilder result = new StringBuilder();
        for (byte b : input.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '~' || c == '.') {
                result.append(c);
            } else {
                result.append(String.format("%%%02X", b));
            }
        }
        return result.toString();
    }

    public static String encodePath(String path) {
        StringJoiner joiner = new StringJoiner("/");
        for (String segment : path.split("/", -1)) joiner.add(uriEncode(segment));
        return joiner.toString();
    }

    public static String buildCanonicalQueryString(TreeMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append(uriEncode(entry.getKey())).append("=").append(uriEncode(entry.getValue()));
        }
        return sb.toString();
    }

    public static String getDomainLtd(String domain) {
        try {
            return InternetDomainName.from(URLUtil.getHost(domain)).topPrivateDomain().toString();
        } catch (Exception e) {
            return null;
        }
    }

}
