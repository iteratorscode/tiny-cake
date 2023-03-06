package com.tiny.tiramisu.rpc;

import com.alibaba.fastjson.JSON;
import com.tiny.cocoa.protocol.ProtocolType;
import com.tiny.cocoa.protocol.RpcMessage;
import com.tiny.cocoa.protocol.RpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcProtocol> {

    private final ApplicationContext applicationContext;

    public RpcServerHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol rpcProtocol) throws Exception {
        // 1 接收到数据并处理
        log.info("receive rpc message: {}", rpcProtocol);

        byte[] data = rpcProtocol.getData();
        RpcMessage message = JSON.parseObject(data, RpcMessage.class);
        log.info("receive message: {}", message);

        // 2 处理消息
        RpcMessageHandler rpcMessageHandler = applicationContext.getBean(RpcMessageHandler.class);
        Object result = rpcMessageHandler.handle(message);
        log.info("to client: {}", result);

        // 处理消息
        RpcProtocol toClient = new RpcProtocol();
        byte[] jsonBytes = JSON.toJSONBytes(result);
        toClient.setType(ProtocolType.TO_CLIENT.getType());
        toClient.setLength(jsonBytes.length);
        toClient.setData(jsonBytes);
        log.info("to client message: {}", result);
        ctx.writeAndFlush(jsonBytes);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        log.error("exception", cause);
    }
}
