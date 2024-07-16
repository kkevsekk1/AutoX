import {
    createApp, xml, startActivity, ModifierExtension
} from "vue-ui";
const { padding } = ModifierExtension

function onRender(state) {
    console.log("当前小时:", state.hour);
    console.log("当前分钟:", state.minute);
}

const modifier = [padding(20, 5)]
const modifier2 = [padding(7)]
const app = createApp({
    render() {
        return xml`
    <column>
        <card modifier=${modifier}>
            <TimePicker
             modifier=${modifier2}
             onRender=${onRender} initialTime="18:46" is24Hour=${false} />
        </card>
        <card modifier=${modifier}>
            <TimeInput
             modifier=${modifier2}
             onRender=${onRender} initialTime="13:46" is24Hour=${true} />
        </card>
    </column>
        `
    }
})

startActivity(app)