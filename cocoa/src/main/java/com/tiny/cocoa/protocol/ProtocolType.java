package com.tiny.cocoa.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProtocolType {

    TO_SERVER(1, "To-Server"),
    TO_CLIENT(2, "To-Client"),
    ;

    private Integer type;

    private String desc;

}
