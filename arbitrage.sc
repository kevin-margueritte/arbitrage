import $file.librairies
import $file.change, change._
import $file.currency, currency._

import java.math.{MathContext, RoundingMode}
import scala.annotation.tailrec


/**
  * Declarations :
  * N = number of currencies
  * M = number of changes
  *
  * Method complexities :
  *   - next : 1 < 3xO(M) <=> O(M)
  *   - getAdjacents : 1 < O(M) <=> O(M)
  *   - compute : O(M n log n) + O(M) + O (M x N) <=> O(M x N)
  *
  * The global complexity is O(M x N)
  */
object ArbitrageAlgorithm {

  @tailrec def compute(stateList: Seq[State], baseCurrency: BaseCurrency): String = {
    val finalState = stateList.forall {
      case Waiting(_, _) => true
      case _ => false
    }

    if (finalState) {
      val stateRes = stateList.sortWith((stateA, stateB) => stateA.rate > stateB.rate).head // O(n log n)
      val history = stateRes.history.map { // O(n)
        case Explored(from, _) => from.currencyName
        case _ => baseCurrency.currencyName
      }.mkString("->")
      val rate = stateRes.rate.round(new MathContext(3, RoundingMode.DOWN))
      s"Arbitrage opportunity $history (${rate} %)"
    } else
      compute(stateList.flatMap(state => getAdjacents(state).map(event => next(state, event))), baseCurrency)
  }

  private def getAdjacents(state: State): Seq[Event] = {
    state match {
      case Waiting(_, _)                        => Stopped() :: Nil // Final state => Stop
      case Computing(_, _, Nil, _)              => Stopped() :: Nil // The graph is empty => Stop
      case Computing(_, _ :BaseCurrency, _, _)  => Stopped() :: Nil // The state is computing and the current is the base currency => Stop
      case run: Running                         =>                  // The state is Computing or Starting, it's possible to get adjacent currencies
        run.changes.filter(_.from == run.currentCurrency) match {   // Check if the current currency has an adjacent currency
          case Nil => Stopped() :: Nil                                                        // No adjacent => Stop
          case adjacent => adjacent.map(change => Explored(run.currentCurrency, change.to))   // Compute later adjacent currencies
        }
    }
  }

  private def next(state: State, event: Event): State = {
    (state, event) match {
      case (waiting: Waiting, _)      => waiting // Final State
      case (r: Running, Stopped())    => Waiting(r.rate, r.history :+ event)  // No adjacent => Final state
      case (r: Starting, Explored(from, to)) => // It's the starting state, the currency is the base currency => Compute adjacent currency
        val currentRate = r.changes.filter(change => change.from == from && change.to == to).head.rate
        Computing(r.rate * currentRate, to, r.changes, r.history :+ event)
      case (r: Computing, Explored(from, to)) =>  // Arbitrage are computing => Compute the adjacent currency and remove the conversion from/to for the current currency
        val removeFrom = r.changes.filter(change => change.from == from && change.to == to).head
        val removeTo = r.changes.filter(change => change.from == to && change.to == from).head

        Computing(r.rate * removeFrom.rate, to,
          r.changes.filter(change => (change != removeFrom) && (change != removeTo)), r.history :+ event)
    }
  }

}

sealed trait State {
  val rate: BigDecimal
  val history: Seq[Event]
}

sealed trait Running {
  self: State =>

  val currentCurrency: Currency
  val changes: Seq[Change]
}

case class Starting(rate: BigDecimal, currentCurrency: BaseCurrency, changes: Seq[Change], history: Seq[Event]) extends State with Running
case class Computing(rate: BigDecimal, currentCurrency: Currency, changes: Seq[Change], history: Seq[Event]) extends State with Running
case class Waiting(rate: BigDecimal, history: Seq[Event]) extends State

sealed trait Event

case class Explored(from: Currency, to: Currency) extends Event
case class Stopped() extends Event

