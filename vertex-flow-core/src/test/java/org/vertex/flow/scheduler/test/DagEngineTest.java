package org.vertex.flow.scheduler.test;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.vertex.flow.graph.builder.GraphNodeWrapper;
import org.vertex.flow.graph.builder.GraphWrapper;
import org.vertex.flow.operator.*;
import org.vertex.flow.scheduler.DagScheduler;

import java.util.concurrent.TimeUnit;


class DagEngineTest {

    private AnnotationConfigApplicationContext context;

    private DagScheduler engine;

    @BeforeEach
    void setUp() {
        engine = new DagScheduler();
    }

    @BeforeEach
    void initSpringContext() {
        context = new AnnotationConfigApplicationContext();
        // 注册算子 Bean 和 SpringContextHolder
        context.scan("org.vertex.flow.operator"); // 扫描算子包
        context.register(org.vertex.flow.util.spring.SpringContextHolder.class);
        context.refresh(); // 初始化 Spring 容器
    }
    @AfterEach
    void closeContext() {
        context.close();
    }

    // 1. 线性依赖 A -> B -> C
    @Test
    void testLinearExecution() {
        GraphWrapper graph = new GraphWrapper();

        GraphNodeWrapper nodeA = graph.addNode(StringConcatOperator.class, "A");
        GraphNodeWrapper nodeB = graph.addNode(TokenizeOperator.class, "B").depend(nodeA);
        graph.addNode(PrintOperator.class, "C").depend(nodeB);

        engine.runAndWait(graph.getGraph(), 100, TimeUnit.MILLISECONDS);
        // TODO: 断言执行顺序和输出
    }

    // 2. 并行依赖 A -> (B, C) -> D
    @Test
    void testParallelExecution() {
        GraphWrapper graph = new GraphWrapper();

        GraphNodeWrapper nodeA = graph.addNode(StringConcatOperator.class);
        GraphNodeWrapper nodeB = graph.addNode(TokenizeOperator.class).depend(nodeA);
        GraphNodeWrapper nodeC = graph.addNode(SumOperator.class).depend(nodeA);

        GraphNodeWrapper nodeD = graph.addNode(PrintOperator.class)
                .depend(nodeB)
                .depend(nodeC);

        engine.runAndWait(graph.getGraph(), 100, TimeUnit.MILLISECONDS);
        // TODO: 断言 B、C 可并行执行，D 在后
    }

    // 3. 节点执行失败 A -> B(fail) -> C
    @Test
    void testNodeFailureStopsDownstream() {
        GraphWrapper graph = new GraphWrapper();

        GraphNodeWrapper nodeA = graph.addNode(StringConcatOperator.class);
        GraphNodeWrapper nodeB = graph.addNode(FailOperator.class).depend(nodeA);
        graph.addNode(PrintOperator.class).depend(nodeB);

        engine.runAndWait(graph.getGraph(), 100, TimeUnit.MILLISECONDS);
        // TODO: 断言 A 执行，B 抛异常，C 未执行
    }

    // 4. 上下文传递 A -> B
    @Test
    void testContextPropagation() {
        GraphWrapper graph = new GraphWrapper();

        GraphNodeWrapper nodeA = graph.addNode(ContextWriteOperator.class);
        graph.addNode(ContextReadOperator.class).depend(nodeA);

        engine.runAndWait(graph.getGraph(), 100, TimeUnit.MILLISECONDS);
        // TODO: 验证上下文值被传递
    }

    // 5. 节点无输入依赖
    @Test
    void testNodeWithoutDependencies() {
        GraphWrapper graph = new GraphWrapper();
        graph.addNode(PrintOperator.class);

        engine.runAndWait(graph.getGraph(), 100, TimeUnit.MILLISECONDS);
        // TODO: 验证节点能独立执行
    }

    // 6. 环检测 A -> B -> C -> A
    @Test
    void testCycleDetection() {
        GraphWrapper graph = new GraphWrapper();

        GraphNodeWrapper nodeA = graph.addNode(PrintOperator.class);
        GraphNodeWrapper nodeB = graph.addNode(PrintOperator.class).depend(nodeA);
        GraphNodeWrapper nodeC = graph.addNode(PrintOperator.class).depend(nodeB);

        nodeA.depend(nodeC); // 构成环

        try {
            engine.runAndWait(graph.getGraph(), 100, TimeUnit.MILLISECONDS);
            assert false : "Should have thrown due to cycle";
        } catch (IllegalStateException e) {
            // TODO: 验证抛出环异常
        }
    }

    // 7. 节点 ID 唯一性
    @Test
    void testUniqueNodeId() {
        GraphWrapper graph = new GraphWrapper();
        graph.addNode(PrintOperator.class);

        try {
            // graph.addNode(SumOperator.class).setNodeId("A"); // 重复ID
            assert false : "Should have thrown due to duplicate ID";
        } catch (IllegalArgumentException e) {
            // TODO: 验证重复ID异常
        }
    }

    // 8. 多 DAG 隔离
    @Test
    void testMultipleDagsIsolation() {
        GraphWrapper graph1 = new GraphWrapper();
        GraphWrapper graph2 = new GraphWrapper();

        graph1.addNode(PrintOperator.class);
        graph2.addNode(SumOperator.class);

        engine.runAndWait(graph1.getGraph(), 100, TimeUnit.MILLISECONDS);
        engine.runAndWait(graph2.getGraph(), 100, TimeUnit.MILLISECONDS);
        // TODO: 验证两个 DAG 上下文互不干扰
    }

    // 9. 节点超时处理
    @Test
    void testNodeTimeoutHandling() {
        GraphWrapper graph = new GraphWrapper();

        GraphNodeWrapper nodeA = graph.addNode(StringConcatOperator.class);
        graph.addNode(TimeoutOperator.class).depend(nodeA);

        engine.runAndWait(graph.getGraph(), 100, TimeUnit.MILLISECONDS);
        // TODO: 验证 B 超时，下游未执行
    }

    // 10. DAG 完成回调
    @Test
    void testDagCompletionCallback() {
        GraphWrapper graph = new GraphWrapper();

        GraphNodeWrapper nodeA = graph.addNode(StringConcatOperator.class);
        GraphNodeWrapper nodeB = graph.addNode(PrintOperator.class).depend(nodeA);

        //engine.registerCompletionCallback(() -> System.out.println("DAG 完成回调触发"));
        engine.runAndWait(graph.getGraph(), 100, TimeUnit.MILLISECONDS);
        // TODO: 验证回调被触发
    }

    // 11. 空图执行
    @Test
    void testEmptyGraphExecution() {
        GraphWrapper graph = new GraphWrapper();
        engine.runAndWait(graph.getGraph(), 100, TimeUnit.MILLISECONDS);
        // TODO: 验证无节点执行不报错
    }

    // 12. 图中孤立节点
    @Test
    void testIsolatedNodeExecution() {
        GraphWrapper graph = new GraphWrapper();
        graph.addNode(PrintOperator.class);
        engine.runAndWait(graph.getGraph(), 100, TimeUnit.MILLISECONDS);
        // TODO: 验证孤立节点执行成功
    }
}


