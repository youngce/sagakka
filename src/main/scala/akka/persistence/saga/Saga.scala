package akka.persistence.saga

import java.util
import java.util.UUID

import akka.actor.{ActorRef, ActorSelection}
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}

object Saga {

  sealed trait Message {
    val id: UUID
    //    val deliveryId: Long
  }

  sealed trait Command extends Message {
    val reply: ActorSelection
  }


  trait Event extends Message

  trait Started extends Event {

  }

  trait Succeeded extends Event

  trait Failed extends Event


}

trait Saga extends PersistentActor with AtLeastOnceDelivery {

  import Saga._

  val startedCommands: java.util.HashMap[UUID, (Long, Command)] = new util.HashMap[UUID, (Long, Command)]()

  def startTransaction(to: ActorSelection)(cmd: Command) = {


    deliver(to)(deliveryId => {
      startedCommands.put(cmd.id, deliveryId -> cmd)
      cmd

    })
  }

  def commit: PartialFunction[Succeeded, Unit]

  def rollback: PartialFunction[Failed, Unit]

  override def receive: Receive = super.receive.orElse(commitOrRollback)

  private def commitOrRollback: Receive = {
    case s: Succeeded =>
      commit.apply(s)
      endTransaction(s)
    case f: Failed =>
      rollback.apply(f)
      endTransaction(f)
  }

  private def endTransaction(evt: Event) = {

    val (deliveryId, _) = startedCommands.remove(evt.id)
    confirmDelivery(deliveryId)
  }

}
