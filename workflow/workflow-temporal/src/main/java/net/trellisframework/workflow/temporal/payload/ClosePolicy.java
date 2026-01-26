package net.trellisframework.workflow.temporal.payload;

import io.temporal.api.enums.v1.ParentClosePolicy;

public enum ClosePolicy {
    TERMINATE(ParentClosePolicy.PARENT_CLOSE_POLICY_TERMINATE),
    ABANDON(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON),
    REQUEST_CANCEL(ParentClosePolicy.PARENT_CLOSE_POLICY_REQUEST_CANCEL);

    private final ParentClosePolicy policy;

    ClosePolicy(ParentClosePolicy policy) {
        this.policy = policy;
    }

    public ParentClosePolicy toTemporalPolicy() {
        return policy;
    }
}
