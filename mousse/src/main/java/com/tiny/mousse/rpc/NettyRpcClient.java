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
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Component
public class NettyRpcClient {

    private Bootstrap bootstrap;

    private Channel channel;

    public void start(String ip, Integer port) throws Exception {
        EventLoopGroup eventGroup = new NioEventLoopGroup();
        try {
            bootstrap = new Bootstrap();
            bootstrap.group(eventGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new RpcClientInitializer());
            ChannelFuture channelFuture = bootstrap.connect(ip, port).sync();
            channel = channelFuture.channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            eventGroup.shutdownGracefully();
        }
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
}
