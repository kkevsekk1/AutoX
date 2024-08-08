import {
    createApp, xml, startActivity, defineComponent, ModifierExtension, Icons
} from "vue-ui";
const { padding, fillMaxSize, height, rotate, fillMaxWidth } = ModifierExtension
import { showToast } from "toast";
const { ArrowDropDown, Home, Add } = Icons.Default


const M1 = defineComponent({
    data() {
        return {
            meuns: ["菜单1", "菜单2", "菜单3"],
            show: false,
        }
    },
    methods: {
        click: function (meun) {
            this.show = false
            showToast("你点击了: " + meun)
        }
    },
    render() {
        return xml`
        <box>
            <Button onClick=${() => { this.show = true }} >弹出菜单</Button>
            <DropdownMenu onDismissRequest=${() => { this.show = false }} 
                expanded=${this.show}>
            ${this.meuns.map((v) => {
            return xml`
                <DropdownMenuItem 
                 onClick=${this.click.bind(this, v)}
                 title=${v}
                 />
                `
        })
            }
            </DropdownMenu>
        </box>
        `
    }
})
const M2 = defineComponent({
    data() {
        return {
            meuns: ["选项1", "选项2", "选项3"],
            show: false,
            value: "选项1"
        }
    },
    computed: {
        rotate() {
            if (this.show) {
                return 180
            } else return 0
        }
    },
    methods: {
        click: function (meun) {
            this.show = false
            this.value = meun
            showToast("你点击了: " + meun)
        }
    },
    render() {
        return xml`
        <ExposedDropdownMenuBox 
            expanded=${this.show}
            onExpandedChange=${(i) => { this.show = i }}>
            <OutlinedTextField 
                value=${this.value}
                readOnly=${true}
                singleLine=${true} >
                <template #trailingIcon>
                    <Icon modifier=${[rotate(this.rotate)]} src=${ArrowDropDown} /> 
                </template>
            </OutlinedTextField>
            <template #menu>
            ${this.meuns.map((v) => {
            return xml`
                    <DropdownMenuItem 
                     onClick=${this.click.bind(this, v)}
                     title=${v}
                     />
                    `
        })
            }
            </template>
        </ExposedDropdownMenuBox>
        `
    }
})

const app = createApp({
    render() {
        return xml`
<box modifier=${[fillMaxSize()]} contentAlignment="center">
    <column>
        <${M1}/>
        <${M2}/>
    </column>
</box>
        `
    }
})

startActivity(app)