package server

import com.google.gson.Gson
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import org.etcd4s.pb.etcdserverpb._
import org.etcd4s.pb.v3electionpb.CampaignRequest
import org.etcd4s.{Etcd4sClient, Etcd4sClientConfig}

import scala.concurrent.Future
import scala.util.Random

class ServiceManager { // Is ServiceManager only used by one service?
  import scala.concurrent.ExecutionContext.Implicits.global

  // Address and port of the etcd server
  val addressClient = "127.0.0.1"
  val addressPort = 2379
  val tts = 1
  private val client = getClient
  private val id: Long = Random.nextLong() // This might need to be moved to another place
  // If the service

  def startConnection(address: String, port: Int, url: String): Future[PutResponse] = {
    val response = client.rpcClient.leaseRpc.leaseGrant(LeaseGrantRequest(tts, id))
    val leaseId = response.map(_.iD)
    val future: Future[PutResponse] = response.flatMap(v => {
      println(v.tTL)
      client.rpcClient.kvRpc
        .put(PutRequest(
          stringToByteString(url + "/" + id),
          stringToByteString(new Gson().toJson(AddressWithPort(address, port))),
          v.iD,
          prevKv = false,
          ignoreValue = false,
          ignoreLease = false))
    })
    val request: StreamObserver[LeaseKeepAliveRequest] = client.rpcClient.leaseRpc.leaseKeepAlive(new KeepAliveObserver)
    keepAlive(id, request)
    campaign(leaseId) // Maybe move somewhere else
    future
  }

  def getAddress(url: String): Future[Option[AddressWithPort]] = {
    val future = client.kvService.getRange(url).map(res => {
      val quantity = res.count
      if(quantity > 0)
        Option(new Gson()
          .fromJson(res.kvs(Random.nextInt(res.count.toInt)).value.toStringUtf8, classOf[AddressWithPort]))
      else None
    })
    future
  }

  private def getClient = {
    val config = Etcd4sClientConfig(
      address = addressClient,
      port = addressPort
    )
    Etcd4sClient.newClient(config)
  }

  private def stringToByteString(string: String): ByteString = {
    import com.google.protobuf.ByteString
    ByteString.copyFrom(string.getBytes())
  }

  private def keepAlive(id: Long, request: StreamObserver[LeaseKeepAliveRequest]): Unit = {
    request.onNext(LeaseKeepAliveRequest(id))
    Future {
      Thread.sleep(tts * 1000)
      keepAlive(id, request)
    }

  }

  /** A node campaigns to become the leader
    *
    * If the request is sent when there is another leader, the node will enter a wait list to become leader
    * If there is not leader, this node will immediately become leader
    *
    * When the node is elected leader, CampaignRequest returns a CampaignResponse
    * After being elected leader, the node will enter the leaderLoop
    *
    * @param leaseId The id of the leader's lease
    *                After being elected leader, the node will lead as long as this lease is kept alive
    *                Once the lease expires, another leader will be elected
    */
  private def campaign(leaseId: Future[Long]): Unit = {
    leaseId.map( lease => {
      client.rpcClient.electionRpc.campaign(CampaignRequest(
        stringToByteString("election"),
        lease,
        stringToByteString(id.toString)
      )).map( response => {
        // The response is sent when this service becomes leader
        // Therefore, the lease id should always be the id of the leader
        // Which means the comparison is probably unnecessary.
        if (response.leader.get.lease == lease) {
          println("I'm leader")
          leaderLoop()
        }
      })
    })
  }

  /** Defines the action that the leader must execute
    *
    * This action can only be executed by the leader
    */
  private def leaderAction(): Unit = {
    println("I'm doing leader stuff")
  }

  /** Creates a loop that will execute the leaderAction after some time
    *
    * The loop can only be entered when the node is the leader
    */
  private def leaderLoop(): Unit = {
    leaderAction()
    Future {
      Thread.sleep(tts * 6000) // in milliseconds
      leaderLoop()
    }
  }
}

case class AddressWithPort(address: String, port: Int)

class KeepAliveObserver extends StreamObserver[LeaseKeepAliveResponse] {
  override def onNext(value: LeaseKeepAliveResponse): Unit = Unit

  override def onError(t: Throwable): Unit = throw t

  override def onCompleted(): Unit = println("Completed")
}
