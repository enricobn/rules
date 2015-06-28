package org.rules.lift

import org.rules.lift.model.RulesDAO

/**
 * Created by enrico on 6/27/15.
 */
trait RulesDAOProvider {
  protected lazy val rulesDAO = RulesInjector.inject[RulesDAO].openOrThrowException(
    "Cannot find an implementation of RulesDAO")
}
