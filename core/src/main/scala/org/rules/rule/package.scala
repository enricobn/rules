package org.rules

package object rule {
  
  /*
   *  I think I cannot put them in a SimpleRequirement companion object (I have tried, but it does not work), 
   *  because it's used when a Requirement is needed, so I think it looks for a Requirement companion object
   */
  
  implicit def simpleRequirementString(str:String): SimpleRequirement = SimpleRequirement(str)

  implicit def simpleRequirementTuple(tuple: (String, String, String)): SimpleRequirement =
    SimpleRequirement(tuple._1, Map(tuple._2 -> tuple._3))
}