package ffs.activiti.biz;

import ffs.activiti.util.ActivitiExtensionUtil;
import ffs.chaos.util.Util;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;

import java.util.Map;

/**
 * HelloTask具体实现类
 *
 * @author ffs
 */
public class HelloTaskDelegate implements JavaDelegate {

  private Expression text;

  public Expression getText() {
    return text;
  }

  public void setText(Expression text) {
    this.text = text;
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    ExecutionEntity entity = (ExecutionEntity) execution;
    Map<String, Object> extensions = ActivitiExtensionUtil.getExtensions(execution.getEngineServices().getRepositoryService(),
        execution.getProcessDefinitionId(), entity.getActivity().getId());
    System.out.println("Extension Properties:");
    Util.printMap(extensions);
    System.out.println("Hello, " + text.getExpressionText());
  }
}
