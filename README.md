## Saga Pattern Tips
### Create a Unique ID per Transaction

Having a unique identifier for each transaction is a common technique for traceability, but it also helps participants to have a standard way to request data from each other. By using a transaction Id, for instance, Delivery Service could ask Stock Service where to pick up the products and double check with the Payment Service if the order was paid.
### Add the Reply Address Within the Command

Instead of designing your participants to replyTo to a fixed address, consider sending the replyTo address within the message, this way you enable your participants to replyTo to multiple orchestrators.
### Idempotent Operations

If you are using queues for communication between services (like SQS, Kafka, RabbitMQ, etc.), I personally recommended you make your operations Idempotent. Most of those queues might deliver the same message twice.

It also might increase the fault tolerance of your service. Quite often a bug in a client might trigger/replay unwanted messages and mess up with your database.
### Avoiding Synchronous Communications

As the transaction goes, don't forget to add into the message all the data needed for each operation to be executed. The whole goal is to avoid synchronous calls between the services just to request more data. It will enable your services to execute their local transactions even when other services are offline.

## References
* [Saga Pattern | How to Implement Business Transactions Using Microservices - Part I](https://dzone.com/articles/saga-pattern-how-to-implement-business-transaction)