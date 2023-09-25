package net.trellisframework.context.provider;

import net.trellisframework.context.process.*;
import net.trellisframework.context.process.Process;
import net.trellisframework.context.process.*;

public interface ProcessContextProvider extends InjectorBeanProvider {
    default <TProcess extends Process<O>, O> O call(Class<TProcess> process) {
        return getBean(process).execute();
    }

    default <TProcess extends Process1<O, I>, O, I>
    O call(Class<TProcess> process, I i1) {
        return getBean(process).execute(i1);
    }

    default <TProcess extends Process2<O, I1, I2>, O, I1, I2> O call(Class<TProcess> process, I1 i1, I2 i2) {
        return getBean(process).execute(i1, i2);
    }

    default <TProcess extends Process3<O, I1, I2, I3>, O, I1, I2, I3> O call(Class<TProcess> process, I1 i1, I2 i2, I3 i3) {
        return getBean(process).execute(i1, i2, i3);
    }

    default <TProcess extends Process4<O, I1, I2, I3, I4>, O, I1, I2, I3, I4> O call(Class<TProcess> process, I1 i1, I2 i2, I3 i3, I4 i4) {
        return getBean(process).execute(i1, i2, i3, i4);
    }

    default <TProcess extends Process5<O, I1, I2, I3, I4, I5>, O, I1, I2, I3, I4, I5> O call(Class<TProcess> process, I1 i1, I2 i2, I3 i3, I4 i4, I5 i5) {
        return getBean(process).execute(i1, i2, i3, i4, i5);
    }
}
