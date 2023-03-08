package com.tiny.mousse.rpc;

import com.alibaba.fastjson.JSON;
import com.tiny.cocoa.protocol.ProtocolType;
import com.tiny.cocoa.protocol.RpcMessage;
import com.tiny.cocoa.protocol.RpcProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Component
public class NettyRpcClient implements DisposableBean {

    private Bootstrap bootstrap;

    private Channel channel;

    private EventLoopGroup eventGroup;

    public void start(String ip, Integer port, RpcMessage message) throws Exception {
        eventGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventGroup)
                .channel(NioSocketChannel.class)
                .handler(new RpcClientInitializer());
        ChannelFuture channelFuture = bootstrap.connect(ip, port).sync();
        channel = channelFuture.channel();
        // RpcProtocol rpcProtocol = new RpcProtocol();
        // rpcProtocol.setType(ProtocolType.TO_SERVER.getType());
        // byte[] bytes = JSON.toJSONBytes(message);
        // rpcProtocol.setData(bytes);
        // rpcProtocol.setLength(bytes.length);
        // channel.writeAndFlush(rpcProtocol);
    }

    public Object send(RpcMessage message) {
        RpcProtocol rpcProtocol = new RpcProtocol();
        rpcProtocol.setType(ProtocolType.TO_SERVER.getType());
        byte[] bytes = JSON.toJSONBytes(message);
        rpcProtocol.setData(bytes);
        rpcProtocol.setLength(bytes.length);

        if (Objects.nonNull(channel)) {
            channel.writeAndFlush(rpcProtocol);
        }
        return null;
    }

    @Override
    public void destroy() throws Exception {
        if (Objects.nonNull(channel)) {
            channel.close();
        }

        if (Objects.nonNull(eventGroup)) {
            eventGroup.shutdownGracefully();
        }
    }
}
