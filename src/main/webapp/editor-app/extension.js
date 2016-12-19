/**
 * 外部扩展
 */
(function ($) {
  $(function () {
    getRelatedModelsService(renderSelect);
  });

  // 初始化下拉框
  function renderSelect(data) {
    var $select = $("#relatedModelsSelect");
    var selected = UrlParam.get("modelId");
    for (var i = 0, len = data.length; i < len; i++) {
      var item = data[i];
      var name = item.name ? item.name : item.id;
      var selectedText = item.id == selected ? 'selected="selected"' : '';
      var option = $('<option value="' + item.id + '"' + selectedText + '>' + name + '</option>');
      $select.append(option);
    }
    $select.on("change", function () {
      var modelId = $(this).val();
      gotoSubProc(modelId);
    });
  }

  // 获取数据
  function getRelatedModelsService(callback) {
    var rootId = UrlParam.get("rootId") ? UrlParam.get("rootId") : UrlParam.get("modelId");
    $.getJSON(BASE_URL + "/model/getRelatedModels", {modelId: rootId}, function (resp) {
      callback(resp.data);
    });
  }

  // 跳转到子流程
  function gotoSubProc(modelId) {
    var rootId = UrlParam.get("rootId");
    if (!rootId) {
      UrlParam.set("rootId", UrlParam.get("modelId"));
    }
    UrlParam.set("modelId", modelId);
    location.href = location.origin + location.pathname + UrlParam.format();
  }

  jQuery.extend({
    addExtensions: function (ngScope) {
    var editor = ngScope.editor;
    editor.registerOnEvent("dblclick", function (evenshapet, shapeCls) {
      var shape = shapeCls.toJSON();
      if (shape.stencil.id == "SubProcRef") {
        var modelId = shape.properties.modelref;
        gotoSubProc(modelId);
      }
    });
  }});
})(jQuery);
