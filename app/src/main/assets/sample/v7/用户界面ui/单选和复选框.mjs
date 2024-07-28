import {
    createApp, xml, startActivity, Icons, reactive, computed, ref
} from "vue-ui";

const s = reactive([false, false, false])
const status = computed(() => {
    let n = null
    for (let r of s) {
        if (n == null) n = r
        if (n !== r) return "indeterminate"
    }
    if (n) {
        return "on"
    } else return "off"
})
function clickAll() {
    let n = false
    if (status.value === "on") {
        n = false
    } else if (status.value === "off") {
        n = true
    }
    for (let i = 0; i < s.length; i++) {
        s[i] = n
    }
}

const radio = ref(1)
const app = createApp({
    render() {
        return xml`
    <column>
        <row verticalAlignment="center">选择全部<Checkbox status=${status.value} onClick=${clickAll} /> </row>
        <row verticalAlignment="center">复选框1 <Checkbox checked=${s[0]} 
            onCheckedChange=${(r) => { s[0] = r }} /></row>
        <row verticalAlignment="center">复选框2 <Checkbox checked=${s[1]} 
            onCheckedChange=${(r) => { s[1] = r }} /></row>
        <row verticalAlignment="center">复选框3 <Checkbox checked=${s[2]} 
            onCheckedChange=${(r) => { s[2] = r }} /></row>
        <Divider type="horizontal"/>
        <row verticalAlignment="center">
           单选框1  <RadioButton selected=${radio.value == 1} onClick=${() => { radio.value = 1 }} />
        </row>
        <row verticalAlignment="center">
           单选框2  <RadioButton selected=${radio.value == 2} onClick=${() => { radio.value = 2 }} />
        </row>
        <row verticalAlignment="center">
           单选框3  <RadioButton selected=${radio.value == 3} onClick=${() => { radio.value = 3 }} />
        </row>
    </column>
        `
    }
})

startActivity(app)