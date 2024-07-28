import {
    createApp, xml, startActivity, Theme, Icons, reactive, computed, shallowRef, ModifierExtension
} from "vue-ui";
import { showToast } from "toast";
const { background, width, padding, height, fillMaxSize } = ModifierExtension
/**
 * MaterialTheme组件提供主题颜色配置，暂不支持配置排版typography和形状shapes
 */
const colorScheme1 = Theme.lightColorScheme({
    primary: 0xFF006878n,
    surface: 0xFF83D2E5n,
    background: 0xFFF5FAFCn
})
const colorScheme2 = Theme.lightColorScheme({
    primary: 0xFF88D6E9n,
    surface: 0xFF287F90n,
    background: 0xFFF5FAECn
})
//colorScheme是一个java对象，不能使用ref储存，否则传递给组件将无法还原成java对象
const current = shallowRef(colorScheme1)

const app = createApp({
    render() {
        return xml`
<MaterialTheme 
    lightColorScheme=${current.value}
    darkColorScheme=${current.value}
    dynamicColor=${false/*动态颜色，指定为true并且设备支持将使用壁纸采集的颜色*/}
    dark=${false/*暗色模式，不设置将根据系统模式来选择 */}
    >
    <column modifier=${[background("theme"), fillMaxSize()]}>
        <Button onClick=${() => { current.value = colorScheme1 }}>主题1</Button>
        <Button onClick=${() => { current.value = colorScheme2 }}>主题2</Button>
        
    </column>
</MaterialTheme>
    `
    }
})

startActivity(app)