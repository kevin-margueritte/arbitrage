import $file.librairies
import $file.change, change._
import $file.currency, currency._
import $file.api, api._
import $file.arbitrage, arbitrage._

import ammonite.ops._

import cats.data.Kleisli
import cats.effect.IO

@main
def main(s: String, path: Path = pwd) = {

  import cats.implicits._

  val baseCurrency = BaseCurrency(s)

  // Check if the base currency is correct
  val checkBaseCurrency = Kleisli[Either[String, ?], (List[Change], BaseCurrency), BaseCurrency] {
    case (listChanges, base) =>

      val listBaseCurrencies = listChanges.filter {
        case Change(BaseCurrency(currency), _, _) if currency == base.currencyName => true
        case _ => false
      }

      listBaseCurrencies match {
        case Nil => Left("No base currency not found")
        case _ => Right(base)
      }
  }

  // Check if a currency A to B exist then B to A exist
  val checkChanges = Kleisli[Either[String, ?], List[Change], List[Change]] { listChanges =>
    listChanges.traverse[Either[String, ?], Change] { case change =>
      val toFromExist = listChanges.exists {
        case Change(from, to, _) if change.to == from && change.from == to => true
        case _ => false
      }

      if (toFromExist) Right(change)
      else Left(s"The change ${change.from}_${change.to} exist but ${change.to}_${change.from} doesn't exist ")
    }
  }

  val removeSameFromToCurrency = Kleisli[Either[String, ?], List[Change], List[Change]] { listChanges =>
    val changesFiltered = listChanges.filter {
      case Change(from, to, _) if from.currencyName == to.currencyName => false
      case _ => true
    }

    Right(changesFiltered)
  }

  val result = ApiRates.getActualRates(baseCurrency).flatMapF { changes =>
    val result = for {
      baseCurrency <- checkBaseCurrency.run((changes, baseCurrency))
      changesFiltered <- checkChanges.compose(removeSameFromToCurrency).run(changes)
    } yield {
      val startState = Starting(1, baseCurrency, changesFiltered, Nil)
      ArbitrageAlgorithm.compute(startState :: Nil, baseCurrency)
    }
    IO.pure(result)
  }.value.unsafeRunSync()

  println(result)

}
