import {
    createApp, xml, startActivity, Icons, reactive, computed, ref, ModifierExtension
} from "vue-ui";
import { showToast } from "toast";
const { background, width, padding, height, fillMaxSize } = ModifierExtension

const modifier = [width(300), height(100), padding(20, 5)]
const modifier2 = [padding(7)]
const onClick = () => showToast('你点击了卡片')
const app = createApp({
    render() {
        return xml`
    <column>
        <card modifier=${modifier}>
            <text modifier=${modifier2}>默认卡片</text>
        </card>
        <card onClick=${onClick} modifier=${modifier}>
            <text modifier=${modifier2}>可点击卡片</text>
        </card>
        <card type="elevated" modifier=${modifier}>
            <text modifier=${modifier2}>elevated卡片</text>
        </card>
        <card type="outlined" modifier=${modifier}>
            <text modifier=${modifier2}>outlined卡片</text>
        </card>
        <card type="outlined"
                borderWidth=${1}
                borderColor=${0xfffcccaan}
               modifier=${modifier}>
            <text modifier=${modifier2}>带轮廓卡片</text>
        </card>
    </column>
        `
    }
})

startActivity(app)