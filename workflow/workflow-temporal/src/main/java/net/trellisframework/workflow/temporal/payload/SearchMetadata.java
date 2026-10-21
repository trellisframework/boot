package net.trellisframework.workflow.temporal.payload;

public record SearchMetadata(String workspaceId, String userId, String username) {

    public boolean hasValues(){
        return workspaceId != null || userId != null || username != null;
    }
}
