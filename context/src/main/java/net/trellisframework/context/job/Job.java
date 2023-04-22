package net.trellisframework.context.job;

import net.trellisframework.context.provider.ActionContextProvider;
import net.trellisframework.util.mapper.ModelMapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Job implements ModelMapper, ActionContextProvider {

    protected ExecutorService getExecutorService(int nThreads) {
        return Executors.newFixedThreadPool(nThreads);
    }

}
