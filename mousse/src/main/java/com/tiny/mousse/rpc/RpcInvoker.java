package com.tiny.mousse.rpc;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.tiny.cocoa.protocol.RpcRequest;
import com.tiny.cocoa.protocol.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Slf4j
public class RpcInvoker implements InvocationHandler {

    private final ApplicationContext applicationContext;

    public RpcInvoker(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            if ("toString".equals(methodName)) {
                return proxy.toString();
            } else if ("hashCode".equals(methodName)) {
                return proxy.hashCode();
            }
        } else if (parameterTypes.length == 1 && "equals".equals(methodName)) {
            return proxy.equals(args[0]);
        }


        String interfaceName = method.getDeclaringClass().getName();
        String serviceName = interfaceName + "." + methodName;

        NamingService namingService = applicationContext.getBean(NamingService.class);
        List<Instance> allInstances = namingService.getAllInstances(serviceName);
        log.info("Service {} instances: {}", serviceName, allInstances);

        Instance instance = allInstances.get(0);
        int port = instance.getPort();
        String ip = instance.getIp();

        // 发送网络请求 ip:port
        NettyRpcClient rpcClient = applicationContext.getBean(NettyRpcClient.class);


        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setInterfaceName(interfaceName);
        rpcRequest.setMethodName(methodName);
        rpcRequest.setParamTypes(parameterTypes);
        rpcRequest.setArgs(args);
        rpcRequest.setMessageId(UUID.randomUUID().toString());
        log.info("send to server message: {}", rpcRequest);
        rpcClient.start(ip, port);
        CompletableFuture<RpcResponse> result = rpcClient.send(rpcRequest);
        RpcResponse message = result.get();

        return "class: " + serviceName +
                " " +
                "method: " + method.getName() +
                " " +
                "args: " + Arrays.toString(args) +
                " " +
                "resp: " + message.toString();
    }
}
