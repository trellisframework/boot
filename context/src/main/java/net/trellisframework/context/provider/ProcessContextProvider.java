package net.trellisframework.context.provider;

import net.trellisframework.context.process.*;
import net.trellisframework.context.process.Process;
import net.trellisframework.context.process.*;

public interface ProcessContextProvider extends InjectorBeanProvider {
    default <TProcess extends Process<TProcessOutput>,
            TProcessOutput>
    TProcessOutput call(Class<TProcess> process) {
        return getBean(process).execute();
    }

    default <TProcess extends Process1<TProcessOutput, TProcessInput>,
            TProcessOutput,
            TProcessInput>
    TProcessOutput call(Class<TProcess> process, TProcessInput t1) {
        return getBean(process).execute(t1);
    }

    default <TProcess extends Process2<TProcessOutput, TProcessInput1, TProcessInput2>,
            TProcessOutput,
            TProcessInput1,
            TProcessInput2>
    TProcessOutput call(Class<TProcess> process, TProcessInput1 t1, TProcessInput2 t2) {
        return getBean(process).execute(t1, t2);
    }

    default <TProcess extends Process3<TProcessOutput, TProcessInput1, TProcessInput2, TProcessInput3>,
            TProcessOutput,
            TProcessInput1,
            TProcessInput2,
            TProcessInput3>
    TProcessOutput call(Class<TProcess> process, TProcessInput1 t1, TProcessInput2 t2, TProcessInput3 t3) {
        return getBean(process).execute(t1, t2, t3);
    }

    default <TProcess extends Process4<TProcessOutput, TProcessInput1, TProcessInput2, TProcessInput3, TProcessInput4>,
            TProcessOutput,
            TProcessInput1,
            TProcessInput2,
            TProcessInput3,
            TProcessInput4>
    TProcessOutput call(Class<TProcess> process, TProcessInput1 t1, TProcessInput2 t2, TProcessInput3 t3, TProcessInput4 t4) {
        return getBean(process).execute(t1, t2, t3, t4);
    }

    default <TProcess extends Process5<TProcessOutput, TProcessInput1, TProcessInput2, TProcessInput3, TProcessInput4, TProcessInput5>,
            TProcessOutput,
            TProcessInput1,
            TProcessInput2,
            TProcessInput3,
            TProcessInput4,
            TProcessInput5>
    TProcessOutput call(Class<TProcess> process, TProcessInput1 t1, TProcessInput2 t2, TProcessInput3 t3, TProcessInput4 t4, TProcessInput5 t5) {
        return getBean(process).execute(t1, t2, t3, t4, t5);
    }

    default <TProcess extends Process6<TProcessOutput, TProcessInput1, TProcessInput2, TProcessInput3, TProcessInput4, TProcessInput5, TProcessInput6>,
            TProcessOutput,
            TProcessInput1,
            TProcessInput2,
            TProcessInput3,
            TProcessInput4,
            TProcessInput5,
            TProcessInput6>
    TProcessOutput call(Class<TProcess> process, TProcessInput1 t1, TProcessInput2 t2, TProcessInput3 t3, TProcessInput4 t4, TProcessInput5 t5, TProcessInput6 t6) {
        return getBean(process).execute(t1, t2, t3, t4, t5, t6);
    }

    default <TProcess extends Process7<TProcessOutput, TProcessInput1, TProcessInput2, TProcessInput3, TProcessInput4, TProcessInput5, TProcessInput6, TProcessInput7>,
            TProcessOutput,
            TProcessInput1,
            TProcessInput2,
            TProcessInput3,
            TProcessInput4,
            TProcessInput5,
            TProcessInput6,
            TProcessInput7>
    TProcessOutput call(Class<TProcess> process, TProcessInput1 t1, TProcessInput2 t2, TProcessInput3 t3, TProcessInput4 t4, TProcessInput5 t5, TProcessInput6 t6, TProcessInput7 t7) {
        return getBean(process).execute(t1, t2, t3, t4, t5, t6, t7);
    }

    default <TProcess extends Process8<TProcessOutput, TProcessInput1, TProcessInput2, TProcessInput3, TProcessInput4, TProcessInput5, TProcessInput6, TProcessInput7, TProcessInput8>,
            TProcessOutput,
            TProcessInput1,
            TProcessInput2,
            TProcessInput3,
            TProcessInput4,
            TProcessInput5,
            TProcessInput6,
            TProcessInput7,
            TProcessInput8>
    TProcessOutput call(Class<TProcess> process, TProcessInput1 t1, TProcessInput2 t2, TProcessInput3 t3, TProcessInput4 t4, TProcessInput5 t5, TProcessInput6 t6, TProcessInput7 t7, TProcessInput8 t8) {
        return getBean(process).execute(t1, t2, t3, t4, t5, t6, t7, t8);
    }

    default <TProcess extends Process9<TProcessOutput, TProcessInput1, TProcessInput2, TProcessInput3, TProcessInput4, TProcessInput5, TProcessInput6, TProcessInput7, TProcessInput8, TProcessInput9>,
            TProcessOutput,
            TProcessInput1,
            TProcessInput2,
            TProcessInput3,
            TProcessInput4,
            TProcessInput5,
            TProcessInput6,
            TProcessInput7,
            TProcessInput8,
            TProcessInput9>
    TProcessOutput call(Class<TProcess> process, TProcessInput1 t1, TProcessInput2 t2, TProcessInput3 t3, TProcessInput4 t4, TProcessInput5 t5, TProcessInput6 t6, TProcessInput7 t7, TProcessInput8 t8, TProcessInput9 t9) {
        return getBean(process).execute(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    default <TProcess extends Process10<TProcessOutput, TProcessInput1, TProcessInput2, TProcessInput3, TProcessInput4, TProcessInput5, TProcessInput6, TProcessInput7, TProcessInput8, TProcessInput9, TProcessInput10>,
            TProcessOutput,
            TProcessInput1,
            TProcessInput2,
            TProcessInput3,
            TProcessInput4,
            TProcessInput5,
            TProcessInput6,
            TProcessInput7,
            TProcessInput8,
            TProcessInput9,
            TProcessInput10>
    TProcessOutput call(Class<TProcess> process, TProcessInput1 t1, TProcessInput2 t2, TProcessInput3 t3, TProcessInput4 t4, TProcessInput5 t5, TProcessInput6 t6, TProcessInput7 t7, TProcessInput8 t8, TProcessInput9 t9, TProcessInput10 t10) {
        return getBean(process).execute(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10);
    }
}
