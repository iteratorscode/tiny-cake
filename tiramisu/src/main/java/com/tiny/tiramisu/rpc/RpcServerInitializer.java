package com.tiny.tiramisu.rpc;

import com.tiny.cocoa.codec.RpcProtocolDecoder;
import com.tiny.cocoa.codec.RpcProtocolEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import org.springframework.context.ApplicationContext;

/**
 * @author iterators
 * @since 2023/03/05
 */
public class RpcServerInitializer extends ChannelInitializer<SocketChannel> {

    private static final int MAX_FRAME_LENGTH = 1024 * 1024;  //最大长度
    private static final int LENGTH_FIELD_LENGTH = 4;  //长度字段所占的字节数
    private static final int LENGTH_FIELD_OFFSET = 2;  //长度偏移
    private static final int LENGTH_ADJUSTMENT = 0;
    private static final int INITIAL_BYTES_TO_STRIP = 0;

    private final ApplicationContext applicationContext;

    public RpcServerInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();

        pipeline.addLast("rpcProtocolDecoder", new RpcProtocolDecoder(
                MAX_FRAME_LENGTH,
                LENGTH_FIELD_OFFSET,
                LENGTH_FIELD_LENGTH,
                LENGTH_ADJUSTMENT,
                INITIAL_BYTES_TO_STRIP, false));

        pipeline.addLast("rpcProtocolEncoder", new RpcProtocolEncoder());

        //加入业务处理的handler
        pipeline.addLast("rpcServerHandler",new RpcServerHandler(applicationContext));
    }
}
