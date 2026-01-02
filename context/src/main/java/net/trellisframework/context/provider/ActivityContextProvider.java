package net.trellisframework.context.provider;

import net.trellisframework.context.activity.*;

public interface ActivityContextProvider extends InjectorBeanProvider {

    default <A extends Activity<O>, O> O activity(Class<A> activity) {
        return getBean(activity).execute();
    }

    default <A extends Activity1<O, I1>, O, I1> O activity(Class<A> activity, I1 i1) {
        return getBean(activity).execute(i1);
    }

    default <A extends Activity2<O, I1, I2>, O, I1, I2> O activity(Class<A> activity, I1 i1, I2 i2) {
        return getBean(activity).execute(i1, i2);
    }

    default <A extends Activity3<O, I1, I2, I3>, O, I1, I2, I3> O activity(Class<A> activity, I1 i1, I2 i2, I3 i3) {
        return getBean(activity).execute(i1, i2, i3);
    }

    default <A extends Activity4<O, I1, I2, I3, I4>, O, I1, I2, I3, I4> O activity(Class<A> activity, I1 i1, I2 i2,
            I3 i3, I4 i4) {
        return getBean(activity).execute(i1, i2, i3, i4);
    }

    default <A extends Activity5<O, I1, I2, I3, I4, I5>, O, I1, I2, I3, I4, I5> O activity(Class<A> activity, I1 i1,
            I2 i2, I3 i3, I4 i4, I5 i5) {
        return getBean(activity).execute(i1, i2, i3, i4, i5);
    }
}
