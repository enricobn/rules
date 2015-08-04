package org.rules.lift.utils

import net.liftweb.http.S
import net.liftweb.http.js.JE.{JsRaw, Str}
import net.liftweb.http.js.{JsExp, JsCmd}
import net.liftweb.http.js.JsCmds.Run

/**
 * Created by enrico on 7/30/15.
 */
object JsMemo {
  def apply(scope: AnyRef, keys: Int) : JsMemo = JsMemo(System.identityHashCode(scope).toString, keys)
}

case class JsMemo(id: String, keys: Int) {

  def clear(key: Str*) =
    if (key.isEmpty) {
      Run(
        s"""
          window.jsmemo.put("$id", new JsMemoMultipleHashMap($keys));
        """)
    } else {
      Run(
        s"""
          var memo = window.jsmemo.get("$id");
          if (memo) {
            memo.remove(${key.map(_.toJsCmd).mkString(",")});
          }
        """.stripMargin)
    }

  def put(key: Str*)(value: Str) = {
    if (key.length != keys) {
      throw new IllegalArgumentException(s"expected $keys keys, but got ${key.length}")
    }
    println(key.map(_.toJsCmd).mkString(","), value)
    Run(
      s"""
        var memo = window.jsmemo.get("$id");
        if (!memo) {
          memo = new JsMemoMultipleHashMap($keys);
          window.jsmemo.put("$id", memo);
        }
        memo.put(${key.map(_.toJsCmd).mkString(",")}, ${value.toJsCmd});
      """.stripMargin
    )
  }

  def get(key: Str*) =
    JsRaw(
      s"""
        (function () {
          var memo = window.jsmemo.get("$id");
          if (memo) {
            return memo.get(${key.map(_.toJsCmd).mkString(",")});
          }
        }())
      """.stripMargin
    )

}
