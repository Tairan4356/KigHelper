package com.ziegler.kighelper.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ziegler.kighelper.ui.theme.FontType
import com.ziegler.kighelper.utils.InstalledFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontTypeSelector(
    selectedType: Int,
    onTypeSelected: (Int, String?) -> Unit,
    installedFonts: List<InstalledFont>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val builtinCount = FontType.entries.size
    val displayText = if (selectedType < builtinCount) {
        FontType.entries[selectedType].displayName
    } else {
        val customIndex = selectedType - builtinCount
        if (customIndex < installedFonts.size) {
            installedFonts[customIndex].displayName
        } else {
            FontType.entries[0].displayName
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier
    ) {
        TextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded, onDismissRequest = { expanded = false }) {
            FontType.entries.forEachIndexed { index, fontType ->
                SettingDropdownMenuItem(
                    text = fontType.displayName, onClick = {
                        onTypeSelected(index, null)
                        expanded = false
                    })
            }
            if (installedFonts.isNotEmpty()) {
                installedFonts.forEachIndexed { index, font ->
                    SettingDropdownMenuItem(
                        text = font.displayName, onClick = {
                            onTypeSelected(builtinCount + index, font.baseName)
                            expanded = false
                        })
                }
            }
        }
    }
}
