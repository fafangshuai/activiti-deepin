package ffs.activiti.biz;

import ffs.activiti.util.ActivitiExtensionUtil;
import ffs.chaos.util.Util;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;

import java.util.Map;

/**
 * HelloTask具体实现类
 *
 * @author ffs
 */
public class HelloTaskDelegate implements ActivityBehavior {

  private Expression text;

  public Expression getText() {
    return text;
  }

  public void setText(Expression text) {
    this.text = text;
  }

  @Override
  public void execute(ActivityExecution execution) throws Exception {
    System.out.println("Hello, " + text.getExpressionText());
    Map<String, Object> extensions = ActivitiExtensionUtil.getExtensions(execution.getEngineServices().getRepositoryService(),
        execution.getProcessDefinitionId(), execution.getActivity().getId());
    System.out.println("Extension Properties:");
    Util.printMap(extensions);
    execution.takeAll(execution.getActivity().getOutgoingTransitions(), null);
  }
}
