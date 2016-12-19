/**
 * 外部扩展
 */
(function ($) {
  $(function () {
    getRelatedModelsService(renderSelect);
  });

  // 处理查看
  function handleView() {
    var palette = $("#paletteHelpWrapper");
    var canvas = $("#canvasHelpWrapper");
    var properties = $("#propertiesHelpWrapper");
    var buttons = jQuery("div.btn-toolbar.pull-left.ng-scope").find("button:lt(2)");
    if (UrlParam.get("view") == "true") {
      palette.hide();
      buttons.hide();
      canvas.addClass("full-width");
      properties.addClass("full-width");

    } else {
      palette.show();
      buttons.show();
      canvas.removeClass("full-width");
      properties.removeClass("full-width");
    }
  }

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

  // 注册双击事件
  function registerDBClickEventOnSubProcRef(ngScope) {
    var editor = ngScope.editor;
    editor.registerOnEvent("dblclick", function (event, shapeCls) {
      var shape = shapeCls.toJSON();
      if (shape.stencil.id == "SubProcRef") {
        var modelId = shape.properties.modelref;
        gotoSubProc(modelId);
      }
    });
  }

  jQuery.extend({
    ngPostHandler: function (ngScope) {
      registerDBClickEventOnSubProcRef(ngScope);
      handleView();
    }
  });
})(jQuery);
