package com.tiny.mousse.controller;

import com.tiny.cocoa.api.IGreetingService;
import com.tiny.mousse.annotation.RpcReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author iterators
 * @since 2023/03/05
 */
@RestController
@RequestMapping("/greeting")
public class GreetingController {

    @RpcReference
    private IGreetingService greetingService;

    @GetMapping("/hello/{name}")
    public String hello(@PathVariable("name") String name) {
        return greetingService.sayHello(name);
    }

}
