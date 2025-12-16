package net.trellisframework.data.redis.constant;

import net.trellisframework.core.message.MessageHandler;

public enum Messages implements MessageHandler {
    LOCK_NOT_ACQUIRED,
    POOL_NOT_REGISTERED,
    NO_RESOURCES_IN_POOL,
    NO_AVAILABLE_RESOURCES_IN_POOL,
    NO_AVAILABLE_RESOURCES,

}
