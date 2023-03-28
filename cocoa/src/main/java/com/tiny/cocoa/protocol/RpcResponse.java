package com.tiny.cocoa.protocol;

import lombok.Data;

/**
 * @author iterators
 * @since 2023/03/05
 */
@Data
public class RpcResponse {

    private String messageId;

    private String data;
}
