import {
    createApp, xml, defineComponent, startActivity, Icons, reactive, computed, ref, ModifierExtension
} from "vue-ui";
import { showToast } from "toast";
const { Menu, Search, Add, Star } = Icons.Default
const { background, width, padding, height,
    fillMaxSize, fillMaxWidth, verticalScroll } = ModifierExtension


const BottomSheetScaffold = defineComponent({
    data() {
        return {
            snackbarHostState: null,
            bottomSheetState: null,
        }
    },
    render() {
        return xml`
    <BottomSheetScaffold 
        onRender=${({ snackbarHostState, bottomSheetState }) => {
                this.bottomSheetState = bottomSheetState;
                this.snackbarHostState = snackbarHostState;
            }}
        sheetSwipeEnabled=${true}
    >
        <template #sheetContent>
            <column modifier=${[height(300)]}>
            BottomSheet内容
            </column>
        </template>
        <template #topBar>
            <TopAppBar title=${"TopAppBar"} >
                <template #navigationIcon>
                    <IconButton><Icon src=${Menu} /></IconButton>
                </template>
            </TopAppBar>
        </template>
        主页内容
    </BottomSheetScaffold>
    `
    }
})
const ModalBottomSheet = defineComponent({
    data() {
        return {
            open: false
        }
    },
    render() {
        return xml`
    <box>
    ${this.open && xml`
    <ModalBottomSheet onDismissRequest=${() => { this.open = false }}>
        <column modifier=${[height(300)]}>BottomSheet内容</column>
    </ModalBottomSheet>`}
    <Button onClick=${() => { this.open = true }}>打开BottomSheet</Button>
    </box>
    `
    }
})
function start(component) {
    startActivity(createApp(component));
}
const app = createApp({
    render() {
        return xml`
        <column>
            <Button onClick=${() => { start(BottomSheetScaffold) }}>BottomSheetScaffold</Button>
            <Button onClick=${() => { start(ModalBottomSheet) }}>ModalBottomSheet</Button>
        </column>
            `
    }
})

startActivity(app)