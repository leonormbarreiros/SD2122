# Turmas

### Our solution

**Replication** allows for the maintenance of data in multiple machines, which enhances performance, availability and fault tolerance. That means our system has 2 servers: the primary and the secondary.
The replication we implemented is lazy replication, which means the replicas can diverge. However, by using the **gossip** framework, they can only diverge for so long.
So, periodically, the replica managers exchange messages, and communicate the updates they received from each client.

Our main work for phase 3 was the implementation of how the RM combine their updates.
- Whenever each server receives a write request from a client, it checks its own state to see if it's possible (eg.: if it's a student who wants to enroll in the class, checks if the enrollments are open and if there is still room in the class)
- If the write is (locally) possible, it is accepted. However,  it is also saved in a list of writes, along with a timestamp (implemented with a Lamport's logical clock) of when it occured
- When one of the replicas decides to propagate its state, it sends all the timestamped writes. Our algorithm ensures that after propagation, everything that "happened before" is safe and will not be cancelled (which is why we didn't use vector clocks). Also, after one propagation, both servers will have exactly the same state.

The algorithm works as follows:
- One server propagates its state (P or S) to the other one
- When the other one receives the message it performs a "unify state" operation:
  - If the writes performed by the sender contain an "open" write, the full state of the sender is copied
  - Each "enroll" write is added to a list of possibly final writes. Because the sender's write are concurrent with my own, there's no way to know which happened before, so we alternate them in ascending order of their own clocks 
    - Eg.: the sender enrolled aluno1111:leonor at time 3 and aluno2222:juliana at time 4; the receiver enrolled aluno3333:diogo at time 7
    - The list of writes will be: aluno1111:leonor->aluno3333:diogo->aluno2222:juliana 
    - Note: the algorithm only works if the list of writes is always the same, whether the sender is P or S
  - If this doesn't exceed capacity, put the sender's writes on my own state, and send my state (when the response is received, the state is simply copied - after some validations)
  - If it does, add each write from the list until it's not possible; the writes that exceed capacity get cancelled

With the right modifications (eg.: periodical lookups instead of only one at the start, P unifies with S1 and then with S2, and writes are only final after both unifications), this algorithm will work with more than 2 servers. 
    



