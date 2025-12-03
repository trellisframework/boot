package net.trellisframework.data.redis.ratelimit;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ResourceState implements Serializable {
    List<Window> rates = new ArrayList<>();
    List<Long> acquiredTimestamps = new ArrayList<>();
    Long coolOffUntil;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Window implements Serializable {
        long duration;
        long startAt;
        int used;
    }
}
