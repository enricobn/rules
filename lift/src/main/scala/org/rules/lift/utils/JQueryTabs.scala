package org.rules.lift.utils

import java.util.UUID

import net.liftweb.http.js.JsCmds.{SetHtml, Run}

import scala.xml.NodeSeq

/**
 * Created by enrico on 7/19/15.
 */
trait JQueryTabs {
  def addTab(tabContainerId: String, name: String, content: NodeSeq) = {
    val newTabId = UUID.randomUUID().toString
    Run(
      s"""
        console.log("addTab($tabContainerId,$newTabId)");
        /* I must use > ul since I want direct children, not all (nested tabs)*/
        $$( "div#$tabContainerId > ul" ).append(
          "<li><a href='#$newTabId'>$name</a><span class='ui-icon ui-icon-close' role='presentation'>Remove Tab</span></li>"
        );
        $$( "div#$tabContainerId" ).append(
          "<div id='$newTabId' style='height: 100%;'></div>"
        );
      """.stripMargin) &
      SetHtml(newTabId, content) &
      Run(
        s"""
          /* I must use > ul since I want direct children, not all (nested tabs)*/
          var last = $$('div#$tabContainerId > ul li:last').index()
          console.log("last " + last);
          $$( "div#$tabContainerId" ).tabs( "refresh" );
          $$( "div#$tabContainerId" ).tabs({active: last});
          $$( "div#$tabContainerId" ).show();
        """.stripMargin)
  }

  def createTabs(where: String, tabContainerId: String) =
    SetHtml(where,
      <div id={tabContainerId} style="height: 100%">
        <ul></ul>
      </div>) &
      Run(
        s"""
          var tabs = $$( "#$tabContainerId" ).tabs();
          // close icon: removing the tab on click
          tabs.delegate( "span.ui-icon-close", "click", function() {
            var panelId = $$( this ).closest( "li" ).remove().attr( "aria-controls" );
            $$( "#" + panelId ).remove();
            tabs.tabs( "refresh" );
            var tabCount = $$('#$tabContainerId >ul >li').size();
            if (tabCount == 0) {
              $$( "#$tabContainerId" ).hide();
            }
          });
          $$( "#$tabContainerId" ).hide();
        """.stripMargin
      )
}
