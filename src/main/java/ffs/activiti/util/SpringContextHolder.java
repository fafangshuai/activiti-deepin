package ffs.activiti.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SpringContextHolder implements ApplicationContextAware {

  private static ApplicationContext context;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    context = applicationContext;
  }

  public static Object getBean(String name) throws BeansException {return context.getBean(name);}

  public static <T> T getBean(Class<T> requiredType) throws BeansException {return context.getBean(requiredType);}
}
