package net.trellisframework.workflow.temporal.util;

import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributeUpdate;
import io.temporal.workflow.Workflow;
import net.trellisframework.workflow.temporal.payload.SearchMetadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UserSearchAttributesArgs {

    public static final String WORKSPACE_ID = "workspaceId";
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";

    private static final SearchAttributeKey<String> WORKSPACE_ID_KEY = SearchAttributeKey.forKeyword(WORKSPACE_ID);
    private static final SearchAttributeKey<String> USER_ID_KEY = SearchAttributeKey.forKeyword(USER_ID);
    private static final SearchAttributeKey<String> USERNAME_KEY = SearchAttributeKey.forKeyword(USERNAME);

    private UserSearchAttributesArgs() {
    }

    public static Map<String, IndexedValueType> registeredAttributes() {
        Map<String, IndexedValueType> attributes = new LinkedHashMap<>();
        attributes.put(WORKSPACE_ID, IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
        attributes.put(USER_ID, IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
        attributes.put(USERNAME, IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
        return Map.copyOf(attributes);
    }

    public static void upsertFrom(Object[] arguments) {
        SearchMetadata metadata = findMetadata(arguments);
        if (metadata == null || !metadata.hasValues()) return;
        List<SearchAttributeUpdate<?>> updates = new ArrayList<>();
        TemporalAttributes.add(updates, WORKSPACE_ID_KEY, metadata.workspaceId());
        TemporalAttributes.add(updates, USER_ID_KEY, metadata.userId());
        TemporalAttributes.add(updates, USERNAME_KEY, metadata.username());
        if (!updates.isEmpty()){
            Workflow.upsertTypedSearchAttributes(updates.toArray(SearchAttributeUpdate<?>[]::new));
        }
    }


    private static SearchMetadata findMetadata(Object[] arguments) {
        if (arguments == null){
            return null;
        }
        for (Object argument : arguments) {
            Map<?, ?> input = TemporalAttributes.toMap(argument);
            if (input == null) continue;
            Map<?, ?> workspace = TemporalAttributes.toMap(input.get("workspace"));
            if (workspace == null) continue;
            Map<?, ?> user = TemporalAttributes.toMap(workspace.get("user"));
            Object workspaceId = workspace.containsKey("workspaceId") ? workspace.get("workspaceId") : workspace.get("id");
            return new SearchMetadata(TemporalAttributes.normalize(workspaceId),
                    user == null ? null : TemporalAttributes.normalize(user.get("id")),
                    user == null ? null : TemporalAttributes.normalize(user.get("username")));
        }
        return null;
    }
}
