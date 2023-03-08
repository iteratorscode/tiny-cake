package com.tiny.mousse.rpc;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.tiny.cocoa.protocol.RpcMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setInterfaceName(interfaceName);
        rpcMessage.setMethodName(methodName);
        Map<String, String> arguments = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            arguments.put(String.valueOf(i), JSON.toJSONString(args[i]));
        }
        rpcMessage.setArgs(arguments);
        log.info("send to server message: {}", rpcMessage);
        rpcClient.start(ip, port, rpcMessage);
        rpcClient.send(rpcMessage);

        return "class: " + serviceName +
                " " +
                "method: " + method.getName() +
                " " +
                "args: " + Arrays.toString(args);
    }
}
