import {
    createApp, xml, startActivity, Icons, reactive, computed, ref, ModifierExtension
} from "vue-ui";
import { showToast } from "toast";
const { fillMaxWidth } = ModifierExtension


const app = createApp({
    render() {
        return xml`
    <column>
        普通文本
        <text fontSize=${40} >字体大小</text>
        <text fontWeight=${800} >加粗</text>
        <text fontStyle=italic >斜体</text>
        <text color=#cc99c1 >字体颜色</text>
        <text textDecoration=underline >下划线</text>
        <text  maxLines=${3} minLines=${2} text=${`
            使用 Material Design 3（新一代 Material Design）组件构建 Jetpack Compose UI。Material 3 中包括了更新后的主题和组件，以及动态配色等 Material You 个性化功能，旨在与新的 Android 12 视觉风格和系统界面相得益彰。
            `} />
        <text modifier=${[fillMaxWidth()]} textAlign="center" >居中字体</text>
    </column>
        `
    }
})

startActivity(app)