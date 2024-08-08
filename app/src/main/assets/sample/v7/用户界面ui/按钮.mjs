import {
    createApp, xml, startActivity, Icons, defineComponent, ModifierExtension
} from "vue-ui";
const { Menu, Home } = Icons.Default
const { clickable } = ModifierExtension

const G = defineComponent({
    data() {
        return {
            items: [{
                title: "选项1",
                checked: false,
            }, {
                title: "选项2",
                checked: false,
            }, {
                title: "选项3",
                checked: false,
            }
            ],
            allChecked: false
        }
    },
    render() {
        const t = this.items.map((item) => {
            function click() {
                item.checked = !item.checked
            }
            return xml`
            <row modifier=${[clickable(click)]} verticalAlignment="center">
                ${item.title}
                <Switch checked=${item.checked} 
                    onCheckedChange=${(s) => { item.checked = s }} />
            </row>
            `
        })
        const switchAll = (r) => {
            if (r === undefined) {
                r = !this.allChecked
            }
            this.allChecked = r
            this.items.forEach((item) => { item.checked = r })
        }
        return xml`
        <column>
            <row modifier=${[clickable(switchAll)]} verticalAlignment="center">
                切换全部
                <Switch checked=${this.allChecked} 
                    onCheckedChange=${(s) => { switchAll(s) }} />
            </row >
            ${t}
        </column>
    `
    }
})

const app = createApp({
    render() {
        return xml`
    <column>
        <Button>普通按钮</Button>
        <Button type="text">text按钮</Button>
        <Button type="elevated">elevated按钮</Button>
        <Button type="outlined">outlined按钮</Button>
        <Button type="tonal">tonal按钮</Button>
        <IconButton><Icon src=${Home} /></IconButton>
        切换按钮
        <${G}/>
    </column>
    `
    }
})

startActivity(app)