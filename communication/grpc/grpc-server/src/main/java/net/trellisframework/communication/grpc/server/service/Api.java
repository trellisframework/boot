package net.trellisframework.communication.grpc.server.service;

import net.trellisframework.communication.grpc.server.provider.GrpcActionContextProvider;
import net.trellisframework.context.provider.ActionContextProvider;
import net.trellisframework.util.mapper.ModelMapper;

public interface Api extends ModelMapper, GrpcActionContextProvider {

}
