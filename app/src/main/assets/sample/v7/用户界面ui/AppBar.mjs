import {
    createApp, xml, defineComponent, startActivity, Icons, reactive, computed, ref, ModifierExtension
} from "vue-ui";
import { showToast } from "toast";
const { Menu, Search, Add, Star } = Icons.Default
const { background, width, padding, height,
    fillMaxSize, fillMaxWidth, verticalScroll } = ModifierExtension

const TopAppBar = defineComponent({
    render() {
        return xml`
    <TopAppBar title=${"TopAppBar"} >
        <template #navigationIcon>
            <IconButton>
                <Icon src=${Menu} />
            </IconButton>
        </template>
        <template #actions>
            <IconButton>
                <Icon src=@drawable/ic_more_vert_black_48dp />
            </IconButton>
        </template>
    </TopAppBar>
            `
    }
})

const BottomAppBar = defineComponent({
    render() {
        return xml`
<column verticalArrangement=end modifier=${[fillMaxSize()]}>
    <BottomAppBar>
        <template #floatingActionButton>
            <FloatingActionButton size="small" icon=${Add} />
        </template>
        <template #actions>
            <IconButton>
                <Icon src=${Menu} />
            </IconButton>
            <IconButton>
                <Icon src=${Search} />
            </IconButton>
            <IconButton>
                <Icon src=${Star} />
            </IconButton>
        </template>
    </BottomAppBar>
</column>
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
        <Button onClick=${() => { start(TopAppBar) }}>TopAppbar</Button>
        <Button onClick=${() => { start(BottomAppBar) }}>BottomAppBar</Button>
    </column>
        `
    }
})

startActivity(app)