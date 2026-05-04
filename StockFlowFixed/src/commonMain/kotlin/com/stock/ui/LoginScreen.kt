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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    // Animated pulse for the logo orb
    val pulse = rememberInfiniteTransition(label = "logoPulse")
    val orbScale by pulse.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = EaseInOutSine), RepeatMode.Reverse
        ),
        label = "orbScale"
    )
    val orbGlow by pulse.animateFloat(
        initialValue = 0.15f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = EaseInOutSine), RepeatMode.Reverse
        ),
        label = "orbGlow"
    )

    // Floating particles animation
    val particleTransition = rememberInfiniteTransition(label = "particles")
    val particleY1 by particleTransition.animateFloat(
        initialValue = -20f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(4000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "py1"
    )
    val particleY2 by particleTransition.animateFloat(
        initialValue = 15f, targetValue = -15f,
        animationSpec = infiniteRepeatable(tween(3500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "py2"
    )

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SFGradient.bgRadial),
            contentAlignment = Alignment.Center
        ) {
            // ── Decorative background orbs ────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .offset(x = (-120).dp, y = (-200).dp)
                    .graphicsLayer { translationY = particleY1 }
                    .clip(CircleShape)
                    .background(SFColor.Accent.copy(alpha = 0.05f))
                    .blur(100.dp)
            )
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = 140.dp, y = 260.dp)
                    .graphicsLayer { translationY = particleY2 }
                    .clip(CircleShape)
                    .background(SFColor.Indigo.copy(alpha = 0.04f))
                    .blur(80.dp)
            )
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .offset(x = 180.dp, y = (-100).dp)
                    .graphicsLayer { translationY = particleY1 * 0.5f }
                    .clip(CircleShape)
                    .background(SFColor.Gain.copy(alpha = 0.03f))
                    .blur(60.dp)
            )

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 40 },
            ) {
                Column(
                    modifier = Modifier
                        .width(420.dp)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Animated Logo ──────────────────────────────────────────
                    Box(contentAlignment = Alignment.Center) {
                        // Outer glow ring
                        Box(
                            modifier = Modifier
                                .size((90 * orbScale).dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(SFColor.Accent.copy(alpha = orbGlow * 0.3f))
                                .blur(20.dp)
                        )
                        // Main logo
                        Box(
                            modifier = Modifier
                                .size((72 * orbScale).dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SFGradient.accentBrand),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "StockFlow",
                        color = SFColor.TextPrimary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.8).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Next-Gen Trading Simulator",
                        color = SFColor.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(Modifier.height(36.dp))

                    // ── Glass card ─────────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SFShape.XXLarge)
                            .background(SFGradient.loginCard)
                            .border(1.dp, SFColor.Border.copy(alpha = 0.6f), SFShape.XXLarge)
                            .padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Tab selector: Log In / Sign Up
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(SFShape.Medium)
                                .background(SFColor.Surface2),
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
                                                SFGradient.accentBrand
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
                                        .padding(vertical = 13.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (selected) Color.White else SFColor.TextSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.3.sp
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
                                Icon(
                                    Icons.Default.Email, null,
                                    tint = if (email.isNotEmpty()) SFColor.Accent else SFColor.TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
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
                                Icon(
                                    Icons.Default.Lock, null,
                                    tint = if (password.isNotEmpty()) SFColor.Accent else SFColor.TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            visualTransformation = if (showPass) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
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
                                        if (showPass) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = if (showPass) "Hide" else "Show",
                                        tint = SFColor.TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )

                        // Error / Success message
                        AnimatedVisibility(
                            visible = errorMsg.isNotEmpty() || successMsg.isNotEmpty(),
                            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                        ) {
                            val isError = errorMsg.isNotEmpty()
                            val msg = if (isError) errorMsg else successMsg
                            val bgColor = if (isError) SFColor.Loss else SFColor.Gain
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(SFShape.Medium)
                                    .background(bgColor.copy(alpha = 0.10f))
                                    .border(1.dp, bgColor.copy(alpha = 0.2f), SFShape.Medium)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(bgColor)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    msg,
                                    color = bgColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp
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
                            modifier = Modifier.fillMaxWidth().height(54.dp),
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
                                        if (!isLoading) SFGradient.accentBrand
                                        else Brush.horizontalGradient(
                                            listOf(SFColor.Surface2, SFColor.Surface2)
                                        ),
                                        RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = SFColor.Accent,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        if (isSignUp) "Create Account" else "Sign In",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        // Divider
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.weight(1f).height(1.dp).background(SFColor.Border))
                            Text(
                                "OR",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = SFColor.TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            Box(Modifier.weight(1f).height(1.dp).background(SFColor.Border))
                        }

                        // Google Login Button
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMsg = ""
                                    successMsg = ""
                                    focusManager.clearFocus()

                                    val result = SupabaseManager.signInWithGoogle()
                                    result.fold(
                                        onSuccess = { onLoginSuccess() },
                                        onFailure = { e ->
                                            e.printStackTrace()
                                            errorMsg = "Google Sign-In failed: ${e.message}"
                                        }
                                    )
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = SFColor.Surface2,
                                disabledBackgroundColor = SFColor.Surface2.copy(alpha = 0.5f)
                            ),
                            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "G",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Continue with Google",
                                    color = SFColor.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Footer stats
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FooterStatChip("$1M", "Starting Balance")
                        DotSeparator()
                        FooterStatChip("Real-Time", "Market Data")
                        DotSeparator()
                        FooterStatChip("Cloud", "Sync")
                    }
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
        label = { Text(label, color = SFColor.TextMuted, fontSize = 13.sp) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = SFColor.TextPrimary,
            cursorColor = SFColor.Accent,
            focusedBorderColor = SFColor.Accent.copy(alpha = 0.7f),
            unfocusedBorderColor = SFColor.Border,
            backgroundColor = SFColor.Surface2.copy(alpha = 0.5f),
            focusedLabelColor = SFColor.Accent,
            unfocusedLabelColor = SFColor.TextMuted
        )
    )
}

@Composable
private fun FooterStatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = SFColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = SFColor.TextMuted, fontSize = 11.sp)
    }
}
