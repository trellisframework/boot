package net.trellisframework.data.redis.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.trellisframework.core.payload.Payload;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
public class RateLimit implements Payload {
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

        public LimitationBuilder maxConcurrent(Integer maxConcurrent) {
            this.maxConcurrent = maxConcurrent;
            return this;
        }

        public LimitationBuilder maxConcurrent(Integer maxConcurrent, Duration timeout) {
            if (maxConcurrent != null) {
                this.maxConcurrent = maxConcurrent;
                this.permitTimeout = timeout;
            }
            return this;
        }

        public LimitationBuilder permitTimeout(Duration timeout) {
            this.permitTimeout = timeout;
            return this;
        }

        public LimitationBuilder add(Duration duration, Integer maxRequests) {
            if (maxRequests != null) {
                rates.add(Rate.of(duration, maxRequests));
            }
            return this;
        }

        public LimitationBuilder millis(Integer maxRequests) {
            return millis(1, maxRequests);
        }

        public LimitationBuilder millis(Integer millis, Integer maxRequests) {
            return add(Duration.ofMillis(millis), maxRequests);
        }

        public LimitationBuilder second(Integer maxRequests) {
            return second(1, maxRequests);
        }

        public LimitationBuilder second(Integer second, Integer maxRequests) {
            return add(Duration.ofSeconds(second), maxRequests);
        }

        public LimitationBuilder minute(Integer maxRequests) {
            return minute(1, maxRequests);
        }

        public LimitationBuilder minute(Integer minute, Integer maxRequests) {
            return add(Duration.ofMinutes(minute), maxRequests);
        }

        public LimitationBuilder hour(Integer maxRequests) {
            return hour(1, maxRequests);
        }

        public LimitationBuilder hour(Integer hour, Integer maxRequests) {
            return add(Duration.ofHours(hour), maxRequests);
        }

        public LimitationBuilder day(Integer maxRequests) {
            return day(1, maxRequests);
        }

        public LimitationBuilder day(Integer day, Integer maxRequests) {
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
    public static class Rate implements Payload {
        Duration duration;
        int maxRequests;
    }
}