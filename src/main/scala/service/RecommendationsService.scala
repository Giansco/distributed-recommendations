package service

import io.grpc.{ManagedChannel, ManagedChannelBuilder, Status, StatusRuntimeException}
import proto.mail.{MailReply, MailRequest, MailServiceGrpc}
import proto.product.{GetProductsByCategoryReply, GetProductsByCategoryRequest, ProductReply, ProductServiceGrpc}
import proto.recommendations._
import proto.user.UserServiceGrpc
import proto.wishlist._
import server.StubManager

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Random, Success, Try}

class RecommendationsService(stubManager: StubManager)(implicit ec: ExecutionContext) extends RecommendationsServiceGrpc.RecommendationsService {

  // Tengo que guardar los usuarios ya procesados, donde los guardo?
  /**
    * The master chooses a user to analyze every x amount of time (frequency set in registry)
    */
  override def chooseUserToAnalyze(request: ChooseUserRequest): Future[ChooseUserReply] ={

    println("I'm being called")

    // Here we get recent users from wishlist
    val result: Future[GetRecentUsersResponse] = stubManager.wishListStub.getRecentUsers(GetRecentUsersRequest())

    val result2: Future[Seq[Long]] = result.map( r => r.userIds )

    val future: Try[Seq[Long]] = Await.ready(result2, Duration.apply(5, "second")).value.get

    future  match {
      case Success(value) => tellStubToAnalyzeUser(Random.shuffle(value).head)
      case Failure(exception: StatusRuntimeException) =>
        if(exception.getStatus.getCode == Status.Code.UNAVAILABLE) {
          println("Get another stub")
          chooseUserToAnalyze(request)
        } else throw exception
    }
  }

  private def tellStubToAnalyzeUser(userId: Long): Future[ChooseUserReply] ={
    // Here we should call any recommendation service.
    val result: Future[AnalyzeUserResponse] = stubManager.recommendationsStub.analyzeUser(AnalyzeUserRequest(userId))

    val result2: Future[Seq[ProductReply]] = result.map( r => r.products )

    val future: Try[Seq[ProductReply]] = Await.ready(result2, Duration.apply(5, "second")).value.get

    future  match {
      case Success(value) => sendMail(userId, value)
      case Failure(exception: StatusRuntimeException) =>
        if(exception.getStatus.getCode == Status.Code.UNAVAILABLE) {
          println("Get another stub")
          tellStubToAnalyzeUser(userId)
        } else throw exception
    }
  }

  private def sendMail(userId: Long, products: Seq[ProductReply]): Future[ChooseUserReply] = {

    val result: Future[MailReply] = stubManager.mailStub.sendMail(MailRequest(userId, products))

    val result2: Future[String] = result.map( r => r.response )

    val future: Try[String] = Await.ready(result2, Duration.apply(5, "second")).value.get

    future  match {
      case Success(_) => Future.successful(ChooseUserReply())
      case Failure(exception: StatusRuntimeException) =>
        if(exception.getStatus.getCode == Status.Code.UNAVAILABLE) {
          println("Get another stub")
          sendMail(userId, products)
        } else throw exception
    }
  }

  override def analyzeUser(request: AnalyzeUserRequest): Future[AnalyzeUserResponse] = {

    val result: Future[GetProductsResponse] = stubManager.wishListStub.getProducts(GetProductsRequest(request.userId))

    val result2: Future[Seq[ProductReply]] = result.map(r => r.products)

    /*
      Se tuve que hacer blocking porque el time to live es igual a 2 segundos lo que nos da la posibilidad
      que el etcd nos haya dado una address ya caida. Hay que buscar otra solucion.
    * */
    val future: Try[Seq[ProductReply]] = Await.ready(result2, Duration.apply(5, "second")).value.get

    future  match {
      case Success(value) => findRecommendations(value)
      case Failure(exception: StatusRuntimeException) =>
        if(exception.getStatus.getCode == Status.Code.UNAVAILABLE) {
          println("Get another stub")
          analyzeUser(request)
        } else throw exception
    }
  }

  /**
    * Asks product service for all the products with the most popular category in products param
    * TODO que no recomiende los que ya tiene en la wishlist
   */
  private def findRecommendations(products: Seq[ProductReply]): Future[AnalyzeUserResponse] = {
    val mostPopularCategory: String =
      products
        .groupBy(p => p.category)
        .toList
        .maxBy(t => t._2.length)
        ._1


    val result: Future[GetProductsByCategoryReply] = stubManager.productStub.getProductsByCategory(GetProductsByCategoryRequest(mostPopularCategory))

    val result2: Future[Seq[ProductReply]] = result.map(r => r.products)

    /*
      Se tuve que hacer blocking porque el time to live es igual a 2 segundos lo que nos da la posibilidad
      que el etcd nos haya dado una address ya caida. Hay que buscar otra solucion.
    * */
    val future: Try[Seq[ProductReply]] = Await.ready(result2, Duration.apply(5, "second")).value.get

    future  match {
      case Success(value) => Future.successful(AnalyzeUserResponse(value))
      case Failure(exception: StatusRuntimeException) =>
        if(exception.getStatus.getCode == Status.Code.UNAVAILABLE) {
          println("Get another stub")
          findRecommendations(products)
        } else throw exception
    }

  }

  //implementar isActive (solo recibe el request y devuelve un reply con un string)
  @Deprecated
  override def isActive(request: PingRequest): Future[PingReply] = {
    Future.successful(PingReply("active"))
  }

 /* private def getRecommendationsStub: Future[RecommendationsServiceGrpc.RecommendationsService] = {
    serviceManager.getAddress("recommendations").map{
        case Some(value) =>
          println(value.port)
          val channel: ManagedChannel = ManagedChannelBuilder.forAddress(value.address, value.port)
            .usePlaintext(true)
            .build()
          RecommendationsServiceGrpc.stub(channel)
        case None => throw new RuntimeException("No recommendation services running")
    }
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

  private def getWishListStub: Future[WishListServiceGrpc.WishListService] = {
    serviceManager.getAddress("wishlist").map{
      case Some(value) =>
        println(value.port)
        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(value.address, value.port)
          .usePlaintext(true)
          .build()
        WishListServiceGrpc.stub(channel)
      case None => throw new RuntimeException("No wishlist services running")
    }
  }*/

  /*private def getUserStub: Future[UserServiceGrpc.UserService] = {
    // Check how user services are registered in etcd
    serviceManager.getAddress("user").map{
      case Some(value) =>
        println(value.port)
        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(value.address, value.port)
          .usePlaintext(true)
          .build()
        UserServiceGrpc.stub(channel)
      case None => throw new RuntimeException("No user services running")
    }
  }*/

  /*private def getMailStub: Future[MailServiceGrpc.MailService] = {
    // Check how user services are registered in etcd
    serviceManager.getAddress("mail").map{
      case Some(value) =>
        println(value.port)
        val channel: ManagedChannel = ManagedChannelBuilder.forAddress(value.address, value.port)
          .usePlaintext(true)
          .build()
        MailServiceGrpc.stub(channel)
      case None => throw new RuntimeException("No user services running")
    }
  }*/

}

case object UserNotFoundException extends RuntimeException
