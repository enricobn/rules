package org.rules.rule

import org.rules.UI

object MessageType extends Enumeration {
  val Info, Warning, Error = Value
} 

trait Message {
  def messageType : MessageType.Value
  def message : String
  def exception : Option[Exception] = None
}

case class Info(message : String) extends Message {
  def messageType : MessageType.Value = MessageType.Info
}

case class Warning(message : String) extends Message {
  def messageType : MessageType.Value = MessageType.Warning
}

case class Error(message: String, override val exception: Option[Exception] = None) extends Message {
  def messageType : MessageType.Value = MessageType.Error
}

object Logged {
    
  implicit def stringToLoggedError[T](errorMessage: String) : Logged[T] = {
    Logged(None, List(Error(errorMessage)))
  }

  implicit def messagesToLogged[T](messages: List[Message]) : Logged[T] = {
    Logged(None, messages)
  }

  implicit def valueToLogged[T](value: T) : Logged[T] = {
    Logged(Some(value))
  }
  
}

case class Logged[T](value: Option[T], messages: List[Message] = List.empty) {

  /**
   * if this is not valid then this otherwise a Logged with no messages, otherwise applies the function to values
   * TODO warnings
   * @param v
   * @param func
   * @return
   */
  def fold(v: T) (func: (T, T) => T) : Logged[T] = {
    value match {
      case Some(good) => func(v, good)
      case _ => this
    }
  }

  /**
   * if logged and this are valid applies function to values, if both are invalid a sum of messages, otherwise
   * an invalid value with messages
   * TODO warnings
   * @param logged
   * @param func
   * @return
   */
  def fold(logged: Logged[T]) (func: (T, T) => T) : Logged[T] = {
    value match {
      case Some(v) =>
        logged.value match {
          case Some(l) => func(v,l)
          case _ => Logged(None, logged.messages)
        }
      case _ =>
        logged.value match {
          case Some(l) => Logged(None, messages)
          case _ => Logged(None, messages ++ logged.messages)
        }
    }
  }

}

class RuleSolver[TOKEN] (factories: Set[RuleFactory[TOKEN]], r: Set[Rule[TOKEN]], ui: UI) {
  val choiceCache = new ChoiceCache
  val runCache = new RunCache
  val rules = r ++ factories.foldLeft(Set.empty[Rule[TOKEN]]){ (actual, factory) => actual ++ factory.create(ui) }
  
  def chooseRuleTree(trees: Set[RuleTree], message: String) : Option[RuleTree] = {
    if (trees.size == 1) {
      Some(trees.head)
    } else {
      ui.choose("Disambiguation", message, trees.toList)
    }
  }

  class ChoiceCache {
    val choices : scala.collection.mutable.Map[(Set[Rule[TOKEN]], Tags),Rule[TOKEN]] = scala.collection.mutable.Map.empty
    
    def get(choice: Set[RuleTree], tags: Tags) : Option[Rule[TOKEN]] = {
      val key = Tuple2(choice.map(_.rule),tags)
      if (choices.contains(key)) {
        Some(choices(key))
      } else {
        None
      }
    }
    
    def addChoice(choice: Set[RuleTree], tags: Tags, choosen: RuleTree) = {
      val key = Tuple2(choice.map(_.rule),tags)
      choices += Tuple2(key, choosen.rule)
    }
    
    def clear() = {
      choices.clear()
    }
  }

  case class RunContext(values: Map[TOKEN,AnyRef], tags: Tags) {
    def ++(runContext: RunContext) = RunContext(values ++ runContext.values, tags ++ runContext.tags)
  }

  class RunCache {
    val runs : scala.collection.mutable.Map[(Rule[TOKEN], Map[Requirement[TOKEN], AnyRef]),Map[TOKEN,AnyRef]] = scala.collection.mutable.Map.empty
    
    def get(rule: Rule[TOKEN], values: Map[Requirement[TOKEN],AnyRef]) : Option[Map[TOKEN,AnyRef]] = {
      val key = Tuple2(rule,values)
      if (runs.contains(key)) {
        Some(runs(key))
      } else {
        None
      }
    }
    
    def addRun(rule: Rule[TOKEN], values: Map[Requirement[TOKEN],AnyRef], choosen: Map[TOKEN,AnyRef]) = {
      val key = Tuple2(rule,values)
      runs += Tuple2(key, choosen)
    }
    
    def clear() = {
      runs.clear()
    }
  }

