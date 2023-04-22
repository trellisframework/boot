package net.trellisframework.communication.proto.server.service;

import net.trellisframework.communication.proto.server.provider.ProtoActionContextProvider;
import net.trellisframework.context.provider.ActionContextProvider;
import net.trellisframework.util.mapper.ModelMapper;

public interface Proto extends ModelMapper, ProtoActionContextProvider, ActionContextProvider {

}
