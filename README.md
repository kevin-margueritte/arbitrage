# Auto-detect arbitrage

The purpose of this project is to detect the best arbitrage opportunities from trades provided by this [api](https://fx.priceonomics.com/v1/rates/) 
# Launch
For launch the project you need [ammonite REPL](http://ammonite.io/#Ammonite) and run this command

    amm execute.sc <CURRENCY>

This command return the best arbitrage opportunities for the `CURRENCY`in parameter. 

Currencies available are : `EUR`, `JPY`, `USD`, `BTC` 

Example : 

    $ amm execute.sc BTC 
    > Right(Arbitrage opportunity BTC->JPY->EUR->BTC (1.10 %))

