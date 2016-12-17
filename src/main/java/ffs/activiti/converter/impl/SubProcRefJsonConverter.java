package ffs.activiti.converter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ffs.activiti.util.SpringContextHolder;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.editor.language.json.converter.BpmnJsonConverterUtil;
import org.activiti.editor.language.json.converter.SubProcessJsonConverter;
import org.activiti.engine.RepositoryService;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class SubProcRefJsonConverter extends SubProcessJsonConverter {

  private static RepositoryService repositoryService = SpringContextHolder.getBean(RepositoryService.class);

  @Override
  protected void convertElementToJson(ObjectNode propertiesNode, BaseElement baseElement) {
    super.convertElementToJson(propertiesNode, baseElement);
  }

  @Override
  protected SubProcess convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap) {
    try {
      ObjectNode mutableNode = (ObjectNode) elementNode;
      String modelId = BpmnJsonConverterUtil.getPropertyValueAsString("modelref", elementNode);
      String subProcId = BpmnJsonConverterUtil.getElementId(elementNode);
      String resourceId = BpmnJsonConverterUtil.getValueAsString("resourceId", elementNode);
      byte[] modelEditorSource = repositoryService.getModelEditorSource(modelId);
      ObjectNode subProcNode = (ObjectNode) new ObjectMapper().readTree((modelEditorSource));
      // 设置元素类型
      ObjectNode stencil = (ObjectNode) mutableNode.get("stencil");
      stencil.put("id", "SubProcess");
      // 设置子流程属性
      ObjectNode properties = (ObjectNode) mutableNode.get("properties");
      properties.put("overrideid", subProcId);
      // 设置流转
      ArrayNode childShapes = (ArrayNode) subProcNode.get("childShapes");
      mutableNode.set("childShapes", childShapes);
      shapeMap.put(resourceId, mutableNode);
      for (JsonNode childShape : childShapes) {
        String stencilId = BpmnJsonConverterUtil.getStencilId(childShape);
        if (!STENCIL_SEQUENCE_FLOW.equals(stencilId)) {
          String childResId = BpmnJsonConverterUtil.getValueAsString("resourceId", childShape);
          shapeMap.put(childResId, childShape);
        }
      }
      return (SubProcess) super.convertJsonToElement(elementNode, modelNode, shapeMap);
    } catch (IOException e) {
      throw new RuntimeException("转换子流程出错", e);
    }
  }

  @Override
  protected String getStencilId(BaseElement baseElement) {
    return "SubProcRef";
  }
}
