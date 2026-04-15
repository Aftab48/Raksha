package com.raksha.app.feature_login_register.presentation.auth.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.raksha.app.ui.theme.ColorBorder
import com.raksha.app.ui.theme.ColorDanger
import com.raksha.app.ui.theme.ColorPrimary
import com.raksha.app.ui.theme.ColorSafe
import com.raksha.app.ui.theme.ColorSurface
import com.raksha.app.ui.theme.ColorSurfaceElevated
import com.raksha.app.ui.theme.ColorTextPrimary
import com.raksha.app.ui.theme.ColorTextSecondary
import com.raksha.app.ui.theme.RakshaTypography

@Composable
fun AuthScreenContainer(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            color = ColorSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = {
                    Text(text = title, style = RakshaTypography.headlineLarge)
                    Text(
                        text = subtitle,
                        style = RakshaTypography.bodyMedium.copy(color = ColorTextSecondary)
                    )
                    content()
                }
            )
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ColorPrimary,
            unfocusedBorderColor = ColorBorder,
            focusedContainerColor = ColorSurfaceElevated,
            unfocusedContainerColor = ColorSurfaceElevated,
            focusedTextColor = ColorTextPrimary,
            unfocusedTextColor = ColorTextPrimary,
            focusedLabelColor = ColorPrimary,
            unfocusedLabelColor = ColorTextSecondary,
            cursorColor = ColorPrimary,
            errorBorderColor = ColorDanger,
            errorLabelColor = ColorDanger,
            errorTextColor = ColorTextPrimary
        )
    )
}

@Composable
fun AuthStatusBanner(
    text: String,
    isError: Boolean
) {
    val contentColor = if (isError) ColorDanger else ColorSafe
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurfaceElevated, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            style = RakshaTypography.bodyMedium
        )
    }
}

@Composable
fun AuthPrimaryButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.height(18.dp),
                color = ColorSurface
            )
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                color = ColorSurface,
                style = RakshaTypography.bodyLarge
            )
        }
    }
}

@Composable
fun AuthFooterText(
    prompt: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Text(
        text = "$prompt $actionLabel",
        style = RakshaTypography.bodyMedium.copy(color = ColorTextSecondary),
        modifier = Modifier.clickable(onClick = onActionClick)
    )
}
