package org.vertex.flow.validator;

import org.vertex.flow.domain.exception.NodeCheckException;
import org.vertex.flow.domain.exception.OperatorCheckException;
import org.vertex.flow.domain.model.Node;
import org.vertex.flow.util.node.NodeUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;

public class NodeValidator {

    private  NodeValidator() {
    }


    public static void validateNode(Object node) throws NodeCheckException, OperatorCheckException {
        if (node == null) {
            throw new NodeCheckException("node is null");
        }
        if (! (node instanceof Node)) {
            throw new NodeCheckException("node must be of type Node");
        }
        validateNode((Node)node);
    }

    private static void validateNode(Node node) throws NodeCheckException {

        // 检查节点绑定的算子的合法性
        checkNodeOperator(node);

        // 检查节点本身的合法性
        checkPreDepends(node);
    }

    /**
     * 节点的算子合法性检查
     */
    private static void checkNodeOperator(Node node) {
        OperatorValidator.validateOperator(node.getOperator());
    }

    /**
     * 1. 前置依赖节点的个数,和本节点的 算子的入参个数 相等
     * 2. 每个前置依赖节点的返回类型, 与算子对应位置的入参类型兼容
     */
    private static void checkPreDepends(Node node) throws NodeCheckException {
        Set<Node> preDependNodes = node.getPreDependNodes();
        Method opMethod = NodeUtil.findOperatorExecuteMethod(node);
        if (opMethod == null) {
            throw new NodeCheckException("@operatorExecute-method not found");
        }

        Parameter[] parameters = opMethod.getParameters();
        if (parameters.length != preDependNodes.size()) {
            throw new NodeCheckException("number of @operatorExecute-method parameters does not match the number of pre-depend nodes");
        }
        for (int i = 0; i < node.getOrderedOperatorInputNodes().size(); i++) {
            Node inputNode = node.getOrderedOperatorInputNodes().get(i);
            Class<?> returnTypeOfInput = NodeUtil.getReturnTypeForOperatorExecuteMethod(inputNode);
            Parameter parameter = parameters[i];

            if (!parameter.getType().isAssignableFrom(returnTypeOfInput)) {
                throw new NodeCheckException("pre-depend node " + inputNode.getNodeId() + "return type does not match");
            }
        }
    }

}
