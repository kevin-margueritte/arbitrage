sealed trait Currency {
  val currencyName: String
}

case class BaseCurrency(currencyName: String) extends Currency

case class OtherCurrency(currencyName: String) extends Currency
