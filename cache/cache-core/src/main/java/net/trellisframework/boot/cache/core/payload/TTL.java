package net.trellisframework.boot.cache.core.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.trellisframework.core.payload.Payload;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class TTL implements Payload {
    private String[] name;
    private TimeUnit unit;
    private long ttl;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TTL ttl = (TTL) o;
        return Objects.deepEquals(name, ttl.name);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(name);
    }
}
