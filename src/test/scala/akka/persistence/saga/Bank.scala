package akka.persistence.saga

import java.util.UUID

import akka.actor.{ActorSelection, ActorSystem}
import akka.persistence.saga.Bank._
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

object Bank {

  import Saga._


  case class Withdraw(id: UUID, amount: Long)

  case class Withdrew(id: UUID, amount: Long, afterBal: Long) {
    require(afterBal >= 0)
  }

  case class Deposit(id: UUID, amount: Long)

  case class Deposited(id: UUID, amount: Long, afterBal: Long)

  case object Err


}

class Bank extends Saga {
  var balance: Long = 0
  var ids = Set.empty[UUID]

  override def commit: PartialFunction[Saga.Succeeded, Unit] = ???

  override def rollback: PartialFunction[Saga.Failed, Unit] = ???

  override def receiveRecover: Receive = {
    case evt:Any => updateState(evt)
  }

  override def receiveCommand: Receive = {
    case Withdraw(id, _) if ids.contains(id)  =>
      sender() ! Err

    case Deposit(id, _) if ids.contains(id) =>
      sender() ! Err
    case Withdraw(id, amount) =>

      val afterBal=balance-amount
      if (afterBal>=0){
        persist(Withdrew(id,amount,balance-amount))(updateState)
      } else{
        sender() ! Err
      }


    case Deposit(id, amount)  =>
      persist(Deposited(id,amount,balance+amount))(updateState)
  }
  def updateState(evt:Any)={
    evt match {
      case Withdrew(id,_,afterBal)=>
        ids+=id
        balance=afterBal
      case Deposited(id,_,afterBal)=>
        ids+=id
        balance=afterBal
    }
  }

  override def persistenceId: String = ???
}

class BankSpec() extends TestKit(ActorSystem("BankSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An Echo actor" must {

    "send back messages unchanged" in {
      val echo = system.actorOf(TestActors.echoActorProps)
      echo ! "hello world"
      expectMsg("hello world")
    }

  }
}