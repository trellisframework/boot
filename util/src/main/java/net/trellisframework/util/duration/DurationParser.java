package net.trellisframework.util.duration;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhd])");

    public static Duration parseOrDefault(String value, Duration defaultValue) {
        try {
            return Optional.ofNullable(value).map(DurationParser::parse).orElse(defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Duration parse(String value) {
        if (value == null || value.isBlank()) {
            return Duration.ZERO;
        }
        Matcher matcher = DURATION_PATTERN.matcher(value.toLowerCase().trim());
        Duration total = Duration.ZERO;
        while (matcher.find()) {
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            Duration duration = switch (unit) {
                case "s" -> Duration.ofSeconds(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                default -> Duration.ZERO;
            };
            total = total.plus(duration);
        }
        return total;
    }

}
