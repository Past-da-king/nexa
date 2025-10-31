package com.example.nexus

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nexus.data.repositories.ConnectionRepository
import com.example.nexus.data.repositories.UserRepositoryImpl
import com.example.nexus.ui.chat.ChatScreen
import com.example.nexus.ui.chat.ChatViewModel
import com.example.nexus.ui.components.BottomNavigationBar
import com.example.nexus.ui.discovery.PeerDiscoveryScreen
import com.example.nexus.ui.discovery.PeerDiscoveryViewModel
import com.example.nexus.ui.home.HomeScreen
import com.example.nexus.ui.dtn.DtnSettingsScreen
import com.example.nexus.ui.home.HomeViewModel
import com.example.nexus.ui.onboarding.CreateProfileScreen
import com.example.nexus.ui.onboarding.CreateProfileViewModel
import com.example.nexus.ui.channel.CreateChannelScreen
import com.example.nexus.ui.channel.ChannelDetailsScreen
import com.example.nexus.ui.channel.InviteMembersScreen
import com.example.nexus.ui.channel.PublicChannelsScreen
import com.example.nexus.models.MessageType
import com.example.nexus.ui.theme.NexusTheme
import dagger.hilt.android.AndroidEntryPoint
import android.content.pm.PackageManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var connectionRepository: ConnectionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. Create our repository instances here ---
        val userRepository = UserRepositoryImpl(applicationContext)

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.RECORD_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.RECORD_AUDIO
            )
        }
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)

        // --- 3. SET THE UI CONTENT ---

        setContent {
            NexusTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // We create one instance of the UserRepository here just for the initial route decision.
                val userRepository = UserRepositoryImpl(applicationContext)

                Scaffold(
                    bottomBar = {
                        val mainScreenRoutes = listOf("home", "discovery", "settings")
                        if (currentRoute in mainScreenRoutes) {
                            BottomNavigationBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (userRepository.isUserOnboarded()) "home" else "onboarding",
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        composable("onboarding") {
                            val viewModel: CreateProfileViewModel = hiltViewModel()
                            CreateProfileScreen(
                                viewModel = viewModel,
                                onProfileCreated = {
                                    navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                                }
                            )
                        }

                        composable("home") {
                            val viewModel: HomeViewModel = hiltViewModel()
                            val conversations by viewModel.conversations.collectAsState()
                            HomeScreen(
                                conversations = conversations,
                                onConversationClicked = { conversation ->
                                    if (conversation.isGroup) {
                                        navController.navigate("channelDetails/${conversation.conversationId}")
                                    } else {
                                        navController.navigate("chat/${conversation.conversationId}")
                                    }
                                },
                                onNewChatClicked = {
                                    navController.navigate("discovery")
                                },
                                onNewChannelClicked = { navController.navigate("createChannel") },
                                onPublicChannelsClicked = { navController.navigate("publicChannels") },
                            )
                        }

                        composable("createChannel") {
                            CreateChannelScreen(
                                onChannelCreated = {
                                    navController.popBackStack()
                                },
                                onUp = { navController.popBackStack() }
                            )
                        }

                        composable("channelDetails/{channelId}") { backStackEntry ->
                            val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                            ChannelDetailsScreen(
                                onNavigateToChat = { id -> navController.navigate("chat/$id") },
                                onNavigateToInviteMembers = { id -> navController.navigate("inviteMembers/$id") },
                                onUp = { navController.popBackStack() }
                            )
                        }

                        composable("inviteMembers/{channelId}") { backStackEntry ->
                            val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                            InviteMembersScreen(
                                onInvitesSent = { navController.popBackStack() },
                                onUp = { navController.popBackStack() }
                            )
                        }

                        composable("publicChannels") {
                            PublicChannelsScreen(
                                onChannelJoined = { channelId ->
                                    navController.navigate("channelDetails/$channelId") {
                                        popUpTo("publicChannels") { inclusive = true }
                                    }
                                },
                                onUp = { navController.popBackStack() }
                            )
                        }

                        composable("discovery") {
                            val viewModel: PeerDiscoveryViewModel = hiltViewModel()

                            PeerDiscoveryScreen(
                                navController = navController,
                                onAcceptRequest = { contact ->
                                    viewModel.acceptFriendRequest(contact)
                                },
                                onRejectRequest = { stableId -> viewModel.rejectFriendRequest(stableId) },
                            )


                        }

                        composable("settings") {
                            DtnSettingsScreen()
                        }

                        composable("chat/{conversationId}") { backStackEntry ->
                            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: "unknown"
                            val viewModel: ChatViewModel = hiltViewModel() // Hilt automatically handles the ID
                            val state by viewModel.state.collectAsState()

                            ChatScreen(
                                state = state,
                                onSendMessage = { text: String, data: String?, type: MessageType -> viewModel.sendMessage(text, data, type) },
                                onDisconnectClicked = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            connectionRepository.setPermissionsGranted(allPermissionsGranted)
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }
}