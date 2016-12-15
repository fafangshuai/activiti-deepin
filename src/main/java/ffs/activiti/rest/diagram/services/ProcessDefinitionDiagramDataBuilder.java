package ffs.activiti.rest.diagram.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.bpmn.behavior.BoundaryEventActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.CallActivityBehavior;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.ErrorEventDefinition;
import org.activiti.engine.impl.bpmn.parser.EventSubscriptionDeclaration;
import org.activiti.engine.impl.jobexecutor.TimerDeclarationImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 图形数据建造器
 * 使用方法：new ProcessDefinitionDiagramDataBuilder(x).build(x).format(x)
 */
public class ProcessDefinitionDiagramDataBuilder {
    // 重绘子流程的宽
    private static final int COLLAPSED_SUB_PROC_WIDTH = 100;
    // 重绘子流程的高
    private static final int COLLAPSED_SUB_PROC_HEIGHT = 80;
    // 默认填充
    private static final int DEFAULT_PADDING = 20;
    // 字段名常量：活动
    private static final String ACTIVITIES = "activities";
    // 字段名常量：线
    private static final String SEQUENCE_FLOWS = "sequenceFlows";

    /* 源码中需要的属性，仅仅搬过来 开始*/
    private RepositoryService repositoryService;
    private RuntimeService runtimeService;

    private ProcessInstance processInstance;
    private String processInstanceId;
    private Map<String, ObjectNode> subProcessInstanceMap;
    private List<String> highLightedFlows;
  /* 源码中需要的属性，仅仅搬过来 结束*/

