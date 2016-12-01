package ffs.activiti.converter;

import com.fasterxml.jackson.databind.JsonNode;
import ffs.activiti.util.ActivitiExtensionUtil;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.language.json.converter.BaseBpmnJsonConverter;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 定制化Json转换器
 * 注册自定义的转换器实现
 *
 * @author ffs
 */
public class CustomizeBpmnJsonConverter extends BpmnJsonConverter {
  private static final Logger logger = LoggerFactory.getLogger(CustomizeBpmnJsonConverter.class);
  // 配置
  private static Properties properties = new Properties();
  private static List<String> diRectanglesRef;

  static {
    initConfig();
    initDiRectangles();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String nodeId = (String) entry.getKey();
      String converter = (String) entry.getValue();
      fillConverter(nodeId, converter);
      diRectanglesRef.add(nodeId);
    }
  }

  /**
   * 重写转换方法，组装扩展属性
   *
   * @param modelNode JSON数据
   * @return BpmnModel 模型
   */
  @Override
  public BpmnModel convertToBpmnModel(JsonNode modelNode) {
    BpmnModel bpmnModel = super.convertToBpmnModel(modelNode);
    ActivitiExtensionUtil.assembleExtensionsToModel(modelNode, bpmnModel);
    return bpmnModel;
  }

  /**
   * 加载配置
   */
  private static void initConfig() {
    try {
      properties.load(CustomizeBpmnJsonConverter.class.getClassLoader().getResourceAsStream("extensions.properties"));
    } catch (Exception e) {
      throw new RuntimeException("加载扩展配置文件出错", e);
    }
  }

  /**
   * 初始化 DI_RECTANGLES 引用
   */
  @SuppressWarnings("unchecked")
  private static void initDiRectangles() {
    try {
      Field field = getDiRectanglesField();
      diRectanglesRef = (List<String>) field.get(null);
    } catch (Exception e) {
      throw new RuntimeException("初始化出错", e);
    }
  }

  /**
   * 填充扩展的转换器
   *
   * @param nodeId    组件标识
   * @param converter 转换类
   */
  @SuppressWarnings("unchecked")
  private static void fillConverter(String nodeId, String converter) {
    try {
      Class<BaseBpmnJsonConverter> converterClass = (Class<BaseBpmnJsonConverter>) Class.forName(converter);
      convertersToBpmnMap.put(nodeId, converterClass);
    } catch (ClassNotFoundException e) {
      logger.error("填充扩展组件转换类出错。组件标识：{}， 转换类：{}", nodeId, converter);
    }
  }

  private static Field getDiRectanglesField() {
    Field[] fields = BpmnJsonConverter.class.getDeclaredFields();
    for (Field field : fields) {
      if (field.getName().equals("DI_RECTANGLES")) {
        field.setAccessible(true);
        return field;
      }
    }
    return null;
  }
}
