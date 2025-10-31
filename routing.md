Our Algorithm: Simplified Epidemic Routing

What it is:
We are implementing a **Simplified Epidemic Routing** protocol for our Delay-Tolerant Network (DTN). This approach is inspired by the way diseases spread, aiming to maximize message delivery probability by aggressively replicating messages across the network. Unlike direct delivery, our simplified version ensures messages are forwarded through intermediary nodes, not just directly to the final recipient.

How it Works (The Core Rule):
When two devices connect, they perform a handshake and exchange "summary vectors"—a list of all the message IDs they are currently holding in their DTN storage. Each device then requests and receives copies of any messages the other device has that it doesn't, **regardless of whether the message is destined for the requesting device itself.**

Let's trace it with our users (revisited):
1.  Alice has a QUEUED message for Dave.
2.  Alice connects to Bob. They perform their handshake and exchange summary vectors.
3.  Bob's device sees that Alice has a message for Dave that he doesn't have. Bob says, "Give me a copy of that message."
4.  Alice forwards the encrypted message to Bob. Now both Alice and Bob have a copy.
5.  Later, Bob connects to Charlie. They repeat the process. Charlie sees that Bob has a message for Dave that she doesn't have. She requests a copy. Now Alice, Bob, and Charlie all have a copy.
6.  Finally, Charlie connects to Dave. She forwards the message, and it is successfully delivered.

Why the Name "Epidemic"?
Just like an epidemic, the message "infects" every node (device) it comes into contact with. This rapidly spreads copies of the message throughout the network, creating many redundant paths and massively increasing the chance that one of those copies will eventually find its way to the final destination.

Key Features of Our Simplified Epidemic Routing:

To ensure a robust and manageable implementation, our protocol will include the following essential features:

*   **Time-To-Live (TTL):** Each message will have a defined Time-To-Live. Messages will be automatically dropped from a device's storage once their TTL expires, preventing indefinite persistence and managing storage.
*   **Hop Counts:** Messages will carry a hop count, which will be decremented each time the message is forwarded to a new device. Messages will be dropped if their hop count reaches zero, limiting excessive replication and preventing infinite loops.
*   **Storage Limits:** Each device will have a defined storage limit for DTN messages. If this limit is reached, a policy (e.g., dropping the oldest messages, or lowest priority messages) will be applied to free up space for new messages.
*   **Duplicate Detection and Suppression:** Devices will efficiently detect and suppress duplicate messages. If a device already possesses a message (identified by its unique ID), it will not request or store another copy, preventing redundant storage and exchange.

What We Are NOT Implementing (for now):

To keep the initial implementation focused and manageable, we will defer the following advanced features:

*   **Advanced Summary Vectors:** We will use simple lists of message IDs rather than more complex structures like Bloom Filters.
*   **Probabilistic Forwarding:** Messages will be forwarded deterministically based on the rules, without introducing probabilistic decisions to limit spread.
