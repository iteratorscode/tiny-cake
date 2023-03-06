package com.tiny.tiramisu.service;

import com.tiny.cocoa.api.IGreetingService;
import com.tiny.tiramisu.annotation.RpcService;
import org.springframework.stereotype.Component;

/**
 * @author iterators
 * @since 2023/03/05
 */
@RpcService
@Component
public class GreetingServiceImpl implements IGreetingService {

    @Override
    public String sayHello(String name) {
        return String.format("Hello %s", name);
    }

}
