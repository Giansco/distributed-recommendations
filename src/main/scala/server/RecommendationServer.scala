package server

import io.grpc.ServerBuilder
import proto.recommendations.RecommendationsServiceGrpc
import service.RecommendationsService
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}


object RecommendationServer extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val stubManager = new ServiceManager
  stubManager.startConnection("0.0.0.0", 50002, "recommendations")

  val server = ServerBuilder.forPort(50002)
  .addService(RecommendationsServiceGrpc.bindService(new RecommendationsService(stubManager), ExecutionContext.global))
  .build()

  server.start()
  println("Running...")

  server.awaitTermination()
}
