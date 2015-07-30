package org.rules.lift.utils

import net.liftweb.http.js.JE.{JsRaw, Str}
import net.liftweb.http.js.{JsExp, JsCmd}
import net.liftweb.http.js.JsCmds.Run

/**
 * Created by enrico on 7/30/15.
 */
trait JsMemo {
  def id = System.identityHashCode(this)

  def clearMap(mapId: Str) =
    Run(
      s"""
        if (typeof $$.memo$id != 'undefined') {
          delete $$.memo$id[${mapId.toJsCmd}];
        }
       """.stripMargin)

  def clear() =
    Run(s"$$.memo$id = 'undefined';")

  def put(key: Str, value: Str) =
    Run(
      s"""
        if (typeof $$.memo$id == 'undefined') {
          $$.memo$id = new Object();
        }
        $$.memo$id[${key.toJsCmd}] = ${value.toJsCmd};
      """.stripMargin
    )

  def get(key: Str) =
    JsRaw(
      s"""
        (function() {
          if (typeof $$.memo$id == 'undefined') {
            return 'undefined';
          } else {
            return $$.memo$id[${key.toJsCmd}];
          }
        }())
      """.stripMargin
    )

  def putToMap(mapId: Str, key: Str, value: Str) =
    Run(
      s"""
        if (typeof $$.memo$id == 'undefined') {
          $$.memo$id = new Object();
        }
        if (!(${mapId.toJsCmd} in $$.memo$id)) {
          $$.memo$id[${mapId.toJsCmd}] = new Object();
        }
        if (!(${key.toJsCmd} in $$.memo$id[${mapId.toJsCmd}])) {
          $$.memo$id[${mapId.toJsCmd}][${key.toJsCmd}] = new Object();
        }
        $$.memo$id[${mapId.toJsCmd}][${key.toJsCmd}] = ${value.toJsCmd};
      """.stripMargin
    )

  def getFromMap(mapId: Str, key: Str) =
    JsRaw(
      s"""
        (function() {
          if (typeof $$.memo$id == 'undefined') {
            return 'undefined';
          }
          if (!(${mapId.toJsCmd} in $$.memo$id)) {
            return 'undefined';
          }
          return $$.memo$id[${mapId.toJsCmd}][${key.toJsCmd}];
        }())
      """.stripMargin
    )

  def getMap(mapId: Str) =
    JsRaw(
      s"""
        (function() {
          if (typeof $$.memo$id == 'undefined') {
            return 'undefined';
          }
          return $$.memo$id[${mapId.toJsCmd}];
        }())
      """.stripMargin
    )

}
