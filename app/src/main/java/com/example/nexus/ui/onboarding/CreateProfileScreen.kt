package com.example.nexus.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.R
import com.example.nexus.ui.theme.NexusTheme

/**
 * The Onboarding Screen for creating a new user profile.
 *
 * This Composable is "stateful" in that it is connected to a ViewModel.
 * It observes the username state from the ViewModel and calls functions on the ViewModel
 * when the user interacts with the UI (changing text, clicking the button).
 *
 * @param viewModel The ViewModel that holds the state and logic for this screen.
 * @param onProfileCreated A callback lambda function that is invoked when the profile
 *                         creation is complete, used to trigger navigation to the next screen.
 */

@Composable
fun CreateProfileScreen(
    viewModel: CreateProfileViewModel,
    onProfileCreated: () -> Unit
) {
    // The state for the username is now collected directly from the ViewModel.
    // The UI will automatically "recompose" (redraw) whenever this value changes.
    val username by viewModel.username.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Decorative Image (Top)
            Image(
                painter = painterResource(id = R.drawable.profile_art), // Ensure this resource exists
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = ContentScale.Fit // Fit might work better than Crop
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Header Text
            Text(
                text = stringResource(R.string.create_your_profile),
                style = MaterialTheme.typography.headlineSmall, // Using MaterialTheme for consistency
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.profile_creation_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Add Profile Photo Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_add_a_photo), // Ensure this resource exists
                    contentDescription = stringResource(R.string.add_profile_photo),
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.add_profile_photo),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Username Text Field - now connected to the ViewModel
            OutlinedTextField(
                value = username,
                onValueChange = { newUsername -> viewModel.onUsernameChange(newUsername) },
                label = { Text(stringResource(R.string.username)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // This spacer pushes the button to the bottom
            Spacer(modifier = Modifier.weight(1f))

            // Create Profile Button - now connected to the ViewModel and the navigation callback
            Button(
                onClick = {
                    viewModel.onCreateProfileClicked()
                    onProfileCreated()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = username.isNotBlank(), // Button is disabled until user types a name
                shape = RoundedCornerShape(25.dp) // Creates the pill shape
            ) {
                Text(stringResource(R.string.create_profile), fontSize = 18.sp)
            }

            // Spacer for bottom padding
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
