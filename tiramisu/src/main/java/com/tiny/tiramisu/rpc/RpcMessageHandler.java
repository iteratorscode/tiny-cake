package com.tiny.tiramisu.rpc;

import com.tiny.cocoa.protocol.RpcMessage;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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

        try {
            Class<?> clazz = Class.forName(interfaceName);
            Method method = ReflectionUtils.findMethod(clazz, methodName, String.class);
            Object bean = applicationContext.getBean(clazz);
            Map<String, Object> args = rpcMessage.getArgs();

            Object rest = method.invoke(bean, args.get("0"));

            RpcMessage message = new RpcMessage();
            message.setMessageId(rpcMessage.getMessageId());
            Map<String, Object> resp = new HashMap<>();
            resp.put("resp", rest.toString());
            message.setArgs(resp);
            message.setInterfaceName(interfaceName);
            message.setMethodName(methodName);
            return message;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }
}
