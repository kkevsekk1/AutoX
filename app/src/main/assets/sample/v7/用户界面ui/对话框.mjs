import {
    createApp, xml, startActivity, Icons, reactive, computed, ref, ModifierExtension
} from "vue-ui";
import { showToast } from "toast";
const { background, width, padding, height, fillMaxWidth } = ModifierExtension


const show = ref(false)
const show2 = ref(false)

const app = createApp({
    render() {
        return xml`
    <column>
        <Button onClick=${() => { show.value = true }}>点击显示对话框</Button>
        <Button onClick=${() => { show2.value = true }}>点击打开第2个对话框</Button>
        ${show.value && xml`
            <Dialog onDismissRequest=${() => { show.value = false }}>
              <card>
                <column modifier=${[padding(20)]}>
                    <column modifier=${[height(120), width(300)]}>
                        对话框内容
                    </column>
                    <row modifier=${[fillMaxWidth()]} horizontalArrangement="end">
                        <Button onClick=${() => { show.value = false }} type="text">取消</Button>
                        <Button onClick=${() => { show.value = false }} type="text">确认</Button>
                    </row>
                </column>
               </card>
            </Dialog>
        `}
        ${show2.value && xml`
            <Dialog dismissOnBackPress=${false} 
                dismissOnClickOutside=${false} onDismissRequest=${() => { show2.value = false }}>
              <card>
                <column modifier=${[padding(20)]}>
                    <column modifier=${[height(120), width(300)]}>
                        不可通过返回键和点击外部取消的对话框
                    </column>
                    <row modifier=${[fillMaxWidth()]} horizontalArrangement="end">
                        <Button onClick=${() => { show2.value = false }} type="text">取消</Button>
                        <Button onClick=${() => { show2.value = false }} type="text">确认</Button>
                    </row>
                </column>
               </card>
            </Dialog>
        `}
    </column>
        `
    }
})

startActivity(app)