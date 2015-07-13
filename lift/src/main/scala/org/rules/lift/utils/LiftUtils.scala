package org.rules.lift.utils

import net.liftweb.common.{Box, Failure, Full}
import net.liftweb.http.S
import net.liftweb.http.SHtml._
import net.liftweb.http.js.JE.JsVar
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.Run

import scala.xml.NodeSeq

/**
 * Created by enrico on 6/26/15.
 */
object LiftUtils {

  def bootboxPrompt(caption: String, func: (String) => JsCmd) =
    Run(s"""bootbox.prompt("$caption", function(result) {
          if(result != null) {
            ${ajaxCall(JsVar("result"), func)};
          }
        });
        """)

  def bootboxConfirm(caption: String, func: () => JsCmd) =
    Run(s"""bootbox.confirm("${caption}", function(result) {
          if(result) {
            ${ajaxInvoke(func)};
          }
        });
        """)

  /**
   * if the box is Full, returns the evaluation of func, passing in the boxed value, otherwise notifies an S.error
   * with the given message and returns the default
   * @param box
   * @param func
   * @param message
   * @param default
   * @tparam T type of the Boxed value
   * @tparam R type of the return value
   * @return
   */
  def getOrElseError[T,R](box: Box[T], func: (T) => R, message: String, default: R) =
    box match {
      case Full(value) => func(value)
      case Failure(msg, _, _) => S.error(message + ": " + msg); default
      case _ => S.error(message); default
    }

  /**
   * like SHtml.memoize, but used in conjunction with MemoizeTransformWithArg lets you use a function with an argument
   * and to call applyAgain(arg) with a different value
   * @param f the function to be called
   * @param value the arg value passed in the first invocation, usually in the render function
   * @tparam T the type of the arg
   * @return
   */
  def memoizeWithArg[T](f: (T) => (NodeSeq => NodeSeq), value: T) = {
    new MemoizeTransformWithArg[T] {
      private var lastNodeSeq: NodeSeq = NodeSeq.Empty

      def apply(ns: NodeSeq) : NodeSeq = {
        lastNodeSeq = ns
        f(value)(ns)
      }

      def applyAgain(value: T): NodeSeq = f(value)(lastNodeSeq)
    }
  }
}

trait MemoizeTransformWithArg[T] extends Function1[NodeSeq, NodeSeq] {
  def applyAgain(arg: T): NodeSeq
}
