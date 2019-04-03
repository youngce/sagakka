package akka.persistence.saga

import java.util
import java.util.UUID

import akka.actor.{ActorContext, ActorLogging, ActorPath, ActorRef, ActorSelection}
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}

import scala.collection.immutable.HashMap

object Saga {

  type ReceiveSucceeded = PartialFunction[Succeeded, ActorSelection => Unit]
  type ReceiveFailed = PartialFunction[Failed, ActorSelection => Unit]
  type DeliveryId = Long
  type ReplyTo = ActorPath

  implicit def actorPath2Selection(path: ActorPath)(implicit context: ActorContext) = context.actorSelection(path)

  implicit def actorRef2Path(ref: ActorRef)(implicit context: ActorContext) = ref.path

  sealed trait Message {
    val id: UUID
    //    val deliveryId: Long
  }

  trait Command extends Message {
    val replyTo: ActorPath
  }

  trait Event extends Message

  trait Started extends Event {

  }

  trait Succeeded extends Event

  trait Failed extends Event


}

trait Saga extends PersistentActor with AtLeastOnceDelivery with ActorLogging {

  import Saga._


  var undoneIds: HashMap[UUID, (DeliveryId, ReplyTo)] = HashMap.empty

  def startTransaction(to: ActorSelection, replyTo: ActorPath)(cmd: Command) = {
    deliver(to)(deliveryId => {
      undoneIds += cmd.id -> (deliveryId, replyTo)
      cmd
    })
  }

  def commit: ReceiveSucceeded

  def rollback: ReceiveFailed
  def continue: ReceiveFailed

  override def receive: Receive = super.receive.orElse(commitOrRollback)

  private def commitOrRollback: Receive = {
    case evt: Event if undoneIds.get(evt.id).isEmpty =>
      log.warning(s"Be not found the transaction id: ${evt.id}, you need to start a transaction first.")
    case s: Succeeded =>
      commit.apply(s)(getReplyToOpt(s.id).get)
      endTransaction(s.id)
    case f: Failed =>
      rollback.apply(f)(getReplyToOpt(f.id).get)
      endTransaction(f.id)
  }

  def endTransaction(id: UUID) = {
    getDeliveryIdOpt(id)
      .map(deliveryId => confirmDelivery(deliveryId))
    undoneIds -= id
  }

  private def getDeliveryIdOpt(id: UUID) = undoneIds.get(id).map(_._1)

  private def getReplyToOpt(id: UUID) = undoneIds.get(id).map(_._2)

}
