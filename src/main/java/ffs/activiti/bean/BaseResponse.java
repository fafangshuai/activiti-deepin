package ffs.activiti.bean;

/**
 * 基本响应对象
 */
public class BaseResponse {
  private static final String SUCCESS = "0";
  private static final String ERROR = "-1";

  // 状态码
  private String code;
  // 状态信息
  private String text;
  // 数据
  private Object data;

  public BaseResponse() {
  }

  public BaseResponse(String code, String text, Object data) {
    this.code = code;
    this.text = text;
    this.data = data;
  }

  public static BaseResponse success() {
    return success(null);
  }

  public static BaseResponse success(Object data) {
    return new BaseResponse(SUCCESS, null, data);
  }

  public static BaseResponse error() {
    return new BaseResponse(ERROR, null, null);
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }
}
