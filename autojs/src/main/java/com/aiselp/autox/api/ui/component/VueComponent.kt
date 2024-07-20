package com.aiselp.autox.api.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

interface VueNativeComponent {
    val tag: String

    @Composable
    fun Render(modifier: Modifier, element: ComposeElement, content: @Composable () -> Unit): Unit
}

object VueComponent {
    val map = buildMap<String, VueNativeComponent> {
        fun add(component: VueNativeComponent) {
            put(component.tag, component)
        }
        add(Template)
        add(Card)
        add(Column)
        add(Row)
        add(Text)
        add(TopAppBar)
        add(Box)
        add(Button)
        add(Scaffold)
        add(Image)
        add(TextField)
        add(IconButton)
        add(Icon)
        add(Switch)
        add(Checkbox)
        add(Fab)
        add(DropdownMenu)
        add(DropdownMenuItem)
        add(View)
        add(ModalNavigationDrawer)
        add(ModalDrawerSheet)
        add(NavigationBar)
        add(NavigationDrawerItem)
        add(RadioButton)
        add(Divider)
        add(ProgressIndicator)
        add(Slider)
        add(RangeSlider)
        add(FlowColumn)
        add(FlowRow)
        add(OutlinedTextField)
        add(TimePicker)
        add(TimeInput)
        add(Dialog)
        add(DatePicker)
        add(DateRangePicker)
        add(FloatingActionButton)
        add(ExtendedFloatingActionButton)
        add(BottomAppBar)
        add(ExposedDropdownMenuBox)
    }
}