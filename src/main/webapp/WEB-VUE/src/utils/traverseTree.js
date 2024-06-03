const traverseTree=(node, flat)=>{
  var stack = [], res = [];
  if (!node) return;
   stack.push({"dom": node, "dep": 0, "path": "$", "name": "根节点"});
  var tmpNode;
  while (stack.length > 0) {
    tmpNode = stack.pop();
    res.push({
      "name": tmpNode.name,
      "pid": tmpNode.pid,
      "path": tmpNode.path,
      "dep": tmpNode.dep
    });
    traverseNode2(tmpNode, tmpNode.dep);
  }

  // 遍历单个节点
  function traverseNode2(node, dep) {
    debugger
    var doc = node.dom;
    if (Object.prototype.toString.call(doc) === '[object Object]') {
      for (var val in doc) {
        var cpath = (node.path + "." + val);
        stack.push({
          "dom": doc[val],
          "dep": (dep + 1),
          "path": cpath,
          "pid": node.path,
          "name": val
        });
      }
    }
    if (Object.prototype.toString.call(doc) === '[object Array]') {
      for (let i = 0; i < doc.length; i++) {
        stack.push({
          "dom": doc[i],
          "dep": (dep + 1),
          "path": (node.path + "[" + i + "]"),
          "pid": node.path,
          "name": node.name + "[" + i + "]"
        });
      }
    }
  }

  // 树形结构转换
  function flat2tree(jsonData) {
    var result = [], temp = {}, i = 0, j = 0, len = jsonData.length;
    for (; i < len; i++)
      temp[jsonData[i]['path']] = jsonData[i]
    for (; j < len; j++) {
      var cel = jsonData[j]
      var tcel = temp[cel['pid']]
      if (tcel) {
        if (!tcel['children']) {
          tcel['children'] = [];
        }
        tcel['children'].push(cel)
      } else {
        result.push(cel);
      }
    }
    return result;
  }

  return flat ? flat2tree(res) : res;
}

export {
  traverseTree
}
