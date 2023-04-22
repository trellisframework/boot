package net.trellisframework.context.rule;

import net.trellisframework.context.provider.ActionContextProvider;
import net.trellisframework.util.reflection.ReflectionUtil;
import de.cronn.reflection.util.PropertyGetter;
import org.apache.commons.lang3.ObjectUtils;

import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
import java.util.Set;

public abstract class DerivationRule<T> extends AbstractRule<T> implements ActionContextProvider {
    private Set<String> fields;

    public DerivationRule(PropertyGetter<T> field) {
        this(field == null ? new HashSet<>() : Set.of(field));
    }

    public DerivationRule(Set<PropertyGetter<T>> fields) {
        this.fields = ObjectUtils.isEmpty(fields) ? new HashSet<>() : new HashSet<>(ReflectionUtil.getPropertiesName(this.getClazz(), fields));
    }

    public abstract Object getDerivedValue(T t);

    public Set<String> getFields() {
        return fields;
    }

    public boolean isEnable() {
        return true;
    }

    private Class<T> getClazz() {
        return (Class<T>) ((ParameterizedType) (getClass().getGenericSuperclass())).getActualTypeArguments()[0];
    }
}
