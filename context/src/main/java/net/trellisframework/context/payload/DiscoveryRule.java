package net.trellisframework.context.payload;

import net.trellisframework.context.rule.AbstractRule;
import net.trellisframework.core.payload.Payload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor(staticName = "of")
@AllArgsConstructor(staticName = "of")
public class DiscoveryRule implements Payload {
    private boolean isSuccess;

    private List<AbstractRule<?>> rules;

    public static DiscoveryRule of(boolean isSuccess) {
        return of(isSuccess, new ArrayList<>());
    }

    public static DiscoveryRule of(List<AbstractRule<?>> rules) {
        return of(true, rules);
    }
}
