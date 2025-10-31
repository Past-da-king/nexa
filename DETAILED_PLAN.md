# The Nexa Unified Protocol: Detailed Plan

## 1. Core Philosophy

This document outlines a single, unified communication protocol for the Nexa application. It is designed from the ground up to be:

1.  **Decentralized & Offline-First:** The protocol does not rely on a central server. It assumes peers may be offline and uses a store-and-forward model for all communication.
2.  **Unified:** There is only **one** way to send data. The same protocol is used for Direct Messages, Group Messages, Friend Requests, and Public Announcements. The sending device does not need to know if the recipient is online or offline.
3.  **Extensible:** The protocol is designed to be easily extended with new types of messages and content in the future without changing the core routing logic.

---

## 2. Core Concepts

### 2.1 The `DtnMessage`: The Universal Envelope

Every piece of data sent across the network is packaged in a universal "envelope" called a `DtnMessage`.

The structure of every `DtnMessage` is as follows:

| Field | Type | Description |
| :--- | :--- | :--- |
| `ID` | `String` | A unique ID for this specific message to prevent processing the same message twice. |
| `SOURCE_ID` | `String` | The User ID of the original sender. |
| `DESTINATION_ID` | `String` | **(Crucial)** The ID of the intended recipient(s). See section 2.2. |
| `MESSAGE_TYPE` | `Enum` | Defines what the payload contains. See section 2.3. |
| `PAYLOAD` | `String` | The actual data (e.g., a text message object), serialized into a JSON string. |
| `HOP_COUNT` | `Integer` | The number of times this message can be forwarded before it is dropped. |
| `TTL` | `Timestamp`| The time when the message expires and should be deleted by any node holding it. |

### 2.2 The `DESTINATION_ID`: User, Group, or Broadcast

The `DESTINATION_ID` field is the key to flexible routing. It can be one of three formats:

1.  **User ID (for DMs, Friend Requests):** A standard user's unique ID. A node will process this message only if the `DESTINATION_ID` matches its own User ID.
2.  **Group ID (for Group Messages):** A unique ID representing a channel. A node will process this message only if it is a member of that group.
3.  **Broadcast ID (for Announcements, Profile Discovery):** A special, reserved string (e.g., `"NEXA_BROADCAST_ALL"`). Every node that receives a message with this ID will process it. This is how public announcements and discoveries will work.

### 2.3 `MESSAGE_TYPE` & `PAYLOAD`: Making the Protocol Extensible

These two fields work together. The `MESSAGE_TYPE` enum tells the receiving device what kind of object is inside the `PAYLOAD`. This allows us to add new features easily.

**Initial Message Types:**

