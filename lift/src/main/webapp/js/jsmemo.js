/*
 * JsMemoHashMap
 * from http://stackoverflow.com/questions/368280/javascript-hashmap-equivalent
 * added values(), remove(key)
 */
JsMemoHashMap = function(){
  this._dict = [];
}

JsMemoHashMap.prototype._get = function(key){
  for(var i=0, couplet; couplet = this._dict[i]; i++){
    if(couplet[0] === key){
      return couplet;
    }
  }
}

JsMemoHashMap.prototype.put = function(key, value){
  var couplet = this._get(key);
  if(couplet){
    couplet[1] = value;
  }else{
    this._dict.push([key, value]);
  }
  return this; // for chaining
}

JsMemoHashMap.prototype.get = function(key){
  var couplet = this._get(key);
  if(couplet){
    return couplet[1];
  }
}

JsMemoHashMap.prototype.values = function(){
  var result = new Array();
  for(var i=0, couplet; couplet = this._dict[i]; i++){
    result.push(couplet[1]);
  }
  return result;
}

JsMemoHashMap.prototype.remove = function(key){
  var index = -1;
  for(var i=0, couplet; couplet = this._dict[i]; i++){
    if(couplet[0] === key){
      index = i;
      break;
    }
  }
  if (index > -1) {
    this._dict.splice(index, 1);
  }
}
/*
 *    JsMemoMultipleHashMap
 */
JsMemoMultipleHashMap = function (keys) {
    if (typeof keys === 'undefined') {
        throw "keys is mandatory";
    }
    this._map = new JsMemoHashMap();
    this._keys = keys;
}

JsMemoMultipleHashMap.prototype._get = function() {
    var value = this._map;
    for (var i = 0; i < arguments.length; i++) {
        var key = arguments[i];
        value = value.get(key);
        if (!value) {
            break;
        }
    }
    return value;
}

JsMemoMultipleHashMap.prototype.get = function() {
    return this._get.apply(this, arguments);
}

JsMemoMultipleHashMap.prototype._put = function() {
    if (this._keys != arguments.length -1) {
        var s = "";
        for (var i = 0; i < arguments.length-1; i++) {
            if (s.length > 0) {
                s += ",";
            }
            s += arguments[i];
        }
        //console.log("expected " + this._keys + " keys but got " + s);
        throw "expected " + this._keys + " keys but got " + s;
    }
    var map = this._map;
    for (var i = 0; i < arguments.length-2; i++) {
        var key = arguments[i];
        var inner_map = map.get(key);
        if (!inner_map) {
            inner_map = new JsMemoHashMap();
            map.put(key, inner_map);
        }
        map = inner_map;
    }
    map.put(arguments[arguments.length -2], arguments[arguments.length -1]);
}

JsMemoMultipleHashMap.prototype.put = function() {
    this._put.apply(this, arguments);
}

JsMemoMultipleHashMap.prototype._remove = function() {
    var map = this._map;
    for (var i = 0; i < arguments.length -1; i++) {
        var key = arguments[i];
        map = map.get(key);
        if (!map) {
            break;
        }
    }
    map.remove(arguments[arguments.length-1]);
}

JsMemoMultipleHashMap.prototype.remove = function() {
    return this._remove.apply(this, arguments);
}

window.jsmemo = new JsMemoHashMap();