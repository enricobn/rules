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

  def bootboxConfirm(caption: String, cmd: JsCmd) =
    Run(s"""bootbox.confirm("${caption}", function(result) {
          if(result) {
            ${cmd.toJsCmd};
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
   * SO you can call apply(arg) for the first apply, then call applyAgain(arg) with a different value
   * @param f the function to be called
   * @tparam T the type of the arg
   * @return
   */
  def memoizeWithArg[T](f: (T) => (NodeSeq => NodeSeq)) =
    new MemoizeTransformWithArg[T] {
      private var lastNodeSeq: Box[NodeSeq] = None

      def apply(value: T) : NodeSeq => NodeSeq = {
        if (lastNodeSeq.isDefined) {
          throw new RuntimeException("apply must be called only once.")
        }
        (ns: NodeSeq) => {
          lastNodeSeq = Full(ns)
          f(value)(ns)
        }
      }

      def applyAgain(value: T): NodeSeq = f(value)(lastNodeSeq.openOrThrowException("You must call apply(T) first."))

    }

}

trait MemoizeTransformWithArg[T] {
  def applyAgain(arg: T): NodeSeq
  def apply(value: T) : NodeSeq => NodeSeq
}
