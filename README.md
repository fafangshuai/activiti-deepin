activiti-deepin
===

>流程引擎Activiti深度定制

### HOW-TO?

#### 添加定制UI
- 向stencilset-ext.json文件的stencils数组中添加元素，可以参考stencilset.json中的元素

  stencils数组中元素介绍:

  Properties       | Description
  ---------------- |------------------
  type             | 默认node，不能改变
  id               | 扩展节点的标识
  title            | 名称
  description      | 描述
  view             | 节点的形状，使用svg。自定义形状需要修改此属性
  icon             | 图标，目录前缀 editor-app/stencilsets/bpmn2.0/icons。自定义图标需要放在此目录下
  groups           | 扩展节点所在的组，在菜单中显示
  propertyPackages | 扩展节点属性配置

- 向extensions.properties文件中添加配置
  key要和`stencils`数组元素中id对应，value为该扩展节点的自定义转换器类的全限定名

- 实现扩展节点的自定义转换器类
  转换器类必须是`org.activiti.editor.language.json.converter.BaseBpmnJsonConverter`的子类

#### 扩展属性
- 向stencilset-ext.json文件的propertyPackages数组中添加元素，可以参考stencilset.json中的元素

  propertyPackages数组中元素介绍：

  Properties       | Description
  ---------------- |------------------
  name             | 扩展属性包名，以小写package结尾
  id               | 扩展属性标识
  title            | 显示名称
  description      | 描述
  value            | 默认值
  type             | 类型。类型分为简单类型(String、Boolean)，复杂类型(multiplecomplex、Complex等)

- 简单类型可以直接使用，复杂类型则需要自定义其实现（基于angularjs-1.2）
  以扩展多个键值对的复杂属性为例：

  需要在properties.js中定制该复杂属性扩展，属性名格式 `oryx-扩展属性id-扩展属性类型`，值为自定义的angular模板