package ffs.activiti.web;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ffs.activiti.bean.BaseResponse;
import ffs.activiti.converter.CustomizeBpmnJsonConverter;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverterUtil;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ffs.activiti.bean.BaseResponse.error;
import static ffs.activiti.bean.BaseResponse.success;
import static org.apache.commons.lang3.StringUtils.defaultString;

/**
 * 模型控制器
 *
 * @author ffs
 */
@Controller
@RequestMapping("/model/")
public class ModelController {
  private static final Logger logger = LoggerFactory.getLogger(ModelController.class);

  @Autowired
  private RepositoryService repositoryService;

  /**
   * 模型列表
   */
  @RequestMapping(value = "list")
  @ResponseBody
  public BaseResponse list() {
    try {
      List<Model> list = repositoryService.createModelQuery().orderByLastUpdateTime().desc().list();
      return success(list);
    } catch (Exception e) {
      logger.error("Get list failed", e);
      return error();
    }
  }

  /**
   * 创建模型
   *
   * @param name 名称
   * @param key  标识
   * @param desc 描述
   */
  @RequestMapping("create")
  @ResponseBody
  public BaseResponse create(String name, String key, String desc) {
    try {
      Map<String, Object> stencilSetNode = new HashMap<>();
      stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");

      Map<String, Object> editorNode = new HashMap<>();
      editorNode.put("id", "canvas");
      editorNode.put("resourceId", "canvas");
      editorNode.put("stencilset", stencilSetNode);

      Map<String, Object> modelNode = new HashMap<>();
      modelNode.put(ModelDataJsonConstants.MODEL_NAME, name);
      modelNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
      modelNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, defaultString(desc));

      Model model = repositoryService.newModel();
      model.setMetaInfo(JSON.toJSONString(modelNode));
      model.setName(name);
      model.setKey(defaultString(key));
      repositoryService.saveModel(model);
      repositoryService.addModelEditorSource(model.getId(), JSON.toJSONString(editorNode).getBytes("utf-8"));
      return success(model.getId());
    } catch (Exception e) {
      logger.error("Create model failed. modelId", e);
      return error();
    }
  }

  /**
   * 删除模型
   *
   * @param modelId 模型Id
   */
  @RequestMapping("delete")
  @ResponseBody
  public BaseResponse delete(String modelId) {
    try {
      repositoryService.deleteModel(modelId);
      return success();
    } catch (Exception e) {
      logger.error(String.format("Delete model [%s] failed.", modelId), e);
      return error();
    }
  }

  /**
   * 根据模型部署流程
   *
   * @param modelId 模型Id
   */
  @RequestMapping("deploy")
  @ResponseBody
  public BaseResponse deploy(String modelId) {
    try {
      Model model = repositoryService.getModel(modelId);
      ObjectNode modelNode = (ObjectNode) new ObjectMapper().readTree(repositoryService.getModelEditorSource(model.getId()));
      BpmnModel bpmnModel = new CustomizeBpmnJsonConverter().convertToBpmnModel(modelNode);
      byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(bpmnModel);
      String processName = model.getName() + ".bpmn20.xml";
      Deployment deployment = repositoryService.createDeployment().name(model.getName())
                                               .addString(processName, new String(bpmnBytes, Charset.forName("UTF-8"))).deploy();
      return success(deployment.getId());
    } catch (Exception e) {
      logger.error(String.format("Deploy failed. modelId: %s", modelId), e);
      return error();
    }
  }

  /**
   * 导出模型
   *
   * @param modelId 模型Id
   * @param type    类型
   */
  @RequestMapping("export")
  @ResponseBody
  public void export(String modelId, String type, HttpServletResponse response) {
    try {
      Model model = repositoryService.getModel(modelId);
      byte[] modelEditorSource = repositoryService.getModelEditorSource(model.getId());

      JsonNode editorNode = new ObjectMapper().readTree((modelEditorSource));
      BpmnModel bpmnModel = new CustomizeBpmnJsonConverter().convertToBpmnModel(editorNode);

      String filename = "";
      byte[] exportBytes = null;
      String processKey = bpmnModel.getMainProcess().getId();
      if (type.equals("bpmn")) {
        exportBytes = new BpmnXMLConverter().convertToXML(bpmnModel);
        filename = processKey + ".bpmn20.xml";
      }
      IOUtils.copy(new ByteArrayInputStream(exportBytes), response.getOutputStream());
      response.setHeader("Content-Disposition", "attachment; filename=" + filename);
      response.flushBuffer();
    } catch (Exception e) {
      logger.error("Export file failed. modelId: " + modelId, e);
    }
  }

  /**
   * 根据模型id获取所有关联模型id
   *
   * @param modelId 模型Id
   */
  @RequestMapping("getRelatedModels")
  @ResponseBody
  public BaseResponse getRelatedModels(String modelId) {
    try {
      List<String> relatedModels = new ArrayList<>();
      relatedModels.add(modelId);
      ObjectNode modelNode = (ObjectNode) new ObjectMapper().readTree(repositoryService.getModelEditorSource(modelId));
      new CustomizeBpmnJsonConverter().convertToBpmnModel(modelNode);
      ArrayNode childShapes = (ArrayNode) modelNode.get("childShapes");
      fillModelIds(relatedModels, childShapes);
      return success(relatedModels);
    } catch (Exception e) {
      logger.error(String.format("GetRelatedModels failed. modelId: %s", modelId), e);
      return error();
    }
  }

  /**
   * 填充模型id列表，递归处理所有子流程
   *
   * @param relatedModels 模型id列表
   * @param childShapes   每个流程的子元素
   */
  private void fillModelIds(List<String> relatedModels, ArrayNode childShapes) {
    for (JsonNode childShape : childShapes) {
      String stencilId = BpmnJsonConverterUtil.getStencilId(childShape);
      if ("SubProcess".equals(stencilId)) {
        String modelRefId = BpmnJsonConverterUtil.getPropertyValueAsString("modelref", childShape);
        relatedModels.add(modelRefId);
        fillModelIds(relatedModels, (ArrayNode) childShape.get("childShapes"));
      }
    }
  }

}
