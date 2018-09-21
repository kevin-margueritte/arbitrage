import $file.librairies
import $file.change, change._
import $file.currency, currency._

import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import io.circe.{Json, JsonObject}
import org.http4s.Uri
import org.http4s.circe.jsonOf
import org.http4s.client.blaze.Http1Client

object ApiRates {

  import cats.implicits._

  // Get actual Rates
  def getActualRates(baseCurrency: BaseCurrency): EitherT[IO, String, List[Change]] = {
    val rates = for {
      httpClient <- Http1Client[IO]()
      actualRatesJson <- httpClient.expect(Uri.uri("https://fx.priceonomics.com/v1/rates/"))(jsonOf[IO, JsonObject])
      actualRates <- IO.pure(decodeJson(actualRatesJson, baseCurrency))
    } yield actualRates

    EitherT(rates)
  }

  // Decode Json from API to list Changes
  private def decodeJson(json: JsonObject, baseCurrency: BaseCurrency): Either[String, List[Change]] = {
    case class FieldNameAndRate(fieldName: String, baseCurrency: BaseCurrency)
    case class FromTo(from: Currency, to: Currency)

    val convertFieldsToCurrencies = Kleisli[Either[String, ?], FieldNameAndRate, FromTo] { case FieldNameAndRate(field, baseCurrency) =>
      field.split("_") match {
        case Array(from, to) if from == baseCurrency.currencyName =>
          Right(FromTo(BaseCurrency(from), OtherCurrency(to)))
        case Array(from, to) if to == baseCurrency.currencyName =>
          Right(FromTo(OtherCurrency(from), BaseCurrency(to)))
        case Array(from, to) =>
          Right(FromTo(OtherCurrency(from), OtherCurrency(to)))
        case _ => Left(s"Field ${field} not parssable")
      }
    }

    val convertJsonToRate = Kleisli[Either[String, ?], Json, BigDecimal] { field =>
      field.hcursor.as[BigDecimal].leftMap(_ => s"Rate : ${field}, is not a number")
    }

    json.toList.traverse[Either[String, ?], Change] { case (fromTo, rate) =>
      for {
        currencies <- convertFieldsToCurrencies.run(FieldNameAndRate(fromTo, baseCurrency))
        rateBg <- convertJsonToRate.run(rate)
      } yield Change(currencies.from, currencies.to, rateBg)
    }
  }

}

