import {
    createApp, xml, startActivity, Icons, reactive, computed, ref, ModifierExtension
} from "vue-ui";
import { showToast } from "toast";
const { background, width, padding, height, fillMaxSize } = ModifierExtension
const { Person, Done, Close, Settings } = Icons.Default

const selected = ref(false)
function onClick() {
    selected.value = !selected.value
}
const app = createApp({
    render() {
        return xml`
    <column>
        <Chip type="assist" onClick=${onClick} leadingIcon=${Settings} label="Assist" />
        <Chip type="filter" onClick=${onClick} selected=${selected.value}
             leadingIcon=${selected.value ? Done : null} label="Filter" />
        <Chip type="input" onClick=${onClick} selected=${selected.value}
             avatar=${Person} label="Input" />
        <Chip type="suggestion" onClick=${onClick} label="Suggestion" />

        <Chip type="assist" style="elevated" leadingIcon=${Settings}
            onClick=${onClick} label="Assist" />
        <Chip type="filter" style="elevated" selected=${selected.value}
            leadingIcon=${selected.value ? Done : null} 
            onClick=${onClick} label="Filter" />
        <Chip type="suggestion" style="elevated" onClick=${onClick} label="Suggestion" />
        <Chip type="input" onClick=${onClick} selected=${selected.value}>
             <template #label>
                <text color=#cc99c9 >Input</text>
             </template>
             <template #avatar>
                <Icon src=${Person} tint=#c419c1 />
             </template>
             <template #leadingIcon>
                <Icon src=${Done} tint=#c45cc1 />
             </template>
             <template #trailingIcon>
                <Icon src=${Close} tint=#557799 />
             </template>
        </Chip>
    </column>
        `
    }
})

startActivity(app)