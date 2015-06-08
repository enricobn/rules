package org.rules.lift.snippet

import scala.xml.{Text}



/**
 * Created by enrico on 6/1/15.
 */
object Index {

  def header =
    <h2>Rules</h2>

  def nav = Text("")
/*      <div>
        London<br/>
        Paris<br/>
        Tokyo<br/>
      </div>
*/
  def footer =
    <h3>(C) 2015 rules.org</h3>

  def content = Text("")
/*  Range(0,10).foldLeft(NodeSeq.Empty){ (actual,r) =>
    actual ++ <h1>London</h1>
      <p>London is the capital city of England. It is the most populous city in the United Kingdom, with a metropolitan area of over 13 million inhabitants.</p>
      <p>Standing on the River Thames, London has been a major settlement for two millennia, its history going back to its founding by the Romans, who named it Londinium.</p>
      */

}
