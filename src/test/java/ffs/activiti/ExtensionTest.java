package ffs.activiti;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:spring/spring-activiti.xml"})
public class ExtensionTest {
  @Autowired
  private RuntimeService runtimeService;
  @Autowired
  private TaskService taskService;

  @Test
  public void testHelloTask() {
    String key = "testHelloTask";
    runtimeService.startProcessInstanceByKey(key);
  }

  @Test
  public void testSequenceFlow2() {
    String key = "testCustomizeFlow";
    Map<String, Object> vars = new HashMap<>();
    vars.put("ICanFly", false);
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(key, vars);
    List<Task> list = taskService.createTaskQuery().processInstanceId(instance.getId()).list();
    for (Task task : list) {
      System.out.printf("Task {id: %s, name: %s}\n", task.getId(), task.getName());
    }
  }
}
