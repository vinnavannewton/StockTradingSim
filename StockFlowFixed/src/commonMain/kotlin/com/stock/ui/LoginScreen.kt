package com.stock.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stock.api.SupabaseManager
import kotlinx.coroutines.launch

// ── Colour tokens ────────────────────────────────────────────────────────────
private val Bg          = Color(0xFF07090F)
private val Surf1       = Color(0xFF0F172A)
private val Surf2       = Color(0xFF1E293B)
private val Accent      = Color(0xFF38BDF8)
private val AccentDark  = Color(0xFF0284C7)
private val Good        = Color(0xFF4ADE80)
private val Bad         = Color(0xFFF87171)
private val Text1       = Color(0xFFF8FAFC)
private val Text2       = Color(0xFF94A3B8)
private val Border      = Color(0xFF334155)
private val GlassWhite  = Color(0x0DFFFFFF)

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope        = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    var isSignUp    by remember { mutableStateOf(false) }
    var isLoading   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }
    var successMsg  by remember { mutableStateOf("") }

    // Animated pulse for the logo orb
    val pulse = rememberInfiniteTransition()
    val orbScale by pulse.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0F2942), Bg),
                        radius = 1200f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Background decorative blobs
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .offset(x = (-80).dp, y = (-160).dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.06f))
                    .blur(80.dp)
            )
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .offset(x = 100.dp, y = 200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF818CF8).copy(alpha = 0.05f))
                    .blur(60.dp)
            )

            Column(
                modifier = Modifier
                    .width(400.dp)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Logo ─────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size((68 * orbScale).dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(listOf(Accent, Color(0xFF818CF8)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "StockFlow",
                    color = Text1,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "Next-Gen Trading Simulator",
                    color = Text2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(Modifier.height(32.dp))

                // ── Glass card ────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF111827).copy(alpha = 0.95f), Surf1)
                            )
                        )
                        .border(1.dp, Border.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tab selector: Log In / Sign Up
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surf2),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        listOf(false to "Log In", true to "Sign Up").forEach { (su, label) ->
                            val selected = isSignUp == su
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected)
                                            Brush.horizontalGradient(listOf(Accent, Color(0xFF818CF8)))
                                        else
                                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        if (!isLoading) {
                                            isSignUp = su
                                            errorMsg = ""
                                            successMsg = ""
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (selected) Color.White else Text2,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Email field
                    LoginField(
                        value = email,
                        onValueChange = { email = it; errorMsg = ""; successMsg = "" },
                        label = "Email address",
                        leadingIcon = {
                            Icon(Icons.Default.Email, null, tint = if (email.isNotEmpty()) Accent else Text2, modifier = Modifier.size(20.dp))
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    // Password field
                    LoginField(
                        value = password,
                        onValueChange = { password = it; errorMsg = ""; successMsg = "" },
                        label = "Password",
                        leadingIcon = {
                            Icon(Icons.Default.Lock, null, tint = if (password.isNotEmpty()) Accent else Text2, modifier = Modifier.size(20.dp))
                        },
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPass) "Hide password" else "Show password",
                                    tint = Text2,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    // Error / Success message
                    AnimatedVisibility(
                        visible = errorMsg.isNotEmpty() || successMsg.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val isError = errorMsg.isNotEmpty()
                        val msg = if (isError) errorMsg else successMsg
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isError) Bad.copy(alpha = 0.12f) else Good.copy(alpha = 0.12f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isError) Bad else Good)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                msg,
                                color = if (isError) Bad else Good,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Main action button
                    Button(
                        onClick = {
                            val trimmedEmail = email.trim()
                            if (trimmedEmail.isBlank() || password.isBlank()) {
                                errorMsg = "Please fill in all fields"
                                return@Button
                            }
                            if (!trimmedEmail.contains("@")) {
                                errorMsg = "Please enter a valid email"
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMsg = "Password must be at least 6 characters"
                                return@Button
                            }
                            scope.launch {
                                isLoading = true
                                errorMsg  = ""
                                successMsg = ""
                                focusManager.clearFocus()

                                val result = if (isSignUp)
                                    SupabaseManager.signUp(trimmedEmail, password)
                                else
                                    SupabaseManager.signIn(trimmedEmail, password)

                                result.fold(
                                    onSuccess = {
                                        if (isSignUp) {
                                            // After sign-up Supabase auto-confirms (email confirm disabled)
                                            // Try to sign in right away
                                            val loginResult = SupabaseManager.signIn(trimmedEmail, password)
                                            loginResult.fold(
                                                onSuccess = { onLoginSuccess() },
                                                onFailure = {
                                                    successMsg = "Account created! Please log in."
                                                    isSignUp = false
                                                }
                                            )
                                        } else {
                                            onLoginSuccess()
                                        }
                                    },
                                    onFailure = { e ->
                                        errorMsg = when {
                                            e.message?.contains("Invalid login", ignoreCase = true) == true ->
                                                "Incorrect email or password"
                                            e.message?.contains("already registered", ignoreCase = true) == true ->
                                                "This email is already registered — try logging in"
                                            e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                                                "Please confirm your email first"
                                            e.message?.contains("network", ignoreCase = true) == true ->
                                                "Network error — check your connection"
                                            else ->
                                                "Something went wrong. Try again."
                                        }
                                    }
                                )
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent,
                            disabledBackgroundColor = Color.Transparent
                        ),
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (!isLoading)
                                        Brush.horizontalGradient(listOf(Accent, Color(0xFF818CF8)))
                                    else
                                        Brush.horizontalGradient(listOf(Surf2, Surf2)),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Accent,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    if (isSignUp) "Create Account" else "Sign In",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Footer stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip("$1M", "Starting Balance")
                    Box(Modifier.size(4.dp).clip(CircleShape).background(Border))
                    StatChip("Real-Time", "Market Data")
                    Box(Modifier.size(4.dp).clip(CircleShape).background(Border))
                    StatChip("Cloud", "Sync")
                }
            }
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Text2, fontSize = 13.sp) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = Text1,
            cursorColor = Accent,
            focusedBorderColor = Accent.copy(alpha = 0.8f),
            unfocusedBorderColor = Border,
            backgroundColor = Surf2.copy(alpha = 0.5f),
            focusedLabelColor = Accent,
            unfocusedLabelColor = Text2
        )
    )
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Text2, fontSize = 11.sp)
    }
}
