package akka.persistence.saga

import java.util.UUID

import akka.actor.{Actor, ActorPath, ActorSelection, ActorSystem, Props}
import akka.persistence.saga.TestSagaActor._
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

object TestSagaActor {

  import Saga._


  case class IsSuccessful(is: Boolean, id: UUID)

  case class SuccessMessage(id: UUID = UUID.randomUUID(), replyTo: ActorPath) extends Command

  case class FailureMessage(id: UUID = UUID.randomUUID(), replyTo: ActorPath) extends Command

  case class BeSucceeded(id: UUID) extends Succeeded

  case class BeFailed(id: UUID) extends Failed

  case object GetState


}

class Destination extends Actor {
  override def receive: Receive = {
    case SuccessMessage(id, replyTo) => context.actorSelection(replyTo) ! BeSucceeded(id)
    case FailureMessage(id, replyTo) => context.actorSelection(replyTo) ! BeFailed(id)
  }
}

class TestSagaActor(dest: ActorPath) extends Saga {

  var ids: List[UUID] = List.empty


  override def commit = {
    case BeSucceeded(id) => replyTo =>
      persist(id) { id =>
        updateState(id)

      }
      replyTo ! id

  }

  def updateState(id: UUID) = {

    ids = ids :+ id
  }

  override def rollback = {
    case BeFailed(id) => replyTo =>
      replyTo ! id

  }

  override def receiveRecover: Receive = {
    case evt: UUID => updateState(evt)
  }

  override def receiveCommand: Receive = {
    case GetState => sender() ! ids
    case IsSuccessful(is, id) =>
      //      persist(id)(updateState)
      //      sender() ! id
      val starting = startTransaction(context.actorSelection(dest),sender().path)(_)
      if (is) {
        starting(SuccessMessage(id, self.path))
      } else {
        starting(FailureMessage(id, self.path))
      }
    //    case s:SuccessMessage=> startTransaction()

  }

  override def persistenceId: String = "test-saga"
}

class TestSagaActorSpec() extends TestKit(ActorSystem("TestSagaActorSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  val dest = system.actorOf(Props(classOf[Destination]))
  val sagaActor = system.actorOf(Props(classOf[TestSagaActor], dest.path))

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "SagaActor" must {

    "send successful messages will be succeeded" in {
      //      TestActors.echoActorProps
      val id = UUID.randomUUID()
      sagaActor ! IsSuccessful(true, id)
      expectMsg(id)
      sagaActor ! GetState
      expectMsg(List(id))
    }
    "send failure messages will be not changed for state" in {
      //      TestActors.echoActorProps
      val id = UUID.randomUUID()
      sagaActor ! IsSuccessful(false, id)
      expectMsg(id)
      sagaActor ! GetState
      expectMsg(List.empty[UUID])
    }

  }
}