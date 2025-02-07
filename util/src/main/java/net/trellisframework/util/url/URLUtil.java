package net.trellisframework.util.url;

import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;

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

}
