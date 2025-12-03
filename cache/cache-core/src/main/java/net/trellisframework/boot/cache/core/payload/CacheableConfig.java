package net.trellisframework.boot.cache.core.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.trellisframework.boot.cache.core.constant.CacheSerializer;

import java.time.Duration;

@Data
@AllArgsConstructor(staticName = "of")
public class CacheableConfig {
    private String[] name;
    private Duration ttl;
    private CacheSerializer serializer;
}
