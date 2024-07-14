import {
    createApp, xml, startActivity, Icons, reactive, computed, ref
} from "vue-ui";
import { showToast } from "toast";

const slider = ref(0)
const slider2 = reactive([5, 33])
const app = createApp({
    render() {
        return xml`
    <column>
        圆形进度条
        <ProgressIndicator type="circular" indeterminate=${true} />
        直线进度条
        <ProgressIndicator type="linear" indeterminate=${true} />
        进度值-范围为 0-1
        <ProgressIndicator type="linear" progress=${0.6} />
        滑块
        ${"滑块当前值：" + slider.value}
        <Slider value=${slider.value}
            onValueChangeFinished=${() => showToast("你滑到的值为：" + slider.value)}
            onValueChange=${(s) => { slider.value = s }} steps=${0} min=${0} max=${100} />
        <Button onClick=${() => { slider.value = 99 }}>点击拉满滑块</Button>
        范围滑块
        当前范围：
        ${"min:" + slider2[0]}
        ${"max:" + slider2[1]}
        <RangeSlider minValue=${slider2[0]} maxValue=${slider2[1]}
            min=${0} max=${50} onValueChange=${(min, max) => {
                slider2[0] = min;
                slider2[1] = max
            }}
        />
    </column>
        `
    }
})

startActivity(app)