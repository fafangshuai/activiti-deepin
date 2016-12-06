package ffs.activiti.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ExtensionAttribute;
import org.activiti.bpmn.model.ExtensionElement;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.engine.RepositoryService;

import java.util.*;

/**
 * Activiti流程引擎扩展工具类
 *
 * @author ffs
 */
public class ActivitiExtensionUtil {
  /*
   * ==== 由Model转换为bpmn时所需常量 ====
   */
  // 自定义扩展的命名空间
  private static final String CUSTOM_EXTENSION_NS = "http://www.example.com";
  // 自定义扩展的命名空间前缀
  private static final String CUSTOM_EXTENSION_NS_PREFIX = "example";
  // 自定义扩展的元素名
  private static final String CUSTOM_EXTENSION_ELEM_NAME = "item";
  // 自定义扩展的属性名
  private static final String CUSTOM_EXTENSION_ATTR_NAME = "name";

  // 扩展条目换缓存，key: 流程Id.活动Id, value: 扩展元素map
  private static Map<String, Map<String, Object>> extensionItemsCache = new WeakHashMap<String, Map<String, Object>>();

  /**
   * 根据UI的Json数据组装扩展到bpmnModel
   *
   * @param editorNode JSON数据
   * @param bpmnModel  模型数据
   */
  public static void assembleExtensionsToModel(JsonNode editorNode, BpmnModel bpmnModel) {
    if (bpmnModel.getMainProcess() == null) {
      return;
    }
    // 增加命名空间
    bpmnModel.addNamespace(CUSTOM_EXTENSION_NS_PREFIX, CUSTOM_EXTENSION_NS);
    // 获取JSON里的元素
    ArrayNode childShapes = (ArrayNode) editorNode.get("childShapes");
    for (JsonNode node : childShapes) {
      // 获取JSON元素的属性
      JsonNode properties = node.get("properties");
      // 获取活动的id
      String flowElementId = properties.get("overrideid").asText();
      if (flowElementId != null && !flowElementId.isEmpty()) {
        // 获取活动

        FlowElement flowElement = bpmnModel.getFlowElement(flowElementId);
        if (flowElement == null) {
          return;
        }
        // 获取活动的扩展元素，在扩展元素属性中添加自定义扩展
        Map<String, List<ExtensionElement>> extensionElements = flowElement.getExtensionElements();
        ArrayList<ExtensionElement> list = new ArrayList<ExtensionElement>();
        // UI上的自定义简单扩展
        /*JsonNode rawSimpleExtension = properties.get("hello");
        if (rawSimpleExtension != null) {
          String simpleExtension = rawSimpleExtension.asText();
          if (simpleExtension != null && !simpleExtension.isEmpty()) {
            list.add(assembleExtensionElement("hello", simpleExtension));
          }
        }*/

        // UI上自定义复杂扩展
        JsonNode rawComplexExtensions = properties.get("complexextensions");
        if (rawComplexExtensions != null && rawComplexExtensions.size() > 0) {
          ArrayNode complexExtensions = (ArrayNode) rawComplexExtensions;
          if (complexExtensions.size() > 0) {
            for (JsonNode complexExtension : complexExtensions) {
              list.add(assembleExtensionElement(complexExtension.get("name").asText(), complexExtension.get("value").asText()));
            }
          }
        }
        if (list.size() > 0) {
          extensionElements.put(CUSTOM_EXTENSION_ELEM_NAME, list);
        }
      }
    }
  }

  /**
   * 组装ExtensionElement
   *
   * @param name  元素名称
   * @param value 元素值
   */
  public static ExtensionElement assembleExtensionElement(String name, String value) {
    ExtensionElement extensionElement = new ExtensionElement();

    Map<String, List<ExtensionAttribute>> attribute = new HashMap<String, List<ExtensionAttribute>>();
    List<ExtensionAttribute> attributeList = new ArrayList<ExtensionAttribute>();
    ExtensionAttribute extensionAttribute = new ExtensionAttribute();
    extensionAttribute.setName(CUSTOM_EXTENSION_ATTR_NAME);
    extensionAttribute.setValue(name);
    attributeList.add(extensionAttribute);
    attribute.put(CUSTOM_EXTENSION_ATTR_NAME, attributeList);

    extensionElement.setName(CUSTOM_EXTENSION_ELEM_NAME);
    extensionElement.setAttributes(attribute);
    extensionElement.setElementText(value);
    extensionElement.setNamespace(CUSTOM_EXTENSION_NS);
    extensionElement.setNamespacePrefix(CUSTOM_EXTENSION_NS_PREFIX);

    return extensionElement;
  }

  /**
   * 获取指定活动的扩展集合
   *
   * @param model      模型
   * @param activityId 活动Id
   * @return Map
   */
  public static Map<String, Object> getExtensions(BpmnModel model, String activityId) {
    if (model == null || model.getMainProcess() == null || activityId == null || activityId.isEmpty()) {
      return null;
    }
    String cacheKey = cacheKey(model, activityId);
    Map<String, Object> extensionMap = extensionItemsCache.get(cacheKey);
    if (extensionMap != null) {
      return extensionMap;
    }
    extensionMap = new HashMap<String, Object>();
    // 获得活动元素
    FlowElement flowElement = model.getFlowElement(activityId);
    // 获得自定义扩展
    Map<String, List<ExtensionElement>> extensionElementMap = flowElement.getExtensionElements();
    if (extensionElementMap.isEmpty()) {
      extensionItemsCache.put(cacheKey, extensionMap);
      return extensionMap;
    }
    List<ExtensionElement> extensionElementList = extensionElementMap.get(CUSTOM_EXTENSION_ELEM_NAME);
    for (ExtensionElement extensionElement : extensionElementList) {
      String name = extensionElement.getAttributeValue(null, CUSTOM_EXTENSION_ATTR_NAME);
      String value = extensionElement.getElementText();
      extensionMap.put(name, value);
    }
    extensionItemsCache.put(cacheKey, extensionMap);
    return extensionMap;
  }

  /**
   * 获取指定活动的扩展集合
   *
   * @param repositoryService   注入仓库服务
   * @param processDefinitionId 流程定义Id
   * @param activityId          活动Id
   * @return Map
   */
  public static Map<String, Object> getExtensions(RepositoryService repositoryService, String processDefinitionId, String activityId) {
    BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
    return getExtensions(model, activityId);
  }

  /**
   * 获取缓存的key 流程Id.活动Id
   *
   * @param model      模型
   * @param activityId 活动Id
   * @return String
   */
  private static String cacheKey(BpmnModel model, String activityId) {
    return model.getMainProcess().getId() + "." + activityId;
  }

}
