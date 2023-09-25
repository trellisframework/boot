package net.trellisframework.context.provider;

import net.trellisframework.context.action.*;
import org.checkerframework.checker.units.qual.A;

public interface ActionContextProvider extends InjectorBeanProvider {
    default <A extends Action<O>, O> O call(Class<A> action) {
        return getBean(action).execute();
    }

    default <A extends Action1<O, I1>, O, I1> O call(Class<A> action, I1 i1) {
        return getBean(action).execute(i1);
    }

    default <A extends Action2<O, I1, I2>, O, I1, I2> O call(Class<A> action, I1 i1, I2 i2) {
        return getBean(action).execute(i1, i2);
    }

    default <A extends Action3<O, I1, I2, I3>, O, I1, I2, I3> O call(Class<A> action, I1 i1, I2 i2, I3 i3) {
        return getBean(action).execute(i1, i2, i3);
    }

    default <A extends Action4<O, I1, I2, I3, I4>, O, I1, I2, I3, I4> O call(Class<A> action, I1 i1, I2 i2, I3 i3, I4 i4) {
        return getBean(action).execute(i1, i2, i3, i4);
    }

    default <A extends Action5<O, I1, I2, I3, I4, I5>, O, I1, I2, I3, I4, I5> O call(Class<A> action, I1 i1, I2 i2, I3 i3, I4 i4, I5 i5) {
        return getBean(action).execute(i1, i2, i3, i4, i5);
    }
}
