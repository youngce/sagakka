package akka.persistence.saga.example.bank

import java.util.UUID

import akka.actor.{ActorPath, Props}
import akka.persistence.PersistentActor
import akka.persistence.saga.Saga
import akka.persistence.saga.Saga._
import akka.persistence.saga.example.bank.TransactionActor.{Create, Created}

object BankActor {

  // if amount >0 is withdraw , else deposit
  case class Transact(id: UUID, amount: Long)

  case class Transacted(id: UUID, amount: Long, afterBalance: Long) extends Saga.Event

}

object TransactionActor {

  case class Transaction(id: UUID, amount: Long, afterBalance: Long, bank: String)


  case class Create(id: UUID, amount: Long, replyTo: ActorPath) extends Saga.Command

  case class Created(id: UUID, amount: Long, bank: String) extends Saga.Succeeded

  case class Update(id: UUID, afterBalance: Long, replyTo: ActorPath) extends Saga.Command

  case class Updated(id: UUID, afterBalance: Long) extends Saga.Succeeded

  case class AlreadyExisted(id: UUID) extends Saga.Failed


}

class TransactionActor extends PersistentActor {

  import TransactionActor._


  var state: Option[Transaction] = None

  override def receiveRecover: Receive = {
    case evt: Created => updateState(evt)
  }

  override def receiveCommand: Receive = {
    case Create(id, amount, replyTo) =>
      val replySelection = context.actorSelection(replyTo)

      state match {
        case Some(txn) =>
          if (txn == Transaction(id, amount, 0, replyTo.name)) {
            replySelection ! Created(id, amount, replyTo.name)
          } else {
            replySelection ! AlreadyExisted(id)
          }

        case None =>
          persist(Created(id, amount, replyTo.name))(evt => {
            updateState(evt)
            replySelection ! evt
          })
      }

  }

  def updateState(evt: Saga.Succeeded): Unit = {
    evt match {
      case Created(_, amount, bank) => state = state.map(_.copy(amount = amount, bank = bank))
      case Updated(_, afterBalance) => state = state.map(_.copy(afterBalance = afterBalance))
    }
  }

  override def persistenceId: String = s"txn-${self.path}"
}

class BankActor extends Saga {

  import BankActor._

  var balance: Long = 0

  override def commit: ReceiveSucceeded = {
    case Created(id, amount, _) => _ =>
      persist(Transacted(id, amount, balance + amount))(evt => {
        balance += evt.amount
      })


  }

  override def rollback: ReceiveFailed = ???

  override def receiveRecover: Receive = ???

  override def receiveCommand: Receive = {
    case Transact(id, amount) =>
      val txnActor = context.child(s"txn-$id")
        .getOrElse(context.actorOf(Props(classOf[TransactionActor])))
      startTransaction(txnActor.path, sender())(Create(id, amount, self))
  }

  override def persistenceId: String = ???
}
