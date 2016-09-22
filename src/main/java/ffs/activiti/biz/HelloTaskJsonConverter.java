package ffs.activiti.biz;

import com.fasterxml.jackson.databind.JsonNode;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ImplementationType;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.editor.language.json.converter.ServiceTaskJsonConverter;

import java.util.Map;


/**
 * HelloTask转换器，定制节点的行为
 */
public class HelloTaskJsonConverter extends ServiceTaskJsonConverter {

  protected String getStencilId(BaseElement baseElement) {
    return "HelloTask";
  }

  @Override
  protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap) {
    // 定制HelloTask为ServiceTask，并设置相应属性
    ServiceTask task = new ServiceTask();
    task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
    task.setImplementation("ffs.activiti.biz.HelloTaskDelegate");
    addField("text", "hello", elementNode, task);
    return task;
  }
}
