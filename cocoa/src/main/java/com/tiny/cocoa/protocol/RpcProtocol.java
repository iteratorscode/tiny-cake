package com.tiny.cocoa.protocol;

import lombok.Data;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Data
public class RpcProtocol {

    /**
     * 请求 or 响应
     */
    private Integer type;

    private Integer length;

    private byte[] data;

}
