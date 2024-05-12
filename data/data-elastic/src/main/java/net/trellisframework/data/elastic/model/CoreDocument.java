package net.trellisframework.data.elastic.model;

import net.trellisframework.data.core.model.CoreEntity;

public interface CoreDocument extends CoreEntity {
    String getId();

    void setId(String id);
}
