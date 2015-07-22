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
        var tabs =  $$( "#$tabContainerId" ).tabs();
        var ul = tabs.find( "ul" );
        $$("<li><a href='#$newTabId'>$name</a><span class='ui-icon ui-icon-close' role='presentation'>Remove Tab</span></li>").appendTo(ul);
        $$( "<div id='$newTabId' style='height: 100%;'></div>" ).appendTo( tabs );
      """.stripMargin) &
      SetHtml(newTabId, content) &
      Run(
        s"""
          tabs.tabs( 'refresh' );
          var last = $$('#$tabContainerId .ui-tabs-nav li:last').index();
          tabs.tabs({active: last});
          tabs.show();
        """.stripMargin)
  }

  def createTabs(where: String, tabContainerId: String) =
    SetHtml(where,
      <div id={tabContainerId} style="height: 100%">
        <ul></ul>
      </div>) &
      Run(
        s"""
        // close icon: removing the tab on click
        var tabs = $$( "#$tabContainerId" ).tabs();
        tabs.delegate( "span.ui-icon-close", "click", function() {
          var panelId = $$( this ).closest( "li" ).remove().attr( "aria-controls" );
          $$( "#" + panelId ).remove();
          tabs.tabs( "refresh" );
          var tabCount = $$('#$tabContainerId >ul >li').size();
          if (tabCount == 0) {
            tabs.hide();
          }
        });
        tabs.hide();
        """
      )
}
