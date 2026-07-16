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

/**
 * 字体类型下拉选择器。状态全部上提，仅负责渲染与回调上抛。
 *
 * @param selectedType 当前选中的字体索引（对应 [FontType.entries] 下标）。
 * @param onTypeSelected 用户选择某字体时回调，参数为新的字体索引。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontTypeSelector(
    selectedType: Int, onTypeSelected: (Int) -> Unit, modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedFont = FontType.entries[selectedType]

    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier
    ) {
        TextField(
            value = selectedFont.displayName,
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
                        onTypeSelected(index)
                        expanded = false
                    })
            }
        }
    }
}
