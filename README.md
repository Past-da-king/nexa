# Nexa: Offline-First Social Networking

**NEXA is a decentralized, peer-to-peer mobile messaging application designed to provide reliable communication for rural and township communities where internet access is limited or unreliable.**

This project is a Capstone submission for the University of Cape Town's Department of Computer Science. It leverages modern Android development practices to create a robust, secure, and truly offline-first social networking experience.

## Core Features (Implemented in Current Prototype)

The current evolutionary prototype has successfully implemented a full end-to-end vertical slice of the most critical user journey.

*   ✅ **Secure, Device-Local Identity:** Users can create a persistent profile with a unique, permanent ID without requiring a phone number or email address.
*   ✅ **Peer Discovery & Connection:** The app uses the Nearby Connections API to discover and connect with nearby peers using a combination of Bluetooth and Wi-Fi Direct.
*   ✅ **Connection Confirmation:** To ensure user privacy and control, a connection request from a peer must be explicitly accepted via an on-screen dialog before a connection is established.
*   ✅ **Direct P2P Messaging:** Once connected, users can send and receive text messages in a dedicated chat screen in near real-time.
*   ✅ **Persistent Chat History:** All conversations are saved locally on the device. When reconnecting with a peer, the previous chat history is automatically loaded.

## Technical Architecture & Tech Stack

Nexa is built with a modern, scalable, and security-conscious technology stack.

*   **Platform:** Android
*   **Language:** Kotlin
*   **User Interface:** 100% Jetpack Compose for a declarative and reactive UI.
*   **Architecture:** Model-View-ViewModel (MVVM) to ensure a clean separation of concerns between UI, state management, and business logic.
*   **State Management:** Kotlin Coroutines and StateFlow are used to manage asynchronous operations and expose application state to the UI layer.
*   **P2P Communication:** [Google Nearby Connections API](https://developers.google.com/nearby/connections/overview) for managing the entire lifecycle of discovery, advertising, and data transfer.
*   **Data Persistence:** Chat histories are serialized to JSON files using `kotlinx.serialization` and stored in the app's private internal storage. *(This will be migrated to a Jetpack Room database for the final version to enable more complex queries and better performance).*
*   **Planned Security Layer:**
    *   **Key Storage:** Android Keystore System
    *   **End-to-End Encryption:** Google Tink Cryptography Library

## Getting Started

To build and run the Nexa prototype, you will need the following:

#### Prerequisites

1.  **Android Studio:** Hedgehog (2023.1.1) or newer.
2.  **Two Android Devices:** To properly test the P2P functionality, you need two physical Android devices (API 26+) or two instances of the Android Emulator.
    *   *Note: Emulator testing may require specific network configurations.*

#### Build and Run

1.  **Clone the repository:**
    ```bash
    git clone <your-repository-url>
    ```
2.  **Open in Android Studio:** Open the cloned project folder in Android Studio. It will automatically sync the Gradle files.
3.  **Run the App:**
    *   Connect your two Android devices to your computer.
    *   Select the first device in Android Studio and click the "Run" button.
    *   Select the second device and click "Run" again.
4.  **Grant Permissions:** When the app launches for the first time, it will request permissions for **Location, Nearby Devices (Bluetooth/Wi-Fi)**. You must grant these permissions for the app to function correctly.

## Project Status

This project is currently at the **Stage 3: Evolutionary Prototype** milestone.

#### What's Been Achieved:

*   The core architectural framework (MVVM) is in place.
*   The P2P communication pipeline has been successfully validated from end to end.
*   The application state is managed cleanly, allowing for seamless navigation between onboarding, discovery, and in-app chat states.
*   Basic data persistence for chat history is functional.

#### Next Steps & Future Work

The next development sprints will build upon this stable foundation to implement the remaining core features:

*   **Integrate Security Layer:** Implement end-to-end encryption for all message payloads using Google Tink and the Android Keystore.
*   **Implement Store-and-Forward (DTN):** Develop the routing service for hopping messages through intermediate peers.
*   **Group Chat / Community Channels:** Build the functionality for creating and participating in multi-user conversations.
*   **UI/UX Polish:** Refine the user interface, add user avatars, and improve the overall user experience.
*   **Database Migration:** Move from file-based storage to a robust Jetpack Room database.

## Team Nexus

*   **Maphuti Shilabje**
*   **Ayanda Phaketsi**
*   **Falakhe Mkhize**

**Client/Supervisor:** Josiah Chavula
**Tutor:** Siyanda Makhathini