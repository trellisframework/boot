package net.trellisframework.data.redis.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
public class RateLimit {
    List<Rate> rates = new ArrayList<>();
    int maxConcurrent;
    Duration permitTimeout = Duration.ofMinutes(1);
    Duration defaultCoolOff = Duration.ofMinutes(1);

    public static LimitationBuilder builder() {
        return new LimitationBuilder();
    }

    public static class LimitationBuilder {
        private final List<Rate> rates = new ArrayList<>();
        private int maxConcurrent;
        private Duration permitTimeout = Duration.ofMinutes(1);
        private Duration defaultCoolOff = Duration.ofMinutes(1);

        public LimitationBuilder maxConcurrent(int maxConcurrent) {
            this.maxConcurrent = maxConcurrent;
            return this;
        }

        public LimitationBuilder maxConcurrent(int maxConcurrent, Duration timeout) {
            this.maxConcurrent = maxConcurrent;
            this.permitTimeout = timeout;
            return this;
        }

        public LimitationBuilder permitTimeout(Duration timeout) {
            this.permitTimeout = timeout;
            return this;
        }

        public LimitationBuilder add(Duration duration, int maxRequests) {
            rates.add(Rate.of(duration, maxRequests));
            return this;
        }

        public LimitationBuilder millis(int maxRequests) {
            return millis(1, maxRequests);
        }

        public LimitationBuilder millis(int millis, int maxRequests) {
            return add(Duration.ofMillis(millis), maxRequests);
        }

        public LimitationBuilder second(int maxRequests) {
            return second(1, maxRequests);
        }

        public LimitationBuilder second(int second, int maxRequests) {
            return add(Duration.ofSeconds(second), maxRequests);
        }

        public LimitationBuilder minute(int maxRequests) {
            return minute(1, maxRequests);
        }

        public LimitationBuilder minute(int minute, int maxRequests) {
            return add(Duration.ofMinutes(minute), maxRequests);
        }

        public LimitationBuilder hour(int maxRequests) {
            return hour(1, maxRequests);
        }

        public LimitationBuilder hour(int hour, int maxRequests) {
            return add(Duration.ofHours(hour), maxRequests);
        }

        public LimitationBuilder day(int maxRequests) {
            return day(1, maxRequests);
        }

        public LimitationBuilder day(int day, int maxRequests) {
            return add(Duration.ofDays(day), maxRequests);
        }

        public LimitationBuilder defaultCoolOff(Duration defaultCoolOff) {
            this.defaultCoolOff = defaultCoolOff;
            return this;
        }

        public RateLimit build() {
            var limitation = new RateLimit();
            limitation.rates = new ArrayList<>(rates);
            limitation.maxConcurrent = maxConcurrent;
            limitation.permitTimeout = permitTimeout;
            limitation.defaultCoolOff = defaultCoolOff;
            return limitation;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class Rate {
        Duration duration;
        int maxRequests;
    }
}