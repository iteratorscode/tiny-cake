package com.tiny.tiramisu.rpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Component
@Slf4j
public class NettyServer implements ApplicationContextAware, DisposableBean {

    private ApplicationContext applicationContext;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private ServerBootstrap serverBootstrap;

    private Channel channel;

    public Boolean start(String ip, Integer port) {
        bossGroup = new NioEventLoopGroup(1); // (1)
        workerGroup = new NioEventLoopGroup();

        serverBootstrap = new ServerBootstrap(); // (2)
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class) // (3)
                .childHandler(new RpcServerInitializer(this.applicationContext))
                .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

        // Bind and start to accept incoming connections.
        ChannelFuture f = serverBootstrap.bind(ip, port); // (7)
        f.syncUninterruptibly();

        // Wait until the server socket is closed.
        // In this example, this does not happen, but you can do that to gracefully
        // shut down your server.
        channel = f.channel();
        log.info("Rpc netty server start success");
        return Boolean.TRUE;
    }


    @Override
    public void destroy() throws Exception {
        try {
            if (channel != null) {
                // unbind.
                channel.close();
            }
        } catch (Throwable e) {
            log.info(e.getMessage(), e);
        }

        try {
            if (serverBootstrap != null) {
                bossGroup.shutdownGracefully().syncUninterruptibly();
                workerGroup.shutdownGracefully().syncUninterruptibly();
            }
        } catch (Throwable e) {
            log.info(e.getMessage(), e);
        }
        log.info("Rpc netty server stop success");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
