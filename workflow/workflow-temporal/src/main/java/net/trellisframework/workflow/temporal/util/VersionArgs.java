package net.trellisframework.workflow.temporal.util;

import io.temporal.common.SearchAttributeKey;
import io.temporal.workflow.Workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class VersionArgs {

    public static final String SEARCH_ATTR_TEMPORAL_CHANGE_VERSION = "TemporalChangeVersion";

    private VersionArgs(){
    }

    public static void upsertTypedSearchAttributes(String changeId, int version){
        List<String> versions = new ArrayList<>(Optional.of(Workflow.getTypedSearchAttributes()
                .getUntypedValues()
                .keySet()
                .stream()
                .map(SearchAttributeKey::getName)
                .toList()).orElse(Collections.emptyList()));
        String versionValue = changeId + "-" + version;
        if (!versions.contains(versionValue)) {
            versions.add(versionValue);
            Workflow.upsertTypedSearchAttributes(
                    io.temporal.common.SearchAttributeKey
                            .forKeywordList(SEARCH_ATTR_TEMPORAL_CHANGE_VERSION)
                            .valueSet(versions)
            );
        }
    }
}
