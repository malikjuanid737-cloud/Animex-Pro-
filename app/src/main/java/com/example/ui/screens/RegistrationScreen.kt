package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.FirebaseServiceHelper
import com.example.ui.theme.AnimeAccent
import com.example.ui.theme.AnimeDarkBg
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.AnimeSecondary

@Composable
fun RegistrationScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var fullName by remember { mutableStateFlowOf("") }
    var email by remember { mutableStateFlowOf("") }
    var password by remember { mutableStateFlowOf("") }
    var confirmPassword by remember { mutableStateFlowOf("") }

    var passwordVisible by remember { mutableStateFlowOf(false) }
    var confirmPasswordVisible by remember { mutableStateFlowOf(false) }
    var isLoading by remember { mutableStateFlowOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Real-time Validation Checks
    val isNameValid = fullName.isNotBlank() && fullName.length >= 2
    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.length >= 6
    val isConfirmPasswordValid = confirmPassword.isNotEmpty() && confirmPassword == password

    // Password strength logic
    val passwordStrength = remember(password) {
        when {
            password.isEmpty() -> Pair(0f, "Empty")
            password.length < 6 -> Pair(0.33f, "Weak")
            password.any { it.isDigit() } && password.any { it.isUpperCase() } -> Pair(1.0f, "Strong")
            else -> Pair(0.66f, "Medium")
        }
    }

    val strengthColor = when (passwordStrength.second) {
        "Weak" -> Color(0xFFE53935)  // Red
        "Medium" -> Color(0xFFFFB300) // Amber/Yellow
        "Strong" -> Color(0xFF43A047) // Green
        else -> Color.DarkGray
    }

    fun handleRegister() {
        if (!isNameValid || !isEmailValid || !isPasswordValid || !isConfirmPasswordValid) {
            Toast.makeText(context, "Please satisfy all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        FirebaseServiceHelper.registerWithEmail(fullName.trim(), email.trim(), password) { success, error ->
            isLoading = false
            if (success) {
                Toast.makeText(context, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                onNavigateToHome()
            } else {
                Toast.makeText(context, "Failed: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnimeDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Join AnimEx Pro",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Create your account to start streaming",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 1. Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                placeholder = { Text("Enter your name") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "User", tint = AnimeSecondary)
                },
                trailingIcon = {
                    AnimatedCheckmark(visible = isNameValid)
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
                modifier = Modifier.fillMaxWidth().testTag("name_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Email Address
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                placeholder = { Text("you@domain.com") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = "Email", tint = AnimeSecondary)
                },
                trailingIcon = {
                    AnimatedCheckmark(visible = isEmailValid)
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
                modifier = Modifier.fillMaxWidth().testTag("email_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("At least 6 characters") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Password", tint = AnimeSecondary)
                },
                trailingIcon = {
                    Row(
                        modifier = Modifier.padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedCheckmark(visible = isPasswordValid)
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Password Toggle",
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                    focusedLabelColor = AnimeSecondary,
                    focusedBorderColor = AnimeSecondary,
                    cursorColor = AnimeAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("password_input")
            )

            // Password Strength Indicator Bar
            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Strength: ${passwordStrength.second}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = strengthColor
                        )
                        if (passwordStrength.second != "Strong") {
                            Text(
                                text = "Include capitals & digits",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(passwordStrength.first)
                                .fillMaxHeight()
                                .background(strengthColor, RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                placeholder = { Text("Re-enter password") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "ConfirmPassword", tint = AnimeSecondary)
                },
                trailingIcon = {
                    Row(
                        modifier = Modifier.padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedCheckmark(visible = isConfirmPasswordValid)
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Password Toggle",
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                    focusedLabelColor = AnimeSecondary,
                    focusedBorderColor = AnimeSecondary,
                    cursorColor = AnimeAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("confirm_password_input")
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Sign Up Gradient Button
            Button(
                onClick = { handleRegister() },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
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
                            text = "Sign Up",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Back to Login link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Already have an account?",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sign In",
                    color = AnimeSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedCheckmark(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success tick",
            tint = Color(0xFF43A047),
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}
