val http4sVersion = "0.18.18"
val circeVersion  = "0.6.1"

interp.load.ivy("org.http4s" %% "http4s-circe" % http4sVersion)
interp.load.ivy("org.http4s" %% "http4s-blaze-client" % http4sVersion)
interp.load.ivy("io.circe" %% "circe-core" % circeVersion)
interp.load.ivy("io.circe" %% "circe-generic" % circeVersion)
interp.load.ivy("io.circe" %% "circe-parser" % circeVersion)
import $plugin.$ivy.`org.spire-math::kind-projector:0.9.7`
