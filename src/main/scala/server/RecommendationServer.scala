package server

import io.grpc.ServerBuilder
import proto.recommendations.RecommendationsServiceGrpc
import service.RecommendationsService
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}


object RecommendationServer extends App {

  // Receives the time interval for recommendations.
  val recommendationTime: Int = args.headOption.getOrElse("6000").toInt

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val stubManager = new ServiceManager
  stubManager.startConnection("0.0.0.0", 50000, "recommendations", recommendationTime)

  val server = ServerBuilder.forPort(50001)
  .addService(RecommendationsServiceGrpc.bindService(new RecommendationsService(stubManager), ExecutionContext.global))
  .build()

  server.start()
  println("Running...")

  server.awaitTermination()
}
