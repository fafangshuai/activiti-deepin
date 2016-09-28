package ffs.activiti.biz;

import com.fasterxml.jackson.databind.JsonNode;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.editor.language.json.converter.SequenceFlowJsonConverter;

import java.util.Map;

public class SequenceFlow2JsonConverter extends SequenceFlowJsonConverter {

  @Override
  protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap) {
    SequenceFlow flow = (SequenceFlow) super.convertJsonToElement(elementNode, modelNode, shapeMap);
    flow.setConditionExpression("${ICanFly}");
    return flow;
  }
}
