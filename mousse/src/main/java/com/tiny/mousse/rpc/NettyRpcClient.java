package com.tiny.mousse.rpc;

import com.alibaba.fastjson.JSON;
import com.tiny.cocoa.protocol.ProtocolType;
import com.tiny.cocoa.protocol.RpcProtocol;
import com.tiny.cocoa.protocol.RpcRequest;
import com.tiny.cocoa.protocol.RpcResponse;
import com.tiny.cocoa.protocol.RpcWaitLock;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Component
public class NettyRpcClient implements DisposableBean {

    private Bootstrap bootstrap;

    private Channel channel;

    private EventLoopGroup eventGroup;

    private Map<String, RpcWaitLock> results = new ConcurrentHashMap<>();

    public void start(String ip, Integer port) throws Exception {
        eventGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventGroup)
                .channel(NioSocketChannel.class)
                .handler(new RpcClientInitializer(results));
        ChannelFuture channelFuture = bootstrap.connect(ip, port).sync();
        channel = channelFuture.channel();
    }

    public CompletableFuture<RpcResponse> send(RpcRequest message) {
        RpcProtocol rpcProtocol = new RpcProtocol();
        rpcProtocol.setType(ProtocolType.TO_SERVER.getType());
        byte[] bytes = JSON.toJSONBytes(message);
        rpcProtocol.setData(bytes);
        rpcProtocol.setLength(bytes.length);

        CompletableFuture<RpcResponse> future = CompletableFuture.supplyAsync(() -> {
            RpcWaitLock rpcWaitLock = new RpcWaitLock(new CountDownLatch(1));
            results.put(message.getMessageId(), rpcWaitLock);
            if (Objects.nonNull(channel)) {
                channel.writeAndFlush(rpcProtocol);
            }
            try {
                rpcWaitLock.getCountDownLatch().await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return rpcWaitLock.getResp();
        });

        return future;
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
