/*
package service

import io.grpc.{ManagedChannel, ManagedChannelBuilder, Status, StatusRuntimeException}
import product.product.{ProductReply, ProductRequest, ProductServiceGrpc}
import product.user.{AddProductRequest, AddProductResponse, AddUserRequest, AddUserResponse, DeleteProductRequest, DeleteProductResponse, GetProductsRequest, GetProductsResponse, PingReply, PingRequest, UserServiceGrpc}
import repositories.{UserRepository, WishListRepository}
import server.ServiceManager

import scala.concurrent.duration.Duration

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class UserService(wishListRepo: WishListRepository, userRepo: UserRepository, serviceManager: ServiceManager)(implicit ec: ExecutionContext) extends UserServiceGrpc.UserService  {

  override def addProduct(in: AddProductRequest): Future[AddProductResponse] = {
    wishListRepo.addProduct(in.userId, in.productId) map {
      w => AddProductResponse(w.productId)
    }
    //how can I manage a failure? use option?
  }

  override def getProducts(in: GetProductsRequest): Future[GetProductsResponse] = {
    getProductStub.flatMap(stub => {
      val result = wishListRepo.getProducts(in.userId).map(ids => ids.map(id => stub.getProduct(ProductRequest(id))))

      val results2: Future[Seq[ProductReply]] = result.flatMap(r => Future.sequence(r))

      /*
        Se tuve que hacer blocking porque el time to live es igual a 2 segundos lo que nos da la posibilidad
        que el etcd nos haya dado una address ya caida. Hay que buscar otra solucion.
      * */
      val future = Await.ready(results2, Duration.apply(5, "second")).value.get

      future  match {
        case Success(value) => Future.successful(GetProductsResponse(value))
        case Failure(exception: StatusRuntimeException) =>
          if(exception.getStatus.getCode == Status.Code.UNAVAILABLE) {
            println("Get another stub")
            getProducts(in)
          } else throw exception
      }
    })
  }

  override def deleteProduct(in: DeleteProductRequest): Future[DeleteProductResponse] = {
    wishListRepo.deleteProduct(in.userId, in.productId).map{
      id => DeleteProductResponse(id)
    }
    //how can I manage a failure? use option?
  }

  override def addUser(request: AddUserRequest): Future[AddUserResponse] = {
    userRepo.create(request.firstName, request.lastName).map(u => AddUserResponse(u.id))
  }

  //implementar isActive (solo recibe el request y devuelve un reply con un string)
  @Deprecated
  override def isActive(request: PingRequest): Future[PingReply] = {
    Future.successful(PingReply("active"))
  }

  private def getProductStub: Future[ProductServiceGrpc.ProductServiceStub] = {
    serviceManager.getAddress("product").map{
        case Some(value) =>
          println(value.port)
          val channel: ManagedChannel = ManagedChannelBuilder.forAddress(value.address, value.port)
            .usePlaintext(true)
            .build()
          ProductServiceGrpc.stub(channel)
        case None => throw new RuntimeException("No product services running")
      }
  }
}

case object UserNotFoundException extends RuntimeException
*/
