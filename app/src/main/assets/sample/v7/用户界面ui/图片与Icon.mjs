import {
    createApp, xml, startActivity, Icons, reactive, computed, ref, ModifierExtension
} from "vue-ui";
import { showToast } from "toast";
const { Menu, Home, Add } = Icons.Default
const { background, width, padding, height,
    fillMaxSize, verticalScroll } = ModifierExtension

const modifier = [fillMaxSize(), padding(5), verticalScroll(), background("theme")]
const ImgModifier = [width(100), height(100)]

const app = createApp({
    render() {
        return xml`
    <column modifier=${modifier}>
        Icon
        <Icon modifier=${ImgModifier} src=${Home} />
        带颜色的Icon
        <Icon modifier=${ImgModifier} src=${Add} tint=${0xffcc4433n} />
        支持@drawer/ 中的资源
        <Icon modifier=${ImgModifier} src="@drawable/ic_ali_close" />
        
        要加载本地或网络uri图片请使用Image
        <Image modifier=${ImgModifier} src="http://doc.autoxjs.com/images/logo.png" />
        Image还支持alpha和alignment，但不支持tint
        <Image alignment="top_end" alpha=${0.5} modifier=${ImgModifier} src="@drawable/ic_ali_close" />
     </column>
        `
    }
})

startActivity(app)