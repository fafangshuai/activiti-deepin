package ffs.activiti.web;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 定制UI加载配置文件请求拦截器
 * 拦截activiti默认的请求到自定义的控制器
 *
 * @author ffs
 */
public class StencilsetInterceptor extends HandlerInterceptorAdapter {
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    response.sendRedirect(request.getContextPath() + "/service/editor/customize/stencilset");
    return false;
  }
}
