package net.trellisframework.context.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

@Data
@NoArgsConstructor(staticName = "of")
@AllArgsConstructor(staticName = "of")
public class Principle implements Serializable {
    private String id;

    private String email;

    private String username;

    private String firstName;

    private String lastName;

    private Map<String, Object> attributes;

    private Map<String, ?> resourceAccess;

    private Collection<Permission> permissions;
}
