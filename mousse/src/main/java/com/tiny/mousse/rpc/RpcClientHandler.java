package com.tiny.mousse.rpc;

import com.alibaba.fastjson.JSON;
import com.tiny.cocoa.protocol.ProtocolType;
import com.tiny.cocoa.protocol.RpcProtocol;
import com.tiny.cocoa.protocol.RpcResponse;
import com.tiny.cocoa.protocol.RpcWaitLock;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcProtocol> {

    private Map<String, RpcWaitLock> results;

    public RpcClientHandler(Map<String, RpcWaitLock> results) {
        this.results = results;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端连接成功 channel id is: {}", ctx.channel().id());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcProtocol rpcProtocol) throws Exception {
        log.info("response: {} from server", rpcProtocol);
        if (ProtocolType.TO_CLIENT.getType().equals(rpcProtocol.getType())) {
            RpcResponse message = JSON.parseObject(rpcProtocol.getData(), RpcResponse.class);
            log.info("message: {} from server", message);
            if (results.containsKey(message.getMessageId())) {
                RpcWaitLock rpcWaitLock = results.get(message.getMessageId());
                rpcWaitLock.getCountDownLatch().countDown();
                rpcWaitLock.setResp(message);
            }
        }
    }
}
