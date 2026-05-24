package com.example.ui.screens

import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.service.FirebaseServiceHelper
import com.example.ui.theme.AnimeAccent
import com.example.ui.theme.AnimeCardBg
import com.example.ui.theme.AnimeDarkBg
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.AnimeSecondary

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    var email by remember { mutableStateFlowOf("") }
    var password by remember { mutableStateFlowOf("") }
    var passwordVisible by remember { mutableStateFlowOf(false) }
    var isLoading by remember { mutableStateFlowOf(false) }

    // Validation Status
    var emailError by remember { mutableStateFlowOf<String?>(null) }
    var passwordError by remember { mutableStateFlowOf<String?>(null) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Configure Google Sign-In options
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("732211806324-gclsm0ggrco7oigk17be9ub8t38rshfc.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    // Google Sign-In execution launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    isLoading = true
                    FirebaseServiceHelper.socialSignInGoogle(idToken) { success, err ->
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "Logged in via Google successfully!", Toast.LENGTH_SHORT).show()
                            onNavigateToHome()
                        } else {
                            Toast.makeText(context, "Firebase authentication failed: $err", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Google Sign-In returned empty ID Token. Verify your Firebase console configuration.", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                val isSimulated = !FirebaseServiceHelper.isFirebaseAvailable.value
                if (isSimulated) {
                    // Fallback to simulated mode
                    isLoading = true
                    FirebaseServiceHelper.socialSignInGoogle("google_simulated_id_token") { success, _ ->
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "Simulated Google Sign-In Success!", Toast.LENGTH_SHORT).show()
                            onNavigateToHome()
                        }
                    }
                } else {
                    Toast.makeText(context, "Google Sign-In Error. ApiException Code: ${e.statusCode}. Ensure SHA-1 fingerprint and Web Client ID are registered in Google Cloud Console / Firebase.", Toast.LENGTH_LONG).show()
                    Log.e("LoginScreen", "Google Sign-In API Exception", e)
                }
            }
        } else {
            Toast.makeText(context, "Google Sign-In canceled.", Toast.LENGTH_SHORT).show()
        }
    }

    fun performValidation(): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            emailError = "Email field is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Please enter a valid email"
            isValid = false
        } else {
            emailError = null
        }

        if (password.isEmpty()) {
            passwordError = "Password field is required"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordError = null
        }

        return isValid
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnimeDarkBg)
    ) {
        // Aesthetic ambient gradient spheres behind contents
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AnimePrimary.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AnimeSecondary.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.linearGradient(colors = listOf(AnimePrimary, AnimeSecondary)),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AX",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    style = androidx.compose.ui.text.TextStyle(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome Back!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Log in to stream AnimEx Pro",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Username/Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                label = { Text("Email Address") },
                placeholder = { Text("Enter your email") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "EmailIcon",
                        tint = AnimeSecondary
                    )
                },
                isError = emailError != null,
                supportingText = {
                    if (emailError != null) {
                        Text(text = emailError!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                    focusedLabelColor = AnimeSecondary,
                    focusedBorderColor = AnimeSecondary,
                    cursorColor = AnimeAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("username_input")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Password Input Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "LockIcon",
                        tint = AnimeSecondary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "PasswordVisibility",
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = passwordError != null,
                supportingText = {
                    if (passwordError != null) {
                        Text(text = passwordError!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                    focusedLabelColor = AnimeSecondary,
                    focusedBorderColor = AnimeSecondary,
                    cursorColor = AnimeAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_input")
            )

            // Forgot Password Anchor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Forgot Password?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AnimeSecondary,
                    modifier = Modifier
                        .clickable { onNavigateToForgotPassword() }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sign In Gradient Button
            Button(
                onClick = {
                    if (performValidation()) {
                        isLoading = true
                        FirebaseServiceHelper.loginWithEmail(email.trim(), password) { success, error ->
                            isLoading = false
                            if (success) {
                                Toast.makeText(context, "Welcome back to AnimEx Pro!", Toast.LENGTH_SHORT).show()
                                onNavigateToHome()
                            } else {
                                Toast.makeText(context, "Login Failed: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_button")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(colors = listOf(AnimePrimary, AnimeSecondary)),
                            shape = RoundedCornerShape(26.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divider Line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.15f)
                )
                Text(
                    text = "OR CONTINUE WITH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.15f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Social Login Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Google Button
                SocialLoginButton(
                    logoTex = "G",
                    gradientColors = listOf(Color(0xFFEA4335), Color(0xFFFBBC05)),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (FirebaseServiceHelper.isFirebaseAvailable.value) {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        } else {
                            isLoading = true
                            FirebaseServiceHelper.socialSignInGoogle("google_simulated_id_token") { success, _ ->
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "Simulated Google Sign-In Success!", Toast.LENGTH_SHORT).show()
                                    onNavigateToHome()
                                }
                            }
                        }
                    }
                )

                // Facebook Button
                SocialLoginButton(
                    logoTex = "f",
                    gradientColors = listOf(Color(0xFF1877F2), Color(0xFF00C6FF)),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        isLoading = true
                        FirebaseServiceHelper.socialSignInFacebook("facebook_dummy_access_token") { success, err ->
                            isLoading = false
                            if (success) {
                                onNavigateToHome()
                            } else {
                                Toast.makeText(context, "Facebook login failed: $err", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )

                // GitHub Button
                SocialLoginButton(
                    logoTex = "Git",
                    gradientColors = listOf(Color(0xFF24292E), Color(0xFF4F5357)),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        isLoading = true
                        FirebaseServiceHelper.socialSignInGitHub("github.com") { success, err ->
                            isLoading = false
                            if (success) {
                                onNavigateToHome()
                            } else {
                                Toast.makeText(context, "GitHub login started", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Anchor link to Register Screen
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "New to AnimEx Pro?",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sign Up Now",
                    color = AnimeAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToRegister() }
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun SocialLoginButton(
    logoTex: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .background(AnimeCardBg, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        Brush.linearGradient(gradientColors),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = logoTex,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (logoTex) {
                    "G" -> "Google"
                    "f" -> "Facebook"
                    else -> "GitHub"
                },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// Custom Helper to keep mutable states safe and readable inside Kotlin Compose
fun <T> mutableStateFlowOf(value: T): MutableState<T> = mutableStateOf(value)
