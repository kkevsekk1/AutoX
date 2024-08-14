import {
    createApp, xml, startActivity, Icons, reactive, computed, ref, ModifierExtension
} from "vue-ui";
import { showToast } from "toast";
const { Menu, Home, Add } = Icons.Default
const { background, width, padding, height,
    fillMaxSize, fillMaxWidth, verticalScroll } = ModifierExtension


const app = createApp({
    render() {
        return xml`
    <column verticalArrangement="space_between" modifier=${[fillMaxSize()]}>
        <row modifier=${[fillMaxWidth()]} horizontalArrangement="space_between" >
            <FloatingActionButton size="small" icon=${Add} />
            <FloatingActionButton >
                <Icon src=${Home} tint=${0xffcc7459n} />
            </FloatingActionButton>
            <FloatingActionButton size="large" icon=${Menu} />
        </row>
        <row modifier=${[fillMaxWidth()]} horizontalArrangement="space_between" >
            <ExtendedFloatingActionButton text="Extended Fab" icon=${Home} />
            <ExtendedFloatingActionButton text="Extended Fab" icon=${Home}>
                <template #icon>
                    <Icon src=${Add} tint=${0xffcc4433n} />
                </template>
                <template #text>
                    <text color=#cc99c1 >Extended Fab2</text>
                </template>
            </ExtendedFloatingActionButton>
        </row>
    </column>
            `
    }
})

startActivity(app)