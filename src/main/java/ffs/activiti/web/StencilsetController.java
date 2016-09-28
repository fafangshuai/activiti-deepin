package ffs.activiti.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.activiti.engine.ActivitiException;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

/**
 * 加载前端UI配置文件控制器
 *
 * @author ffs
 */
@RestController
public class StencilsetController {
  private static final String PROP_PACKAGES = "propertyPackages";
  private static final String PROP_STENCILS = "stencils";
  private static final String PROP_RULES = "rules";
  private static final String PROP_RULES_CONNECTION = "connectionRules";

  /**
   * 获取配置
   * 将默认配置和扩展配置组装后返回
   */
  @RequestMapping(value = "/editor/customize/stencilset", method = RequestMethod.GET, produces = "application/json;charset=utf-8")
  @ResponseBody
  public String getStencilset() {
    InputStream stencilsetStream = this.getClass().getClassLoader().getResourceAsStream("stencilset.json");
    InputStream stencilsetExtStream = this.getClass().getClassLoader().getResourceAsStream("stencilset-ext.json");
    try {
      String rawJson = IOUtils.toString(stencilsetStream, "utf-8");
      String extJson = IOUtils.toString(stencilsetExtStream, "utf-8");
      // 原生JSON
      JSONObject raw = JSON.parseObject(rawJson);
      // 扩展JSON
      JSONObject ext = JSON.parseObject(extJson);
      // 添加扩展属性
      raw.getJSONArray(PROP_PACKAGES).addAll(ext.getJSONArray(PROP_PACKAGES));
      raw.getJSONArray(PROP_STENCILS).addAll(ext.getJSONArray(PROP_STENCILS));
      raw.getJSONObject(PROP_RULES).getJSONArray(PROP_RULES_CONNECTION)
          .addAll(ext.getJSONObject(PROP_RULES).getJSONArray(PROP_RULES_CONNECTION));
      return JSON.toJSONString(raw);
    } catch (IOException e) {
      throw new ActivitiException("Error while loading stencil set", e);
    }
  }
}
