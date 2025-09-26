package org.vertex.flow.scheduler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.vertex.flow.annotation.OperatorExecute;
import org.vertex.flow.annotation.OperatorFallBack;
import org.vertex.flow.context.GraphContext;
import org.vertex.flow.context.GraphContextHolder;
import org.vertex.flow.domain.exception.OperatorExecuteMethodNotFoundException;
import org.vertex.flow.operator.IOperator;
import org.vertex.flow.domain.model.Graph;
import org.vertex.flow.domain.model.Node;
import org.vertex.flow.validator.GraphValidator;
import org.vetex.flow.util.ThreadPoolUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DagScheduler {


    private final ExecutorService executor;

    /**
     * 主线程阻塞等待所有结束节点执行完成
     * 阻塞主线程, 直到计数跌零
     */
    private CountDownLatch syncLatch;

    public DagScheduler(ExecutorService executor) {
        if (executor == null) {
            throw new IllegalArgumentException("executor cannot be null");
        }
        this.executor = executor;
    }

    public DagScheduler() {
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void runAndWait(Graph graph, long timeout, TimeUnit unit) {
        try {
            parseNextDepends4DAG(graph);
            if (!GraphValidator.isDAG(graph)) {
                return;
            }
            GraphValidator.validateGraph(graph);
            if (CollectionUtils.isEmpty(graph.getStartNodesSet())) {
                return ;
            }

            // 初始化图全局上下文
            GraphContextHolder.setContext(new GraphContext());
            schedule(graph, timeout, unit);
            //线程阻塞等待DAG执行结束，或超时被唤醒
            await(timeout, unit);
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
            return ;
        }
        finally {
            executor.shutdown();
            GraphContextHolder.clear();
        }
    }

    private void schedule(Graph graph, long timeout, TimeUnit unit) {
        /**
         * 初始化信号量,每个结束节点执行完毕则计数减一
         * 所有的结束节点都执行完毕后, 计数归零, 主线程才解除阻塞
         */
        syncLatch = new CountDownLatch(graph.getEndNodesSet().size());

        // 从开始节点开始调度
        for (Node node : graph.getStartNodesSet()) {
            scheduleSingleNode(node, graph, true);
        }
    }

    /**
     * 主线程阻塞，唤醒后会打断还在执行中的节点
     */
    private void await(long timeOut, TimeUnit timeUnit) {
        boolean isTimeout = false;
        try {
           isTimeout = syncLatch.await(timeOut, timeUnit);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    //线程阻塞等待DAG执行结束，或超时被唤醒

    private void scheduleSingleNode(Node node, Graph graph, boolean useNewThread) {

        try {
            // 节点的入度为0才可以调度,否则不能调度
            if (node.getInDegree().get() != 0) {
                return;
            }


            // 当前节点是结束节点,直接在本线程进行执行就可以
            if (CollectionUtils.isNotEmpty(graph.getEndNodesSet()) && graph.getEndNodesSet().contains(node)) {
                runSingleNode(node, graph);
            }
            else if (!useNewThread) {
                runSingleNode(node, graph);
            }
            else {
                ThreadPoolUtil.submit(
                        () -> runSingleNode(node, graph),
                        executor
                );
            }
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    private void runSingleNode(Node node, Graph graph) {
        try {
            doNodeExecute(node);
        }
        catch (Exception e) {
            doNodeFallBack(node);
        }
        finally {
            //如果是结束节点，则将信号量减一
            boolean isEndOp = false;
            if (CollectionUtils.isNotEmpty(graph.getEndNodesSet()) && graph.getEndNodesSet().contains(node)) {
                isEndOp = true;
                syncLatch.countDown();
                graph.getEndNodesSet().remove(node);
            }

            // 非结束节点, 通知后续节点
            if (!isEndOp) {
                notifyNextNodes(node, graph);
            }
        }
    }

    /**
     * 不是结束节点则:
     * 1. 通知后续节点,本节点执行完毕了,后续节点的入度 -1
     * 2. 如果后续的某个节点的入度跌0了, 那就调度它
     */
    private void notifyNextNodes(Node node, Graph graph) {
        List<Node> runnableNextNodes = Lists.newArrayList();

        for (Node nextNode : node.getNextNodes()) {
            nextNode.getInDegree().decrementAndGet();
            if (nextNode.getInDegree().get() == 0) {
                runnableNextNodes.add(nextNode);
            }
        }

        if (CollectionUtils.isEmpty(runnableNextNodes)) {
            return;
        }
        // 调度后续节点
        for (Node nextNode : runnableNextNodes) {
            // 最后一个节点在本线程调度即可
            boolean useNewThread = runnableNextNodes.indexOf(nextNode) != runnableNextNodes.size() - 1;
            scheduleSingleNode(node, graph, useNewThread);
        }
    }

    private void doNodeFallBack(Node node) {
        try {
            IOperator operator = node.getOperator();
            Class<? extends IOperator> operatorClass = operator.getClass();
            Optional<Method> opMethod = Arrays.stream(operatorClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(OperatorFallBack.class))
                    .findFirst();
            if (opMethod.isEmpty()) {
                throw new OperatorExecuteMethodNotFoundException("OperatorId =  " + node.getNodeId() + "operator fallBack method not found");
            }
            Method method = opMethod.get();
            method.setAccessible(true);

            Object[] methodParam = parseOperatorParam(method, node);
            Object result = method.invoke(operator, methodParam);
            GraphContextHolder.setValue(node.getNodeId(), result);
        }
        catch (Exception e) {
            GraphContextHolder.setValue(node.getNodeId(), null);
            log.error(e.getMessage(), e);
        }
    }

    /**
     *
     * 仅执行算子的方法,不做其他的事
     */
    private void doNodeExecute(Node node) throws InvocationTargetException, IllegalAccessException {
        IOperator operator = node.getOperator();
        Class<? extends IOperator> operatorClass = operator.getClass();
        Optional<Method> opMethod = Arrays.stream(operatorClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(OperatorExecute.class))
                .findFirst();
        if (opMethod.isEmpty()) {
            throw new OperatorExecuteMethodNotFoundException("OperatorId =  " + node.getNodeId() + "operator execute method not found");
        }
        Method method = opMethod.get();
        method.setAccessible(true);

        Object[] methodParam = parseOperatorParam(method, node);
        Object result = method.invoke(operator, methodParam);
        GraphContextHolder.setValue(node.getNodeId(), result);
    }
    private Object[] parseOperatorParam(Method method, Node node) {
        if (method.getParameterCount() == 0) {
            return new Object[0];
        }

        Object[] params = new Object[method.getParameterCount()];
        List<String> operatorInputNodeIds = node.getOrderedOperatorInputNodes().stream().map(Node::getNodeId).toList();
        for (int i = 0; i < params.length; i++) {
            String nodeId = operatorInputNodeIds.get(i);
            params[i] = GraphContextHolder.getValue(nodeId);
        }
        return params;
    }

    private void parseNextDepends4DAG(Graph graph) {
        if (graph.isNextDependParsed()) {
            return;
        }

        graph.setNextDependParsed(true);

        Map<String, Node> nodeWrapperMap = graph.getNodeId2Node();
        if (MapUtils.isEmpty(nodeWrapperMap)) {
            return;
        }
        for (Map.Entry<String, Node> entry : nodeWrapperMap.entrySet()) {
            Node currentNodeWrapper = entry.getValue();
            if (currentNodeWrapper.isInit()) {
                continue;
            }
            currentNodeWrapper.setInit(true);
            //根据 depend 解析依赖关系. 解析当前节点的前置依赖节点
            parseDepend4Node(currentNodeWrapper);

            //根据 next 解析依赖关系. 解析当前节点的后继节点
            parseNext4Node(currentNodeWrapper);
        }

        // 解析整张Dag的开始节点和结束节点. 开始节点是没有前置依赖节点(入度为0)的节点, 结束节点是没有后继节点(出度为0)的节点
        parseStartNodesAndEndNodes(graph);
    }

    private void parseStartNodesAndEndNodes(Graph graph) {
        Map<String, Node> nodeWrapperMap = graph.getNodeId2Node();
        if (MapUtils.isEmpty(nodeWrapperMap)) {
            return;
        }

        for (Map.Entry<String, Node> entry : nodeWrapperMap.entrySet()) {
            Node wrapper = entry.getValue();
            if (CollectionUtils.isEmpty(wrapper.getPreDependNodes())) {
                graph.getStartNodesSet().add(wrapper);
            }
            if (CollectionUtils.isEmpty(wrapper.getNextNodes())) {
                graph.getEndNodesSet().add(wrapper);
            }
        }
    }

    /**
     * 将当前节点加入其所有后继节点的前置依赖节点集合中
     */
    private void parseNext4Node(Node currentNode) {
        //根据当前节点的后继节点, 解析后继依赖关系
        Set<Node> nextNodes = currentNode.getNextNodes();
        if (CollectionUtils.isEmpty(nextNodes)) {
            return;
        }

        // 将当前节点加入其每个后继节点的前置依赖中
        for (Node nextNode : nextNodes) {

            // 当前节点 已经在其 后继结点 的 前置依赖集合 中, 不重复加入
            if (CollectionUtils.isNotEmpty(nextNode.getPreDependNodes()) && nextNode.getPreDependNodes().contains(currentNode)) {
                continue;
            }

            if (nextNode.getPreDependNodes() == null) {
                nextNode.setPreDependNodes(Sets.newHashSet());
            }

            // 否则把当前节点加入此后继结点的依赖节点中
            nextNode.getPreDependNodes().add(currentNode);

            // 当前节点的outDegree + 1
            currentNode.getOutDegree().incrementAndGet();
        }

    }

    /**
     * 将当前节点加入其所有前置节点的后继节点集合中
     */
    private void parseDepend4Node(Node currentNode) {

        //根据 depend 解析依赖关系
        Set<Node> preDependNodes = currentNode.getPreDependNodes();
        if (CollectionUtils.isEmpty(preDependNodes)) {
            return;
        }

        // 将当前节点添加到每个前置依赖节点的后继节点集合中
        for (Node dependNode : preDependNodes) {

            // 当前节点已经在其前置依赖节点的后继节点集合中, 不重复加入
            if (CollectionUtils.isNotEmpty(dependNode.getNextNodes()) && dependNode.getNextNodes().contains(currentNode)) {
                continue;
            }
            //将当前节点添加到前驱节点的后继集合中
            if (dependNode.getNextNodes() == null) {
                dependNode.setNextNodes(Sets.newHashSet());
            }
            dependNode.getNextNodes().add(currentNode);

            // 当前节点的indegree+1
            currentNode.getInDegree().incrementAndGet();
        }
    }


}
