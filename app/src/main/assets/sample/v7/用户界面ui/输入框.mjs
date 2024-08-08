import {
    createApp, xml, startActivity, Icons, reactive, computed, ref, ModifierExtension
} from "vue-ui";
import { showToast } from "toast";
const { Menu, Home, Add } = Icons.Default
const { background, width, padding, height,
    fillMaxSize, verticalScroll } = ModifierExtension

const input = ref("")
function updateInput(s) {
    input.value = s
}
const modifier = [fillMaxSize(), padding(5, 0), verticalScroll()]

const app = createApp({
    render() {
        return xml`
    <column modifier=${modifier}>
        输入框
        <TextField value=${input.value} onValueChange=${updateInput} />
        Outlined样式
        <OutlinedTextField value=${input.value} onValueChange=${updateInput} />
        密码输入,包括（number，email，url，password，phone，numberPassword）
        <TextField keyboardType="password" value=${input.value} onValueChange=${updateInput} />
        单行输入
        <TextField singleLine=${true} value=${input.value} onValueChange=${updateInput} />
        带label
        <TextField label="label" value=${input.value} onValueChange=${updateInput} />
        带提示输入框
        <TextField value=${input.value} onValueChange=${updateInput} placeholder="输入一些内容" />
        限制行数
        <TextField minLines=${2} maxLines=${5} value=${input.value} onValueChange=${updateInput} />
        输入错误
        <TextField value=${input.value} onValueChange=${updateInput} isError=${true} />
        带前缀和后缀
        <TextField prefix="https://" suffix=".com" value=${input.value} onValueChange=${updateInput} />
        只读和图标
        <TextField readOnly=${true} value=${input.value} onValueChange=${updateInput} >
            <template #leadingIcon><Icon src=${Home} /> </template>
            <template #trailingIcon><Icon src=${Add} /> </template>
        </TextField>
    </column>
        `
    }
})

startActivity(app)