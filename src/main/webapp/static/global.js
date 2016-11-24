window.CTX = "/activiti-deepin";
window.BASE_URL = "http://localhost:8088" + CTX;


function formatFromDate(date) {
  return date.toLocaleString("zh-CN", {hour12: false});
}

function formatFromMs(ms) {
  return formatFromDate(new Date(ms));
}
