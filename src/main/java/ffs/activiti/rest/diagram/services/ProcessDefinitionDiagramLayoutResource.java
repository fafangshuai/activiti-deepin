package ffs.activiti.rest.diagram.services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProcessDefinitionDiagramLayoutResource extends BaseProcessDefinitionDiagramLayoutResource {

  @RequestMapping(value="/process-definition/{processDefinitionId}/diagram-layout/{depth}", method = RequestMethod.GET, produces = "application/json")
  public ObjectNode getDiagram(@PathVariable String processDefinitionId, @PathVariable Integer depth) {
    return getDiagramNode(null, processDefinitionId, depth);
  }
}
