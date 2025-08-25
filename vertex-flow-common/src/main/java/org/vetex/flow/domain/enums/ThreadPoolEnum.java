package org.vetex.flow.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@AllArgsConstructor
@Getter
public enum ThreadPoolEnum {

    CPU_POOL("cpu-pool", null),
    IO_POOL("io-pool", null),  // 缓存线程池风格
    SCHEDULED_POOL("scheduled-pool", null);

    private final String name;
    private final ExecutorService executor;

}
