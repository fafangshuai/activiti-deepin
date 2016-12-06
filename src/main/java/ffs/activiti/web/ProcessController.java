package ffs.activiti.web;

import ffs.activiti.bean.BaseResponse;
import ffs.activiti.bean.Process;
import ffs.activiti.bean.ProcessDef;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

import static ffs.activiti.bean.BaseResponse.error;
import static ffs.activiti.bean.BaseResponse.success;

@Controller
@RequestMapping("/process/")
public class ProcessController {
  private static final Logger logger = LoggerFactory.getLogger(ProcessController.class);

  @Autowired
  private RuntimeService runtimeService;
  @Autowired
  private RepositoryService repositoryService;

  @RequestMapping("def/list")
  @ResponseBody
  public BaseResponse defList() {
    try {
      List<ProcessDef> result = new ArrayList<>();
      List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery()
          .latestVersion().orderByProcessDefinitionKey().asc().list();
      for (ProcessDefinition definition : list) {
        ProcessDef def = new ProcessDef();
        BeanUtils.copyProperties(definition, def);
        result.add(def);
      }
      return success(result);
    } catch (Exception e) {
      logger.error("Get list failed", e);
      return error();
    }
  }

  @RequestMapping("def/delete")
  @ResponseBody
  public BaseResponse deleteDef(String deploymentId) {
    try {
      repositoryService.deleteDeployment(deploymentId, true);
      return success();
    } catch (Exception e) {
      logger.error(String.format("Delete failed. deploymentId: %s", deploymentId), e);
      return error();
    }
  }

  @RequestMapping(value = "list")
  @ResponseBody
  public BaseResponse list() {
    try {
      List<Process> result = new ArrayList<>();
      List<ProcessInstance> list = runtimeService.createProcessInstanceQuery().list();
      for (ProcessInstance item : list) {
        Process process = new Process();
        BeanUtils.copyProperties(item, process);
        try {
          ProcessDefinitionEntity definition = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(item.getProcessDefinitionId());
          ActivityImpl activity = definition.findActivity(item.getActivityId());
          Object name = activity.getProperty("name");
          process.setActivityName(name == null ? "" : String.valueOf(name));
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
        result.add(process);
      }
      return success(result);
    } catch (Exception e) {
      logger.error("Get list failed", e);
      return error();
    }
  }
}
