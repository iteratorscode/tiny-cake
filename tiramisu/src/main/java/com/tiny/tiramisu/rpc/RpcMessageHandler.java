package com.tiny.tiramisu.rpc;

import com.tiny.cocoa.protocol.RpcMessage;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Component
public class RpcMessageHandler implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Object handle(RpcMessage rpcMessage) {
        String interfaceName = rpcMessage.getInterfaceName();
        String methodName = rpcMessage.getMethodName();

        RpcMessage message = new RpcMessage();
        message.setMessageId(rpcMessage.getMessageId());
        message.setArgs(rpcMessage.getArgs());
        message.setInterfaceName(interfaceName);
        message.setMethodName(methodName);
        return rpcMessage;
    }
}
