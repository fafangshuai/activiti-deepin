var Path = (function () {
  var host = location.host;
  var origin = location.origin;
  var contextPath = getContextPath();

  function getContextPath() {
    var pathname = location.pathname;
    if (pathname === "/") {
      return "/";
    }
    if (pathname.lastIndexOf("/") === 0) {
      return "/";
    }
    return "/" + pathname.split("/")[1];
  }

  return {
    prefix: function () {
      return host + contextPath;
    },
    prefixWithHttp: function () {
      return origin + contextPath;
    },
    context: function () {
      return contextPath;
    }
  }
})();

function formatFromDate(date) {
  return date.toLocaleString("zh-CN", {hour12: false});
}

function formatFromMs(ms) {
  return formatFromDate(new Date(ms));
}
