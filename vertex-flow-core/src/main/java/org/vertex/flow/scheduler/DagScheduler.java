package org.vertex.flow.scheduler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.vertex.flow.annotation.OpInput;
import org.vertex.flow.annotation.OperatorExecute;
import org.vertex.flow.annotation.OperatorFallBack;
import org.vertex.flow.domain.exception.OperatorExecuteMethodNotFoundException;
import org.vertex.flow.domain.exception.OperatorParameterInvalidException;
import org.vertex.flow.operator.IOperator;
import org.vertex.flow.graph.builder.DirectedAcyclicGraphWrapper;
import org.vertex.flow.graph.builder.GraphNodeWrapper;
import org.vetex.flow.util.ThreadPoolUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DagScheduler {


    private final ExecutorService executor;

    /**
     * Key:nodeId
     * Value: node 执行结果
     */
    private final Map<String, Object> nodeId2NodeResult = Maps.newConcurrentMap();

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

    public void runAndWait(DirectedAcyclicGraphWrapper graphWrapper, long timeout, TimeUnit unit) {
        try {
            if (!graphWrapper.isDAG()) {
                return;
            }
            // todo 构图的时候顺便记录每个节点的每个入参对应的节点ID,传参的时候从全局Map取
            parseNextDepends4DAG(graphWrapper);

            if (CollectionUtils.isEmpty(graphWrapper.getStartNodesWrapperSet())) {
                return ;
            }

            schedule(graphWrapper, timeout, unit);
        }
        catch (Exception e) {
            return ;
        }
        finally {
            executor.shutdown();
        }
    }

    private void schedule(DirectedAcyclicGraphWrapper graphWrapper, long timeout, TimeUnit unit) {
        /**
         * 初始化信号量,每个结束节点执行完毕则计数减一
         * 所有的结束节点都执行完毕后, 计数归零, 主线程才解除阻塞
         */
        syncLatch = new CountDownLatch(graphWrapper.getEndNodesWrapperSet().size());

        // 从开始节点开始调度
        for (GraphNodeWrapper graphNodeWrapper : graphWrapper.getStartNodesWrapperSet()) {
            scheduleSingleNode(graphNodeWrapper, graphWrapper, true);
        }

        //线程阻塞等待DAG执行结束，或超时被唤醒
        await(timeout, unit);
    }

    /**
     * 主线程阻塞，唤醒后会打断还在执行中的节点
     */
    private void await(long timeOut, TimeUnit timeUnit) {
        boolean isTimeout = false;
        try {
           isTimeout = syncLatch.await(timeOut, timeUnit);
        } catch (InterruptedException e) {

        }
    }

    //线程阻塞等待DAG执行结束，或超时被唤醒

    private void scheduleSingleNode(GraphNodeWrapper graphNodeWrapper, DirectedAcyclicGraphWrapper graphWrapper, boolean useNewThread) {

        try {
            // 节点的入度为0才可以调度,否则不能调度
            if (graphNodeWrapper.getInDegree().get() != 0) {
                return;
            }


            // 当前节点是结束节点,直接在本线程进行执行就可以
            if (CollectionUtils.isNotEmpty(graphWrapper.getEndNodesWrapperSet()) && graphWrapper.getEndNodesWrapperSet().contains(graphNodeWrapper)) {
                runSingleNode(graphNodeWrapper, graphWrapper);
            }
            else if (!useNewThread) {
                runSingleNode(graphNodeWrapper, graphWrapper);
            }
            else {
                ThreadPoolUtil.submit(
                        () -> runSingleNode(graphNodeWrapper, graphWrapper),
                        executor
                );
            }
        }
        catch (Exception e) {
        }

    }

    private void runSingleNode(GraphNodeWrapper graphNodeWrapper, DirectedAcyclicGraphWrapper graphWrapper) {
        try {
            doNodeExecute(graphNodeWrapper);
        }
        catch (Exception e) {
            doNodeFallBack(graphNodeWrapper);
        }
        finally {
            //如果是结束节点，则将信号量减一
            boolean isEndOp = false;
            if (CollectionUtils.isNotEmpty(graphWrapper.getEndNodesWrapperSet()) && graphWrapper.getEndNodesWrapperSet().contains(graphNodeWrapper)) {
                isEndOp = true;
                syncLatch.countDown();
                graphWrapper.getEndNodesWrapperSet().remove(graphNodeWrapper);
            }

            // 非结束节点, 通知后续节点
            if (!isEndOp) {
                notifyNextNodes(graphNodeWrapper, graphWrapper);
            }
        }
    }

    /**
     * 不是结束节点则:
     * 1. 通知后续节点,本节点执行完毕了,后续节点的入度 -1
     * 2. 如果后续的某个节点的入度跌0了, 那就调度它
     */
    private void notifyNextNodes(GraphNodeWrapper graphNodeWrapper,  DirectedAcyclicGraphWrapper graphWrapper) {
        List<GraphNodeWrapper> runnableNextNodes = Lists.newArrayList();

        for (GraphNodeWrapper nextNode : graphNodeWrapper.getNextNodes()) {
            nextNode.getInDegree().decrementAndGet();
            if (nextNode.getInDegree().get() == 0) {
                runnableNextNodes.add(nextNode);
            }
        }

        if (CollectionUtils.isEmpty(runnableNextNodes)) {
            return;
        }
        // 调度后续节点
        for (GraphNodeWrapper nextNode : runnableNextNodes) {
            // 最后一个节点在本线程调度即可
            boolean useNewThread = runnableNextNodes.indexOf(nextNode) != runnableNextNodes.size() - 1;
            scheduleSingleNode(graphNodeWrapper, graphWrapper, useNewThread);
        }
    }

    private void doNodeFallBack(GraphNodeWrapper graphNodeWrapper) {
        try {
            IOperator operator = graphNodeWrapper.getOperator();
            Class<? extends IOperator> operatorClass = operator.getClass();
            Optional<Method> opMethod = Arrays.stream(operatorClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(OperatorFallBack.class))
                    .findFirst();
            if (opMethod.isEmpty()) {
                throw new OperatorExecuteMethodNotFoundException("OperatorId =  " + graphNodeWrapper.getNodeId() + "operator fallBack method not found");
            }
            Method method = opMethod.get();
            method.setAccessible(true);

            Object[] methodParam = parseOperatorParam(method,  graphNodeWrapper);
            Object result = method.invoke(operator, methodParam);
            nodeId2NodeResult.put(graphNodeWrapper.getNodeId(), result);
        }
        catch (Exception e) {
            nodeId2NodeResult.put(graphNodeWrapper.getNodeId(), null);
        }
    }

    /**
     *
     * 仅执行算子的方法,不做其他的事
     */
    private void doNodeExecute(GraphNodeWrapper graphNodeWrapper) throws InvocationTargetException, IllegalAccessException {
        IOperator operator = graphNodeWrapper.getOperator();
        Class<? extends IOperator> operatorClass = operator.getClass();
        Optional<Method> opMethod = Arrays.stream(operatorClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(OperatorExecute.class))
                .findFirst();
        if (opMethod.isEmpty()) {
            throw new OperatorExecuteMethodNotFoundException("OperatorId =  " + graphNodeWrapper.getNodeId() + "operator execute method not found");
        }
        Method method = opMethod.get();
        method.setAccessible(true);

        Object[] methodParam = parseOperatorParam(method,  graphNodeWrapper);
        Object result = method.invoke(operator, methodParam);
        nodeId2NodeResult.put(graphNodeWrapper.getNodeId(), result);
    }
    private Object[] parseOperatorParam(Method method, GraphNodeWrapper graphNodeWrapper) {
        if (method.getParameterCount() == 0) {
            return new Object[0];
        }

        // 应该每个入参都被 @OpInput 注解标记,构图阶段就应该校验过了
        boolean invalidCase = Arrays.stream(method.getParameters())
                .anyMatch(param -> !param.isAnnotationPresent(OpInput.class));
        if (invalidCase) {
            throw new OperatorParameterInvalidException("OperatorId =  " + graphNodeWrapper.getNodeId() + "input param has no OpInput Annotation");
        }
        Object[] params = new Object[method.getParameterCount()];
        List<String> operatorInputNodeIds = graphNodeWrapper.getOperatorInputNodeIds();
        for (int i = 0; i < params.length; i++) {
            String nodeId = operatorInputNodeIds.get(i);
            params[i] = nodeId2NodeResult.get(nodeId);
        }
        return params;
    }

    private void parseNextDepends4DAG(DirectedAcyclicGraphWrapper dagWrapper) {
        if (dagWrapper.isNextDependParsed()) {
            return;
        }

        dagWrapper.setNextDependParsed(true);

        Map<String, GraphNodeWrapper> nodeWrapperMap = dagWrapper.getNodeId2NodeWrapper();
        if (MapUtils.isEmpty(nodeWrapperMap)) {
            return;
        }
        for (Map.Entry<String, GraphNodeWrapper> entry : nodeWrapperMap.entrySet()) {
            GraphNodeWrapper currentNodeWrapper = entry.getValue();
            if (!currentNodeWrapper.isInit()) {
                currentNodeWrapper.setInit(true);
            }

            //根据 depend 解析依赖关系. 解析当前节点的前置依赖节点
            parseDepend4Node(currentNodeWrapper);

            //根据 next 解析依赖关系. 解析当前节点的后继节点
            parseNext4Node(currentNodeWrapper);
        }

        // 解析整张Dag的开始节点和结束节点. 开始节点是没有前置依赖节点(入度为0)的节点, 结束节点是没有后继节点(出度为0)的节点
        parseStartNodesAndEndNodes(dagWrapper);
    }

    private void parseStartNodesAndEndNodes(DirectedAcyclicGraphWrapper dagWrapper) {
        Map<String, GraphNodeWrapper> nodeWrapperMap = dagWrapper.getNodeId2NodeWrapper();
        if (MapUtils.isEmpty(nodeWrapperMap)) {
            return;
        }

        for (Map.Entry<String, GraphNodeWrapper> entry : nodeWrapperMap.entrySet()) {
            GraphNodeWrapper wrapper = entry.getValue();
            if (CollectionUtils.isEmpty(wrapper.getPreDependNodes())) {
                dagWrapper.getStartNodesWrapperSet().add(wrapper);
            }
            if (CollectionUtils.isEmpty(wrapper.getNextNodes())) {
                dagWrapper.getEndNodesWrapperSet().add(wrapper);
            }
        }
    }

    /**
     * 将当前节点加入其所有后继节点的前置依赖节点集合中
     */
    private void parseNext4Node(GraphNodeWrapper currentNodeWrapper) {
        if (currentNodeWrapper.isInit()) {
            return;
        }
        //根据当前节点的后继节点, 解析后继依赖关系
        Set<GraphNodeWrapper> nextNodes = currentNodeWrapper.getNextNodes();
        if (CollectionUtils.isEmpty(nextNodes)) {
            return;
        }

        // 将当前节点加入其每个后继节点的前置依赖中
        for (GraphNodeWrapper nextNode : nextNodes) {

            // 当前节点 已经在其 后继结点 的 前置依赖集合 中, 不重复加入
            if (CollectionUtils.isNotEmpty(nextNode.getPreDependNodes()) && nextNode.getPreDependNodes().contains(currentNodeWrapper)) {
                continue;
            }

            if (nextNode.getPreDependNodes() == null) {
                nextNode.setPreDependNodes(Sets.newHashSet());
            }

            // 否则把当前节点加入此后继结点的依赖节点中
            nextNode.getPreDependNodes().add(currentNodeWrapper);

            // 当前节点的outDegree + 1
            currentNodeWrapper.getOutDegree().incrementAndGet();
        }

    }

    /**
     * 将当前节点加入其所有前置节点的后继节点集合中
     */
    private void parseDepend4Node(GraphNodeWrapper currentNodeWrapper) {
        if (currentNodeWrapper.isInit()) {
            return;
        }

        //根据 depend 解析依赖关系
        Set<GraphNodeWrapper> preDependNodes = currentNodeWrapper.getPreDependNodes();
        if (CollectionUtils.isEmpty(preDependNodes)) {
            return;
        }

        // 将当前节点添加到每个前置依赖节点的后继节点集合中
        for (GraphNodeWrapper dependNode : preDependNodes) {

            // 当前节点已经在其前置依赖节点的后继节点集合中, 不重复加入
            if (CollectionUtils.isNotEmpty(dependNode.getNextNodes()) && dependNode.getNextNodes().contains(currentNodeWrapper)) {
                continue;
            }
            //将当前节点添加到前驱节点的后继集合中
            if (dependNode.getNextNodes() == null) {
                dependNode.setNextNodes(Sets.newHashSet());
            }
            dependNode.getNextNodes().add(currentNodeWrapper);

            // 当前节点的indegree+1
            currentNodeWrapper.getInDegree().incrementAndGet();
        }
    }


}
