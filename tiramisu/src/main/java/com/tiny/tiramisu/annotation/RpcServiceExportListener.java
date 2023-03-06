package com.tiny.tiramisu.annotation;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.tiny.tiramisu.rpc.NettyServer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Slf4j
public class RpcServiceExportListener implements ApplicationListener, ApplicationContextAware  {

    public static final String BEAN_NAME = "RpcServiceExportListener";

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            try {
                onContextRefreshedEvent((ContextRefreshedEvent) event);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SneakyThrows
    private void onContextRefreshedEvent(ContextRefreshedEvent event) throws UnknownHostException {
        // 1 获取所有服务名称
        String[] rpcServiceBeanNames = applicationContext.getBeanNamesForAnnotation(RpcService.class);

        // 2 解析服务名称：接口名称#方法名称
        Set<String> serviceNames = new HashSet<>();
        for (String serviceBeanName : rpcServiceBeanNames) {
            Object bean = applicationContext.getBean(serviceBeanName);
            Class<?>[] interfaces = bean.getClass().getInterfaces();
            for (Class<?> anInterface : interfaces) {
                String interfaceName = anInterface.getName();
                Method[] declaredMethods = anInterface.getDeclaredMethods();
                for (Method declaredMethod : declaredMethods) {
                    String methodName = declaredMethod.getName();
                    serviceNames.add(String.format("%s.%s", interfaceName, methodName));
                }
            }
        }
        log.info("All rpc service name: {}", serviceNames);

        // 3 解析本机ip
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        log.info("All rpc service provider address: {}", hostAddress);
        ServerProperties serverProperties = applicationContext.getBean(ServerProperties.class);
        int port = serverProperties.getPort() + 10000;

        // 4 注册服务
        NamingService namingService = applicationContext.getBean(NamingService.class);
        serviceNames.forEach(serviceName -> {
            try {

                namingService.registerInstance(serviceName, hostAddress, port);
                log.info("Rpc service register success: {}, addr: {}:{}", serviceName, hostAddress, port);
            } catch (NacosException e) {
                throw new RuntimeException(e);
            }
        });

        // 5 启动netty server监听rpc请求
        NettyServer nettyServer = applicationContext.getBean(NettyServer.class);
        nettyServer.start(hostAddress, port);
    }
}
