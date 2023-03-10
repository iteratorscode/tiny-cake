package com.tiny.cocoa.protocol;

import lombok.Data;

import java.util.Map;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Data
public class RpcMessage {

    private String messageId;

    /**
     * 接口名
     */
    private String interfaceName;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 参数
     */
    private Map<String, Object> args;
}
