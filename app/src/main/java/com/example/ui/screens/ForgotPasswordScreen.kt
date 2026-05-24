package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.FirebaseServiceHelper
import com.example.ui.theme.AnimeAccent
import com.example.ui.theme.AnimeDarkBg
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.AnimeSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateFlowOf("") }
    var isLoading by remember { mutableStateFlowOf(false) }
    var isSuccess by remember { mutableStateFlowOf(false) }
    var emailError by remember { mutableStateFlowOf<String?>(null) }

    val context = LocalContext.current

    fun handleResetPassword() {
        if (email.isEmpty()) {
            emailError = "Please enter your registered email"
            return
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Please enter a valid email address"
            return
        } else {
            emailError = null
        }

        isLoading = true
        FirebaseServiceHelper.sendPasswordReset(email.trim()) { success, error ->
            isLoading = false
            if (success) {
                isSuccess = true
            } else {
                Toast.makeText(context, error ?: "An error occurred. Try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AnimeDarkBg,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = AnimeDarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AnimeDarkBg),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isSuccess,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                }, label = "ForgotPasswordTransition"
            ) { successState ->
                if (successState) {
                    // Celebration Screen with animatable Custom Succes Canvas Animation
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        SuccessCelebrationView()

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = "Email Checked!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "We have sent a secure recovery link to:\n$email\nCheck your inbox to retrieve your password.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Return to Sign In", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Regular Input Form
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(AnimeAccent.copy(alpha = 0.12f), shape = RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email Envelop",
                                tint = AnimeAccent,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Lost Your Access?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Don't panic! Write your security account email address beneath, and we'll dispatch an encryption recovery link.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                if (emailError != null) emailError = null
                            },
                            label = { Text("Registered Email") },
                            placeholder = { Text("you@domain.com") },
                            singleLine = true,
                            isError = emailError != null,
                            supportingText = {
                                if (emailError != null) {
                                    Text(text = emailError!!, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedLabelColor = AnimePrimary,
                                focusedBorderColor = AnimePrimary,
                                cursorColor = AnimeAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { handleResetPassword() },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(colors = listOf(AnimePrimary, AnimeSecondary)),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        text = "Send Recovery Email",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuccessCelebrationView() {
    val checkProgress = remember { Animatable(0f) }
    val circleScale = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Run sequential animations
        circleScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
        checkProgress.animateTo(1f, animationSpec = tween(500, easing = LinearEasing))
    }

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2 * circleScale.value

            // 1. Draw elegant outer circle
            drawCircle(
                brush = Brush.linearGradient(colors = listOf(AnimePrimary, AnimeSecondary, AnimeAccent)),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // 2. Draw moving tick checkmark
            if (checkProgress.value > 0f) {
                val pathStroke = Stroke(width = strokeWidth + 1f, cap = StrokeCap.Round)
                val centX = size.width / 2f
                val centY = size.height / 2f

                val startX = centX - 22.dp.toPx()
                val startY = centY + 2.dp.toPx()

                val midX = centX - 6.dp.toPx()
                val midY = centY + 18.dp.toPx()

                val endX = centX + 24.dp.toPx()
                val endY = centY - 14.dp.toPx()

                // Calculate current path coordinates relative to animation progress
                val midProgress = 0.4f
                if (checkProgress.value <= midProgress) {
                    val subPercentage = checkProgress.value / midProgress
                    val pX = startX + (midX - startX) * subPercentage
                    val pY = startY + (midY - startY) * subPercentage
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(startX, startY),
                        end = androidx.compose.ui.geometry.Offset(pX, pY),
                        strokeWidth = pathStroke.width,
                        cap = pathStroke.cap
                    )
                } else {
                    val subPercentage = (checkProgress.value - midProgress) / (1f - midProgress)
                    val pX = midX + (endX - midX) * subPercentage
                    val pY = midY + (endY - midY) * subPercentage
                    
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(startX, startY),
                        end = androidx.compose.ui.geometry.Offset(midX, midY),
                        strokeWidth = pathStroke.width,
                        cap = pathStroke.cap
                    )
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(midX, midY),
                        end = androidx.compose.ui.geometry.Offset(pX, pY),
                        strokeWidth = pathStroke.width,
                        cap = pathStroke.cap
                    )
                }
            }
        }
    }
}
