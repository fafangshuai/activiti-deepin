window.CTX = "/activiti-deepin";
window.BASE_URL = "http://localhost:8088" + CTX;

(function () {
  window.UrlParam = {};
  var query_string = {};
  var query = window.location.search.substring(1);
  if (query) {
    var vars = query.split("&");
    for (var i = 0; i < vars.length; i++) {
      var pair = vars[i].split("=");
      query_string[pair[0]] = pair[1];
    }
  }
  UrlParam.get = function (key) {
    return query_string[key];
  };
  UrlParam.set = function (key, val) {
    var old = query_string[key];
    query_string[key] = val;
    return old;
  };
  UrlParam.format = function () {
    var s = "?";
    for (var i in query_string) {
      s += i + "=" + query_string[i] + "&";
    }
    return s.substring(0, s.length - 1);
  };
})();

function formatFromDate(date) {
  return date.toLocaleString("zh-CN", {hour12: false});
}

function formatFromMs(ms) {
  return formatFromDate(new Date(ms));
}
