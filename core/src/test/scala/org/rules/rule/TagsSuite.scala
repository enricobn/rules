package org.rules.rule

import org.scalatest._

class TagsSuite extends FunSuite {

  test("matches") {
    val tags : Tags = Map("dbType" -> "main")
    
    assert(tags.matches(Tags.empty))
    
    assert(tags.matches(Map("dbType" -> "main")))
    
    assert(!tags.matches(Map("dbType" -> "repo")))
    
    assert(tags.matches(Map("db" -> "oracle")))
    
    assert(Tags.empty.matches(tags))
    
    assert(Tags.empty.matches(Tags.empty))
  }

  test("remove") {
    val tags : Tags = Map("dbType" -> "main")
    
    assert((tags -- Map("dbType" -> "main")).isEmpty)
    
    assert((tags -- Map("dbType" -> "repo")) == tags)
    
    assert((tags -- Map("db" -> "oracle")) == tags)
    
  }
  
}