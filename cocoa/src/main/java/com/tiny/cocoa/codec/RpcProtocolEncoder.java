package com.tiny.cocoa.codec;

import com.tiny.cocoa.protocol.RpcProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Slf4j
public class RpcProtocolEncoder extends MessageToByteEncoder<RpcProtocol> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcProtocol msg, ByteBuf out) throws Exception {
        log.info("RpcProtocolEncoder#encode msg: {}", msg);
        if(msg == null){
            throw new Exception("msg is null");
        }
        out.writeInt(msg.getType());
        out.writeInt(msg.getLength());
        out.writeBytes(msg.getData());
        log.info("RpcProtocolEncoder#encode success");
    }
}
