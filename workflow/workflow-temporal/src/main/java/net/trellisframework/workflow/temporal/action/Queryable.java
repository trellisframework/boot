package net.trellisframework.workflow.temporal.action;

public interface Queryable {

    Object query(String queryType, Object[] args);

}
