package ffs.activiti;

import org.activiti.engine.RuntimeService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:spring/spring-activiti.xml"})
public class ExtensionTest {
  @Autowired
  private RuntimeService runtimeService;

  @Test
  public void testHelloTask() {
    String key = "testHelloTask";
    runtimeService.startProcessInstanceByKey(key);
  }
}
