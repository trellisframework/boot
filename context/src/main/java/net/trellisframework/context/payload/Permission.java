package net.trellisframework.context.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor(staticName = "of")
@AllArgsConstructor(staticName = "of")
public class Permission {
    private String resourceId;
    private String resourceName;
    private Set<String> scopes;
    private Map<String, Set<String>> claims;
}