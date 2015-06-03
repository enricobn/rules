package org.rules
import scala.swing._
import scala.io.StdIn

trait UI {
  def choose[T](title : String, message: String, list : List[T]) : Option[T] = {
    if (list.isEmpty) {
      None
    } else if (list.tail.isEmpty) {
      Some(list.head)
    } else {
      choose_internal(title, message, list)
    }
  }
  
  protected def choose_internal[T](title : String, message: String, list : List[T]) : Option[T]
}

object SwingUI extends UI {
  
  protected def choose_internal[T](title : String, message: String, list : List[T]) : Option[T] = {
    Dialog.showInput(
        title = title,
        message = message, 
        entries = list, 
        initial = list.head)
  }
  
}

object ConsoleUI extends UI {
  
  protected def choose_internal[T](title : String, message: String, list : List[T]) : Option[T] = {
    println(title)
    println(message + ":")
    
    list.indices.foreach { i => println((i + 1) + ". " + list(i)) }
    
    println("\n0 Exit")
    
    try {
      val choice = StdIn.readInt()
      if (choice == 0) {
        None
      } else {
        Some(list(choice -1))
      }
    } catch {
      case e: Exception => choose(title, message, list)
    }
  }
  
}