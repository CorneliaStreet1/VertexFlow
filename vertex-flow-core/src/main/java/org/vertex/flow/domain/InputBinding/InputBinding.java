package org.vertex.flow.domain.InputBinding;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InputBinding {
    private int paramIndex;        // 第几个参数
    private Class<?> paramType;    // 参数类型
    private String fromNodeId;     // 来自哪个前置节点
}