| `MESSAGE_TYPE` | `PAYLOAD` Contains a JSON object of... |
| :--- | :--- |
| `DIRECT_MESSAGE` | `Message` (text, image, etc.) |
| `GROUP_MESSAGE` | `Message` (text, image, etc.) |
| `FRIEND_REQUEST` | `FriendRequest` (containing sender's name, etc.) |
| `GROUP_INVITE` | `GroupInvite` (containing group name, inviter, etc.) |
| `PUBLIC_CHANNEL_ANNOUNCEMENT`| `Channel` (the full channel object) |
| `PROFILE_UPDATE` | `UserProfile` (announcing a user's name/pic change) |

To add a new feature, we simply define a new `MESSAGE_TYPE` and its corresponding `Payload` object. The core routing logic does not need to change.

---

## 3. The Protocol Algorithms (Pseudocode)

### Algorithm 1: `Send` (The Sender's Only Action)

This is the **only** function the application calls to send any data.

```pseudocode
FUNCTION Send(DESTINATION_ID, CONTENT_OBJECT):

  // 1. Create the universal DtnMessage envelope.
  DTN_MESSAGE = new DtnMessage(
      ID:          generate_unique_id(),
      SOURCE:      MY_USER_ID,
      DESTINATION: DESTINATION_ID, // Can be a User ID, Group ID, or BROADCAST_ID
      PAYLOAD:     serialize_to_json(CONTENT_OBJECT),
      TYPE:        get_message_type_for(CONTENT_OBJECT),
      HOP_COUNT:   get_default_hop_count(),
      TTL:         calculate_expiry_time()
  )

  // 2. Store the message in your own outbox.
  MyDatabase.save(DTN_MESSAGE)

  // 3. Immediately hand it off to the routing engine.
  Route(DTN_MESSAGE, sender_peer=NULL)

END FUNCTION
```

### Algorithm 2: `OnReceive` (The Node's Only Action)

This is the **only** function that runs when a node receives a `DtnMessage` from another peer.

```pseudocode
FUNCTION OnReceive(DTN_MESSAGE, SENDER_PEER):

  // 1. Prevent Loops: Have I seen this message before?
  IF MyDatabase.has(DTN_MESSAGE.ID):
    DISCARD // Already processed or forwarded this.
    RETURN
  END IF

  // 2. Store It: Keep a copy to help it travel across the network.
  MyDatabase.save(DTN_MESSAGE)

  // 3. Check Destination: Is this message relevant to me?
  IS_FOR_ME = FALSE
  IF DTN_MESSAGE.DESTINATION == MY_USER_ID:
    IS_FOR_ME = TRUE // It's a DM or Friend Request for me.
  ELSE IF DTN_MESSAGE.DESTINATION == "NEXA_BROADCAST_ALL":
    IS_FOR_ME = TRUE // It's a public announcement for everyone.
  ELSE IF is_group_id(DTN_MESSAGE.DESTINATION) AND I_am_member_of(DTN_MESSAGE.DESTINATION):
    IS_FOR_ME = TRUE // It's a message for a group I'm in.
  END IF

  // 4. Process or Forward
  IF IS_FOR_ME:
    // This message is for me. Process its contents.
    ProcessPayload(DTN_MESSAGE.PAYLOAD, DTN_MESSAGE.TYPE)
  END IF

  // 5. Always Forward: Regardless of whether it was for me, I must help it travel.
  Route(DTN_MESSAGE, SENDER_PEER)

END FUNCTION
```

### Algorithm 3: `Route` (The Core Forwarding Logic)

This function decides where a message should go next.

```pseudocode
FUNCTION Route(DTN_MESSAGE, SENDER_PEER):

  // 1. Check if the message is still alive.
  IF DTN_MESSAGE.HOP_COUNT <= 0:
    RETURN // No more hops left.
  END IF

  // 2. Decrement hop count for the next leg.
  FORWARD_MESSAGE = DTN_MESSAGE.copy(hopCount = DTN_MESSAGE.hopCount - 1)

  // 3. Direct Delivery Optimization: Is the final recipient connected right now?
  DIRECT_RECIPIENT = get_currently_connected_peer(FORWARD_MESSAGE.DESTINATION)
  IF DIRECT_RECIPIENT IS NOT NULL:
      SendToPeer(DIRECT_RECIPIENT, FORWARD_MESSAGE)
      RETURN // Sent directly, our job is done for now.
  END IF

  // 4. Broadcast Forwarding: Recipient is not online. Forward to everyone else.
  ALL_CONNECTED_PEERS = get_all_currently_connected_peers()
  FOR EACH PEER in ALL_CONNECTED_PEERS:
    // Do not send it back to the person who just gave it to us.
    IF PEER.ID != SENDER_PEER.ID:
      SendToPeer(PEER, FORWARD_MESSAGE)
    END IF
  END FOR

END FUNCTION
```

---

## 4. The Friendship Model

Friendship is now just a social layer built on top of this protocol. It does **not** affect routing.

*   When User A wants to add User B, the app calls:
    `Send(DESTINATION_ID=USER_B_ID, CONTENT_OBJECT=FriendRequestObject)`
*   The `DtnMessage` travels the network.
*   When User B's device receives it, `OnReceive` runs. It sees the destination is for User B and the type is `FRIEND_REQUEST`. It shows a notification in the UI.
*   If User B accepts, their app calls:
    `Send(DESTINATION_ID=USER_A_ID, CONTENT_OBJECT=FriendAcceptObject)`
*   When User A's device receives the acceptance, it updates User B's status to "Friend", and a private chat can now be initiated.

This model is clean, robust, and directly implements the logic you have requested. Please let me know your thoughts.
