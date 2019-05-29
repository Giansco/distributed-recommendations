package server

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import proto.mail.MailServiceGrpc
import proto.product.ProductServiceGrpc
import proto.recommendations.RecommendationsServiceGrpc
import proto.wishlist.WishListServiceGrpc

class StubManager {

  private def productChannel: ManagedChannel = ManagedChannelBuilder.forAddress("product", 50000)
    .usePlaintext(true)
    .build()

  def productStub: ProductServiceGrpc.ProductServiceStub = ProductServiceGrpc.stub(productChannel)

  private def mailChannel: ManagedChannel = ManagedChannelBuilder.forAddress("mail", 50000)
    .usePlaintext(true)
    .build()

  def mailStub: MailServiceGrpc.MailServiceStub = MailServiceGrpc.stub(mailChannel)

  private def wishListChannel: ManagedChannel = ManagedChannelBuilder.forAddress("wishlist", 50000)
    .usePlaintext(true)
    .build()

  def wishListStub: WishListServiceGrpc.WishListServiceStub = WishListServiceGrpc.stub(wishListChannel)

  private def recommendationsChannel: ManagedChannel = ManagedChannelBuilder.forAddress("recommendations", 50000)
    .usePlaintext(true)
    .build()

  def recommendationsStub: RecommendationsServiceGrpc.RecommendationsServiceStub = RecommendationsServiceGrpc.stub(recommendationsChannel)
}
