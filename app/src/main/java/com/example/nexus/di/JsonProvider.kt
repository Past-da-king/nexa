package com.example.nexus.di

import com.example.nexus.models.TransmissionPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Provides a single, globally configured instance of the Json serializer.
 * This is crucial for ensuring that polymorphic serialization for our sealed class
 * works consistently across the entire application.
 */
val AppJson = Json {
    // This is the blueprint for how to handle our sealed class.
    serializersModule = SerializersModule {
        // We define a polymorphic relationship for our base class, TransmissionPayload.
        polymorphic(TransmissionPayload::class) {
            // For that base class, we register its two concrete subclasses.
            subclass(TransmissionPayload.Handshake::class)
            subclass(TransmissionPayload.EncryptedMessage::class)
        }
    }
    // Change the class discriminator to avoid conflict with the 'type' property
    classDiscriminator = "messageType"
    // This tells the serializer to use the class's simple name as the discriminator value
    useArrayPolymorphism = false
}