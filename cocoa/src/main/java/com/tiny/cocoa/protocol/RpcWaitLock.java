package com.tiny.cocoa.protocol;

import lombok.Data;

import java.util.concurrent.CountDownLatch;

/**
 * @author iterators
 * @since 2023/03/10
 */
@Data
public class RpcWaitLock {

    private CountDownLatch countDownLatch;

    public RpcWaitLock(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    private RpcMessage resp;
}
