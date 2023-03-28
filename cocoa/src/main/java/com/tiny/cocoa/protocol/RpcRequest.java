package com.tiny.cocoa.protocol;

import lombok.Data;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Data
public class RpcRequest {

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
     * 参数类型
     */
    private Class<?>[] paramTypes;

    /**
     * 参数
     */
    private Object[] args;
}