  case class RuleTree(rule : Rule[TOKEN], parent: Option[RuleTree], parentRequires: Option[Requirement[TOKEN]],
                      requiredTags: Tags) {
    val pos = parents.indexWhere{ tree => tree.rule == this.rule && tree.requiredTags == this.requiredTags }

    if (pos >= 0) {
      throw new CycleException("Cycle detected: " + (parents.drop(pos) :+ rule).mkString(", "))
    }

    lazy val children: Map[Requirement[TOKEN], Set[RuleTree]] = {
      /*
        for each requirement I look for the providers, but I retain only the Rule's that are compatible (matches)
        with requiredTags
       */
      val m = rule.requires.map { requirement =>
        val filtered = providers(requirement).filter( _.providesTags.matches(requiredTags) )
        (requirement, filtered)
      }
      val m1 = m.map { case (k, v) =>
        val allRequiredTags = requiredTags ++ k.tags
        // I remove acquired tags
        (k, v.map{ rule => RuleTree(rule, Some(this), Some(k), allRequiredTags -- rule.providesTags) })
      }
      m1.toMap
    }

    def isValid : Boolean = {
      if (rule.requires.isEmpty) {
        return true
      }
      children.values.forall{ trees => trees.exists{ tree => tree.isValid } }
    }

    lazy val parents : List[RuleTree] = {
      parent match {
        case Some(p) => p.parents :+ p
        case None => List.empty
      }
    }

    // TODO handle warnings?
    def run(context: Logged[RunContext]) : Logged[RunContext] = {
      val choosedRequirements = children.map{ case (k,v) =>
        // TODO check if requiredTags are not compatible with requirement  
        val allRequiredTags = requiredTags ++ k.tags
        
        val filtered = v.filter{ tree => tree.isValid }

        if (filtered.isEmpty) {
          return rule + " no valid rule for " + k
        }

        val cached = choiceCache.get(filtered, allRequiredTags)
        val choice = cached match {
          case Some(r) =>
             val tree = filtered.find { x => x.rule == r }
             tree match {
              case Some(t) => t
               case None => return "error getting cache for " + tree
             }
          case None =>
            val tree = chooseRuleTree(filtered.toSet, toString() + "." + k + " " + allRequiredTags)
            tree match {
              case Some(g) => 
                choiceCache.addChoice(filtered, allRequiredTags, g)
                g
              case None => return "aborted choose for " + k
            }
        }

        val valid = choice.run(context)
        valid.value match {
          case Some(ctx) =>
            Some(Tuple2(k, ctx))
          case None => return Logged(None, valid.messages :+ Error("cannot find requirements for " + choice
              + " " + requiredTags)) 
        }
      }
      
      // I filter out invalid (None) pairs
      val requirements = choosedRequirements.collect{ case Some(kv) => kv }

            // I collect all tags
      val requirementsTags = requirements.foldLeft(Tags.empty){
        (actual,a) => actual ++ a._2.tags }

      if (!requirementsTags.matches(requiredTags)) {
        return "not all required tags are met"
      }
      
      // I haven't met all requirements
      // I think it cannot happens, since if there's not a valid requirement the above map will return with
      // an error
      if (requirements.size != children.size) {
        "Haven't met all requirements for " + this + " missed " + (children.keys.toSet -- requirements.toMap.keys.toSet)
      } else {
        // I create a map with requirements and values for run
        val requirementsValues = requirements.map{ a => (a._1, a._2.values(a._1.token)) }.toMap
        val cached = if (rule.cache) runCache.get(rule, requirementsValues) else None
        val v = cached match {
          case Some(r) =>
            r
          case None =>
            val values = rule.run(ui, requirementsValues)
            if (rule.cache) {
              runCache.addRun(rule, requirementsValues, values)
            }
            values
        }
        // I collect all tags
        val requirementsTags = requirements.foldLeft(Tags.empty){
          (actual,a) => actual ++ a._2.tags
        }
        RunContext(v, requirementsTags)
      }
    }
    
  /**
   * @return a set of the rules that provide that requirement.
   */
    def providers(requirement: Requirement[TOKEN]) : Set[Rule[TOKEN]] =
      rules.filter ( _.provides.contains(requirement.token) && requirement.tags.matches(requiredTags) )

    override def toString : String = {
      rule.toString()
    }
    
    override def hashCode : Int = {
      rule.hashCode()
    }
    
    def pretty : String = {
      toString(0)
    }
    
    private def toString(ind: Int) : String = {
      val indent = Range(0, ind * 2).foldLeft(""){ (a,i) => a + " "}
      indent + rule + "\n" +
        children.foldLeft(""){ (actual,trees) => actual + indent + "  " + trees._1 + "\n" +
          trees._2.foldLeft(""){ (actual,tree) => actual + tree.toString(ind + 2)}
        }
    }
    
  }
  
  def run() : List[Message] = {
    choiceCache.clear()
    runCache.clear()
    
    tree.value match {
      case Some(g) =>
        val result = g.run(RunContext(Map.empty, Tags.empty))
        result.value match {
          case Some(ctx) => List.empty
          case None => result.messages 
        }
      case None =>
        tree.messages.+:(Error("Cannot find a valid tree"))
    }
  }
  
  lazy val tree : Logged[RuleTree] = {
    def goals = rules.filter { rule => rule.provides.isEmpty }

    if (goals.isEmpty) {
      "Cannot find a goal"
    } else if (goals.size != 1) {
      "Found more than one goal"
    } else {
      val t = RuleTree(goals.head, None, None, Tags.empty)
      try {
        // don't remove this check since, as a side effect, it forces children to be evaluated to detect cycles
        if (t.children.isEmpty) {
          "Empty tree"
        } else {
          t
        }
      } catch {
        case e: CycleException => e.getMessage
      }
    }

  }
}
