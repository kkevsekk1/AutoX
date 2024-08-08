import { showToast } from "toast";
import {
    createApp, xml, defineComponent, ModifierExtension,
    startActivity, ref, reactive, Icons
} from "vue-ui";
const { Menu, Home } = Icons.Default
const { background, width, height, weight, fillMaxSize } = ModifierExtension

const DrawerMeuns = [
    {
        title: "选项一",
        icon: "@drawable/ic_android_black_48dp"
    },
    {
        title: "选项二",
        icon: "@drawable/ic_settings_black_48dp"
    },
    {
        title: "选项三",
        icon: "@drawable/ic_favorite_black_48dp"
    },
    {
        title: "退出",
        icon: "@drawable/ic_exit_to_app_black_48dp"
    }
]
//定义侧边栏组件
const Drawer = defineComponent({
    render() {
        return xml`
    <ModalDrawerSheet>
        <column modifier=${[width(220)]}>
            <Image modifier=${[width(220), height(220)]} src="@drawable/autojs_logo" />
            ${DrawerMeuns.map((meun) => {
            return xml`
                    <NavigationDrawerItem 
                        onClick=${() => showToast(meun.title)}
                        icon=${meun.icon}
                        label=${meun.title}
                        />
                    
                `
        })}
        </column>
    </ModalDrawerSheet>
        `
    }
})

const meuns = reactive({
    expanded: ref(false),
    list: ["设置", "关于"],
    onClick(title) {
        showToast("你点击了:" + title)
        this.expanded = false;
    }
})
const TopAppBar = defineComponent({
    render() {
        return xml`
<TopAppBar title=${"界面模板"} >
    <template #navigationIcon>
        <IconButton onClick=${() => { drawer.open() }}>
            <Icon src=${Menu} />
        </IconButton>
    </template>
    <template #actions>
        <IconButton onClick=${() => { meuns.expanded = true }}>
            <Icon src=@drawable/ic_more_vert_black_48dp />
        </IconButton>
        <DropdownMenu onDismissRequest=${() => { meuns.expanded = false }} 
            expanded=${meuns.expanded}>
            ${meuns.list.map((v) => {
            return xml`
                <DropdownMenuItem 
                 onClick=${() => meuns.onClick(v)}
                 title=${v}
                 />
        `
        })
            }
        </DropdownMenu>
    </template>
</TopAppBar>
        `
    }
})

let drawer
let cu = ref(0)
let app = createApp({
    data() {
        return {
            opened: false,
        };
    },
    methods: {
    },
    render() {
        return xml` 
<ModalNavigationDrawer onRender=${(s) => { drawer = s }}>
    <template #drawerContent><${Drawer}/></template>
    <column modifier=${[fillMaxSize()]}>
        <${TopAppBar}/>
        <box modifier=${[background('theme'), fillMaxSize(), weight(1)]}>
            <text text="第 ${cu.value} 内容：" />
        </box>
        <NavigationBar>
            <item selected=${cu.value == 0} onClick=${() => cu.value = 0} icon=${Home}>主页</item>
            <item selected=${cu.value == 1} onClick=${() => cu.value = 1} icon=${Home}>管理</item>
            <item enabled=${false} icon=${Home}>文档</item>
        </NavigationBar>
    </column>
</ModalNavigationDrawer>
      `;
    },
});
// let data = app.mount("activit");

const activit = await startActivity(app)
