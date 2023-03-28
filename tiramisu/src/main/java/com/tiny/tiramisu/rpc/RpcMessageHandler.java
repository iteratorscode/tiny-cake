package com.tiny.tiramisu.rpc;

import com.alibaba.fastjson.JSON;
import com.tiny.cocoa.protocol.RpcRequest;
import com.tiny.cocoa.protocol.RpcResponse;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    public Object handle(RpcRequest rpcRequest) {
        String interfaceName = rpcRequest.getInterfaceName();
        String methodName = rpcRequest.getMethodName();
        Class<?>[] paramTypes = rpcRequest.getParamTypes();

        try {
            Class<?> clazz = Class.forName(interfaceName);
            Method method = ReflectionUtils.findMethod(clazz, methodName, paramTypes);
            Object bean = applicationContext.getBean(clazz);
            Object[] args = rpcRequest.getArgs();

            Object rest = method.invoke(bean, args);

            RpcResponse resp = new RpcResponse();
            resp.setMessageId(rpcRequest.getMessageId());
            resp.setData(JSON.toJSONString(rest));
            return resp;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }
}
