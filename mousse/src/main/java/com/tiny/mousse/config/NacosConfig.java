package com.tiny.mousse.config;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Configuration
public class NacosConfig {

    @Bean
    public NamingService namingService() throws NacosException {
        return NamingFactory.createNamingService("81.70.119.208:18848");
    }

}