    public ProcessDefinitionDiagramDataBuilder(RepositoryService repositoryService, RuntimeService runtimeService,
                                               ProcessInstance processInstance, String processInstanceId,
                                               Map<String, ObjectNode> subProcessInstanceMap,
                                               List<String> highLightedFlows) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.processInstance = processInstance;
        this.processInstanceId = processInstanceId;
        this.subProcessInstanceMap = subProcessInstanceMap;
        this.highLightedFlows = highLightedFlows;
    }

    // 计算深度
    private int depth = 1;
    // 保存一份结果的引用，方便处理
    private ObjectNode root;

    /**
     * 构造树结构的图形数据
     *
     * @param root       返回结果引用
     * @param definition 流程定义
     * @return this
     */
    public ProcessDefinitionDiagramDataBuilder build(ObjectNode root, ProcessDefinitionEntity definition) {
        resolveActivities(root, definition.getActivities());
        root.put("depth", depth);
        this.root = root;
        return this;
    }

    /**
     * 格式化图形数据，主要是重绘子流程以及调整相关元素坐标
     *
     * @param demandDepth 需要的深度
     * @return this
     */
    public ProcessDefinitionDiagramDataBuilder format(int demandDepth, double scaleRatio) {
        if (root == null) {
            throw new RuntimeException("请先执行构建方法");
        }
        // 操作1：拉平数据  操作2：设置子流程属性为折叠、调整子流程大小、调整子流程关联元素的坐标
        if (demandDepth >= depth) { // 操作1
            flatten(root, (ArrayNode) root.get(ACTIVITIES), depth);
        } else if (demandDepth <= 1) { // 操作2
            repaintActivitiesAndFlows(root, (ArrayNode) root.get(ACTIVITIES), 1);
        } else { // 操作2 -> 操作1
            repaintActivitiesAndFlows(root, (ArrayNode) root.get(ACTIVITIES), demandDepth);
            flatten(root, (ArrayNode) root.get(ACTIVITIES), demandDepth);
        }
        if (scaleRatio > 0) {
            scale(root, (ArrayNode) root.get(ACTIVITIES), scaleRatio);
        }
        return this;
    }

    /**
     * 调整整体大小
     *
     * @param scaleRatio 缩放比率
     * @return this
     */
    public ProcessDefinitionDiagramDataBuilder scale(ObjectNode root, ArrayNode activityArrNode, double scaleRatio) {
        for (JsonNode node : activityArrNode) {
            ObjectNode activityNode = (ObjectNode) node;
            int width = activityNode.get("width").asInt();
            int height = activityNode.get("height").asInt();
            int scaledWidth = (int) (width * scaleRatio);
            int scaledHeight = (int) (height * scaleRatio);
            // activityNode.put("width", scaledWidth);
            // activityNode.put("height", scaledHeight);
            repaintAndShift(activityNode, root, scaledWidth, scaledHeight);
            ArrayNode subActivityArrNode = (ArrayNode) activityNode.get(ACTIVITIES);
            if (subActivityArrNode != null && subActivityArrNode.size() > 0) {
                scale(activityNode, subActivityArrNode, scaleRatio);
            }
        }
        return this;
    }

    private int flattenCurrDepth = 1;

    /**
     * 拉平数据
     *
     * @param root            放在第一级的数据
     * @param activityArrNode 活动节点
     * @param demandDepth     请求层数
     */
    private void flatten(ObjectNode root, ArrayNode activityArrNode, int demandDepth) {
        boolean depthIncrease = false;
        int currentDepth = flattenCurrDepth;
        for (JsonNode node : activityArrNode.deepCopy()) {
            ObjectNode activityNode = (ObjectNode) node;
            ArrayNode subActivityArrNode = (ArrayNode) activityNode.get(ACTIVITIES);
            ArrayNode subFlowArrNode = (ArrayNode) activityNode.get(SEQUENCE_FLOWS);
            if (subActivityArrNode != null && subActivityArrNode.size() > 0) {
                if (demandDepth > currentDepth) {
                    ((ArrayNode) root.get(ACTIVITIES)).addAll(subActivityArrNode);
                    ((ArrayNode) root.get(SEQUENCE_FLOWS)).addAll(subFlowArrNode);
                }
                if (!depthIncrease) {
                    flattenCurrDepth += 1;
                    depthIncrease = true;
                }
                // 递归构造树结构
                flatten(root, subActivityArrNode, demandDepth);
            }
        }
    }

    private int repaintCurrDepth = 1;

    /**
     * 重绘子流程及相关元素坐标
     *
     * @param parentNode      父节点、用来查找兄弟节点
     * @param activityArrNode 一层中的所有节点
     */
    private void repaintActivitiesAndFlows(ObjectNode parentNode, ArrayNode activityArrNode, int demandDepth) {
        boolean depthIncrease = false;
        int currentDepth = repaintCurrDepth;
        for (JsonNode node : activityArrNode) {
            ObjectNode activityNode = (ObjectNode) node;
            String activityType = activityNode.get("properties").get("type").asText();
            // 若果是子流程则处理
            if (activityNode.get(ACTIVITIES) != null && activityNode.get(ACTIVITIES).size() > 0 && "subProcess".equals(activityType)) {
                if (demandDepth <= currentDepth) {
                    activityNode.put("isExpanded", false);
                    activityNode.put("collapsed", true);
                    repaintAndShift(activityNode, parentNode, COLLAPSED_SUB_PROC_WIDTH, COLLAPSED_SUB_PROC_HEIGHT);
                }
                if (!depthIncrease) {
                    repaintCurrDepth += 1;
                    depthIncrease = true;
                }
                // 递归重绘所有的
                repaintActivitiesAndFlows(activityNode, (ArrayNode) node.get(ACTIVITIES), demandDepth);
            }
        }
    }

    /**
     * 真正处理回执和偏移的原子方法
     *
     * @param activityNode 子流程节点
     * @param parentNode   父节点
     */
    private void repaintAndShift(ObjectNode activityNode, ObjectNode parentNode, int scaledWidth, int scaledHeight) {
        int width = activityNode.get("width").asInt();
        int height = activityNode.get("height").asInt();
        int x = activityNode.get("x").asInt();
        int y = activityNode.get("y").asInt();
        int rightBorder = x + width;
        int bottomBorder = y + height;
        int shiftX = 0; // x轴偏移
        int shiftY = 0; // y轴偏移
        // 重设宽高并计算偏移
        if (width != scaledWidth) {
            activityNode.put("width", scaledWidth);
            shiftX = scaledWidth - width;
        }
        if (height != scaledHeight) {
            activityNode.put("height", scaledHeight);
            shiftY = scaledHeight - height;
        }

        // TODO 重绘父级，级联的情况处理
        boolean isRepaintParent = false;
    /*try {
      isRepaintParent = parentNode.get("properties").get("type").asText().equals("subProcess");
    } catch (Exception e) {
      // Ignored
    }*/

        int parentBottomBorder = 0;
        int parentRightBorder = 0;
        // 兄弟节点的坐标调整
        ArrayNode siblingArrNode = (ArrayNode) parentNode.get(ACTIVITIES);
        for (JsonNode node : siblingArrNode) {
            ObjectNode sibling = (ObjectNode) node;
            // 跳过自身
            if (sibling.get("activityId").asText().equals(activityNode.get("activityId").asText())) {
                continue;
            }
            int siblingX = sibling.get("x").asInt();
            int siblingY = sibling.get("y").asInt();
            int siblingWidth = sibling.get("width").asInt();
            int siblingHeight = sibling.get("height").asInt();
            int newSiblingX = siblingX;
            int newSiblingY = siblingY;

            // 只调整右侧元素
            if (siblingY < y + height && siblingY + siblingHeight > y && siblingX >= rightBorder) {
                if (siblingX + shiftX <= x + scaledWidth + DEFAULT_PADDING) {
                    newSiblingX = x + scaledWidth + DEFAULT_PADDING;
                    sibling.put("x", newSiblingX);
                }
            }
            // 只调整下方元素
            if (siblingX >= x && siblingY >= bottomBorder) {
                if (siblingY + shiftY <= y + scaledHeight + DEFAULT_PADDING) {
                    newSiblingY = y + scaledHeight + DEFAULT_PADDING;
                    sibling.put("y", newSiblingY);
                }
            }

            if (isRepaintParent) {
                if (parentRightBorder < sibling.get("width").asInt() + newSiblingX) {
                    parentRightBorder = sibling.get("width").asInt() + newSiblingX;
                }
                if (parentBottomBorder < sibling.get("height").asInt() + newSiblingY) {
                    parentBottomBorder = sibling.get("height").asInt() + newSiblingY;
                }
            }
        }

        if (isRepaintParent) {
            parentNode.put("width", parentRightBorder - parentNode.get("x").asInt() + DEFAULT_PADDING);
            parentNode.put("height", parentBottomBorder - parentNode.get("y").asInt() + DEFAULT_PADDING);
        }
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * 转换流程定义到json格式结果
     *
     * @param parentNode   父节点
     * @param activityList 流程定义的活动集合
     */
    private void resolveActivities(ObjectNode parentNode, List<ActivityImpl> activityList) {
        boolean depthIncrease = false;
        // 活动数组
        ArrayNode activityArrNode = new ObjectMapper().createArrayNode();
        // 线数组
        ArrayNode flowArrNode = new ObjectMapper().createArrayNode();
        for (ActivityImpl activity : activityList) {
            ObjectNode activityNode = getActivityNode(activity);
            activityArrNode.add(activityNode);

            ArrayNode outFlows = new ObjectMapper().createArrayNode();
            ArrayNode inFlows = new ObjectMapper().createArrayNode();

            for (PvmTransition outFlow : activity.getOutgoingTransitions()) {
                ObjectNode flowNode = getFlowNode(outFlow, activity);
                flowArrNode.add(flowNode);
                outFlows.add(outFlow.getId());
            }

            for (PvmTransition inFlow : activity.getIncomingTransitions()) {
                inFlows.add(inFlow.getId());
            }

            activityNode.set("incomingTransitions", inFlows);
            activityNode.set("outgoingTransitions", outFlows);

            if (activity.getActivities().size() > 0) {
                if (!depthIncrease) {
                    depth += 1;
                    depthIncrease = true;
                }
                // 递归构造树结构
                resolveActivities(activityNode, activity.getActivities());
            }
        }
        parentNode.set(ACTIVITIES, activityArrNode);
        parentNode.set(SEQUENCE_FLOWS, flowArrNode);
    }

    // 获取线，copy源码
    private ObjectNode getFlowNode(PvmTransition sequenceFlow, ActivityImpl activity) {
        String flowName = (String) sequenceFlow.getProperty("name");
        boolean isHighLighted = (highLightedFlows.contains(sequenceFlow.getId()));
        boolean isConditional = sequenceFlow.getProperty(BpmnParse.PROPERTYNAME_CONDITION) != null &&
                !((String) activity.getProperty("type")).toLowerCase().contains("gateway");
        boolean isDefault = sequenceFlow.getId().equals(activity.getProperty("default"))
                && ((String) activity.getProperty("type")).toLowerCase().contains("gateway");

        List<Integer> waypoints = ((TransitionImpl) sequenceFlow).getWaypoints();
        ArrayNode xPointArray = new ObjectMapper().createArrayNode();
        ArrayNode yPointArray = new ObjectMapper().createArrayNode();
        for (int i = 0; i < waypoints.size(); i += 2) { // waypoints.size()
            // minimally 4: x1, y1,
            // x2, y2
            xPointArray.add(waypoints.get(i));
            yPointArray.add(waypoints.get(i + 1));
        }

        ObjectNode flowJSON = new ObjectMapper().createObjectNode();
        flowJSON.put("id", sequenceFlow.getId());
        flowJSON.put("name", flowName);
        flowJSON.put("flow", "(" + sequenceFlow.getSource().getId() + ")--"
                + sequenceFlow.getId() + "-->("
                + sequenceFlow.getDestination().getId() + ")");

        if (isConditional)
            flowJSON.put("isConditional", isConditional);
        if (isDefault)
            flowJSON.put("isDefault", isDefault);
        if (isHighLighted)
            flowJSON.put("isHighLighted", isHighLighted);

        flowJSON.set("xPointArray", xPointArray);
        flowJSON.set("yPointArray", yPointArray);
        return flowJSON;
    }

    // 获取活动，copy源码
    private ObjectNode getActivityNode(ActivityImpl activity) {
        ObjectNode activityNode = new ObjectMapper().createObjectNode();

        // Gather info on the multi instance marker
        String multiInstance = (String) activity.getProperty("multiInstance");
        if (multiInstance != null) {
            if (!"sequential".equals(multiInstance)) {
                multiInstance = "parallel";
            }
        }

        ActivityBehavior activityBehavior = activity.getActivityBehavior();
        // Gather info on the collapsed marker
        Boolean collapsed = (activityBehavior instanceof CallActivityBehavior);
        Boolean expanded = (Boolean) activity.getProperty(BpmnParse.PROPERTYNAME_ISEXPANDED);
        if (expanded != null) {
            collapsed = !expanded;
        }

        Boolean isInterrupting = null;
        if (activityBehavior instanceof BoundaryEventActivityBehavior) {
            isInterrupting = ((BoundaryEventActivityBehavior) activityBehavior).isInterrupting();
        }

        Map<String, Object> properties = activity.getProperties();
        ObjectNode propertiesJSON = new ObjectMapper().createObjectNode();
        for (String key : properties.keySet()) {
            Object prop = properties.get(key);
            if (prop instanceof String)
                propertiesJSON.put(key, (String) properties.get(key));
            else if (prop instanceof Integer)
                propertiesJSON.put(key, (Integer) properties.get(key));
            else if (prop instanceof Boolean)
                propertiesJSON.put(key, (Boolean) properties.get(key));
            else if ("initial".equals(key)) {
                ActivityImpl act = (ActivityImpl) properties.get(key);
                propertiesJSON.put(key, act.getId());
            } else if ("timerDeclarations".equals(key)) {
                ArrayList<TimerDeclarationImpl> timerDeclarations = (ArrayList<TimerDeclarationImpl>) properties.get(key);
                ArrayNode timerDeclarationArray = new ObjectMapper().createArrayNode();

                if (timerDeclarations != null)
                    for (TimerDeclarationImpl timerDeclaration : timerDeclarations) {
                        ObjectNode timerDeclarationJSON = new ObjectMapper().createObjectNode();

                        timerDeclarationJSON.put("isExclusive", timerDeclaration.isExclusive());
                        if (timerDeclaration.getRepeat() != null)
                            timerDeclarationJSON.put("repeat", timerDeclaration.getRepeat());

                        timerDeclarationJSON.put("retries", String.valueOf(timerDeclaration.getRetries()));
                        timerDeclarationJSON.put("type", timerDeclaration.getJobHandlerType());
                        timerDeclarationJSON.put("configuration", timerDeclaration.getJobHandlerConfiguration());
                        //timerDeclarationJSON.put("expression", timerDeclaration.getDescription());

                        timerDeclarationArray.add(timerDeclarationJSON);
                    }
                if (timerDeclarationArray.size() > 0)
                    propertiesJSON.set(key, timerDeclarationArray);
                // TODO: implement getting description
            } else if ("eventDefinitions".equals(key)) {
                ArrayList<EventSubscriptionDeclaration> eventDefinitions = (ArrayList<EventSubscriptionDeclaration>) properties.get(key);
                ArrayNode eventDefinitionsArray = new ObjectMapper().createArrayNode();

                if (eventDefinitions != null) {
                    for (EventSubscriptionDeclaration eventDefinition : eventDefinitions) {
                        ObjectNode eventDefinitionJSON = new ObjectMapper().createObjectNode();

                        if (eventDefinition.getActivityId() != null)
                            eventDefinitionJSON.put("activityId", eventDefinition.getActivityId());

                        eventDefinitionJSON.put("eventName", eventDefinition.getEventName());
                        eventDefinitionJSON.put("eventType", eventDefinition.getEventType());
                        eventDefinitionJSON.put("isAsync", eventDefinition.isAsync());
                        eventDefinitionJSON.put("isStartEvent", eventDefinition.isStartEvent());
                        eventDefinitionsArray.add(eventDefinitionJSON);
                    }
                }

                if (eventDefinitionsArray.size() > 0)
                    propertiesJSON.put(key, eventDefinitionsArray);

                // TODO: implement it
            } else if ("errorEventDefinitions".equals(key)) {
                ArrayList<ErrorEventDefinition> errorEventDefinitions = (ArrayList<ErrorEventDefinition>) properties.get(key);
                ArrayNode errorEventDefinitionsArray = new ObjectMapper().createArrayNode();

                if (errorEventDefinitions != null) {
                    for (ErrorEventDefinition errorEventDefinition : errorEventDefinitions) {
                        ObjectNode errorEventDefinitionJSON = new ObjectMapper().createObjectNode();

                        if (errorEventDefinition.getErrorCode() != null)
                            errorEventDefinitionJSON.put("errorCode", errorEventDefinition.getErrorCode());
                        else
                            errorEventDefinitionJSON.putNull("errorCode");

                        errorEventDefinitionJSON.put("handlerActivityId",
                                errorEventDefinition.getHandlerActivityId());

                        errorEventDefinitionsArray.add(errorEventDefinitionJSON);
                    }
                }

                if (errorEventDefinitionsArray.size() > 0)
                    propertiesJSON.put(key, errorEventDefinitionsArray);
            }

        }

        if ("callActivity".equals(properties.get("type"))) {
            CallActivityBehavior callActivityBehavior = null;

            if (activityBehavior instanceof CallActivityBehavior) {
                callActivityBehavior = (CallActivityBehavior) activityBehavior;
            }

            if (callActivityBehavior != null) {
                propertiesJSON.put("processDefinitonKey", callActivityBehavior.getProcessDefinitonKey());

                // get processDefinitonId from execution or get last processDefinitonId
                // by key
                ArrayNode processInstanceArray = new ObjectMapper().createArrayNode();
                if (processInstance != null) {
                    List<Execution> executionList = runtimeService.createExecutionQuery()
                                                                  .processInstanceId(processInstanceId)
                                                                  .activityId(activity.getId()).list();
                    if (!executionList.isEmpty()) {
                        for (Execution execution : executionList) {
                            ObjectNode processInstanceJSON = subProcessInstanceMap.get(execution.getId());
                            processInstanceArray.add(processInstanceJSON);
                        }
                    }
                }

                // If active activities nas no instance of this callActivity then add
                // last definition
                if (processInstanceArray.size() == 0 && StringUtils.isNotEmpty(callActivityBehavior.getProcessDefinitonKey())) {
                    // Get last definition by key
                    ProcessDefinition lastProcessDefinition = repositoryService
                            .createProcessDefinitionQuery()
                            .processDefinitionKey(callActivityBehavior.getProcessDefinitonKey())
                            .latestVersion().singleResult();

                    // TODO: unuseful fields there are processDefinitionName, processDefinitionKey
                    if (lastProcessDefinition != null) {
                        ObjectNode processInstanceJSON = new ObjectMapper().createObjectNode();
                        processInstanceJSON.put("processDefinitionId", lastProcessDefinition.getId());
                        processInstanceJSON.put("processDefinitionKey", lastProcessDefinition.getKey());
                        processInstanceJSON.put("processDefinitionName", lastProcessDefinition.getName());
                        processInstanceArray.add(processInstanceJSON);
                    }
                }

                if (processInstanceArray.size() > 0) {
                    propertiesJSON.put("processDefinitons", processInstanceArray);
                }
            }
        }

        activityNode.put("activityId", activity.getId());
        activityNode.put("properties", propertiesJSON);
        if (multiInstance != null)
            activityNode.put("multiInstance", multiInstance);
        if (collapsed)
            activityNode.put("collapsed", collapsed);
        if (isInterrupting != null)
            activityNode.put("isInterrupting", isInterrupting);

        activityNode.put("x", activity.getX());
        activityNode.put("y", activity.getY());
        activityNode.put("width", activity.getWidth());
        activityNode.put("height", activity.getHeight());

        return activityNode;
    }
}
