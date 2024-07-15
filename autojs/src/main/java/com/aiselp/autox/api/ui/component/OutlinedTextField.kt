package com.aiselp.autox.api.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render
import com.aiselp.autox.api.ui.component.TextField.parseKeyboardOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

object OutlinedTextField:VueNativeComponent {
    override val tag: String = "OutlinedTextField"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val value = remember { mutableStateOf("") }
        LaunchedEffect(element.props["value"]) {
            delay(100)
            yield()
            value.value = element.props["value"] as? String ?: ""
        }
        val onValueChange = element.getEvent("onValueChange")
        val enabled = element.props["enabled"] as? Boolean
        val readOnly = element.props["readOnly"] as? Boolean
        val label = element.findTemplate("label") ?: run {
            element.props["label"]?.let {
                ComposeElement("text").apply {
                    props["text"] = it
                }
            }
        }
        val keyboardOptions = parseKeyboardOptions(element)
        val isError = element.props["isError"] as? Boolean
        val singleLine = element.props["singleLine"] as? Boolean
        val placeholder = element.findTemplate("placeholder") ?: run {
            element.props["placeholder"]?.let {
                ComposeElement("text").apply {
                    props["text"] = it
                }
            }
        }
        val supportingText = element.findTemplate("supportingText") ?: run {
            element.props["supportingText"]?.let {
                ComposeElement("text").apply {
                    props["text"] = it
                }
            }
        }
        val leadingIcon = element.findTemplate("leadingIcon") ?: run {
            element.props["leadingIcon"]?.let {
                ComposeElement("Icon").apply {
                    props["src"] = it
                    props["tint"] = MaterialTheme.colorScheme.primary
                }
            }
        }
        KeyboardOptions()
        val trailingIcon = element.findTemplate("trailingIcon") ?: run {
            element.props["trailingIcon"]?.let {
                ComposeElement("Icon").apply {
                    props["src"] = it
                    props["tint"] = MaterialTheme.colorScheme.primary
                }
            }
        }
        val prefix = element.findTemplate("prefix") ?: run {
            element.props["prefix"]?.let {
                ComposeElement("text").apply {
                    props["text"] = it
                }
            }
        }
        val suffix = element.findTemplate("suffix") ?: run {
            element.props["suffix"]?.let {
                ComposeElement("text").apply {
                    props["text"] = it
                }
            }
        }
        val visualTransformation = if (element.props["keyboardType"] == "password") {
            PasswordVisualTransformation()
        } else VisualTransformation.None
        val maxLines = element.props["maxLines"] as? Int
        val minLines = element.props["minLines"] as? Int
        OutlinedTextField(
            value = value.value,
            onValueChange = { value.value = it;onValueChange?.invoke(it) },
            modifier = modifier,
            enabled = enabled ?: true,
            readOnly = readOnly ?: false,
            leadingIcon = leadingIcon?.let { { it.Render() } },
            trailingIcon = trailingIcon?.let { { it.Render() } },
            label = label?.let { { it.Render() } },
            prefix = prefix?.let { { it.Render() } },
            suffix = suffix?.let { { it.Render() } },
            visualTransformation = visualTransformation,
            placeholder = placeholder?.let { { it.Render() } },
            supportingText = supportingText?.let { { it.Render() } },
            isError = isError ?: false,
            singleLine = singleLine ?: false,
            keyboardOptions = keyboardOptions,
            maxLines = maxLines ?: if (singleLine == true) 1 else Int.MAX_VALUE,
            minLines = minLines ?: 1,
        )
    }

}