package com.tiny.cocoa.codec;

import com.tiny.cocoa.protocol.RpcProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author iterators
 * @since 2023/03/05
 */
public class RpcProtocolEncoder extends MessageToByteEncoder<RpcProtocol> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcProtocol msg, ByteBuf out) throws Exception {
        if(msg == null){
            throw new Exception("msg is null");
        }
        out.writeByte(msg.getType());
        out.writeInt(msg.getLength());
        out.writeBytes(msg.getData());
    }
}
