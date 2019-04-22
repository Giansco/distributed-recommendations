/*
package server

import io.grpc.{ManagedChannelBuilder, ServerBuilder}
import proto.user.{AddProductRequest, AddUserRequest, GetProductsRequest, UserServiceGrpc}
import repositories.{UserRepository, WishListRepository}
import service.UserService
import slick.basic.DatabaseConfig
import slick.jdbc.H2Profile

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}

object WishListServer extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val stubManager = new ServiceManager
  stubManager.startConnection("0.0.0.0", 50001, "wishlist")

  val config = DatabaseConfig.forConfig[H2Profile]("db")
  val wishListRepo = new WishListRepository(config)
  val userRepo = new UserRepository(config)

  val server = ServerBuilder.forPort(50001)
    .addService(UserServiceGrpc.bindService(new UserService(wishListRepo, userRepo, stubManager), ExecutionContext.global))
    .build()

  server.start()
  println("Running...")

  server.awaitTermination()
}

object ClientDemo extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val channel = ManagedChannelBuilder.forAddress("localhost", 50001)
    .usePlaintext(true)
    .build()

  val stub = UserServiceGrpc.stub(channel)

  val user = stub.addUser(AddUserRequest("eduardo", "scolaro"))
  user.onComplete { r =>
    stub.addProduct(AddProductRequest(1, r.get.userId)).onComplete(r2 => {
      println(r2.get.productId)
      stub.getProducts(GetProductsRequest(r.get.userId)).onComplete(r4 => {
        println(r4)
        println("completed")
      })
    })
  }
  val stubManager = new ServiceManager()

  stubManager.getAddress("wishlist").onComplete{
    case Success(value) => println(value.get.port)
    case Failure(exception) => println(exception)
  }
//  stubManager.getAddress("wishlist").onComplete{
//    case Success(value) => println(value.get.address)
//    case Failure(exception) => println(exception)
//  }
//  stubManager.getAddress("wishlist").onComplete{
//    case Success(value) => println(value.get.address)
//    case Failure(exception) => println(exception)
//  }

  System.in.read()
}
*/
