package org.rules.lift

import net.liftweb.http.js.JsCmd
import org.scalatest.FunSuite

/**
 * Created by enrico on 6/23/15.
 */
class JsGroupSuite extends FunSuite {

  test("select & deSelect") {
    val g : JsGroup = new JsSimpleGroup(JQueryById("module"), JQueryHide)
    assert(g.select("1").toJsCmd == "$(\"#module-1\").show();")
    assert(g.select("2").toJsCmd == "$(\"#module-2\").show();")
    assert(g.deSelect("1").toJsCmd == "$(\"#module-1\").hide();")
    assert(g.deSelect("2").toJsCmd == "$(\"#module-2\").hide();")
  }
}
