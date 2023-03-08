package com.tiny.mousse.rpc;

import com.alibaba.fastjson.JSON;
import com.tiny.cocoa.protocol.ProtocolType;
import com.tiny.cocoa.protocol.RpcMessage;
import com.tiny.cocoa.protocol.RpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcProtocol> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端连接成功 channel id is: {}", ctx.channel().id());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcProtocol rpcProtocol) throws Exception {
        log.info("response: {} from server", rpcProtocol);
        Object content = JSON.parse(rpcProtocol.getData());
        log.info("data: {} from server", content);
    }
}
