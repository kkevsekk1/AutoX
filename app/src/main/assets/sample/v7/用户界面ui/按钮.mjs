import {
    createApp, xml, startActivity, Icons
} from "vue-ui";
const { Menu, Home } = Icons.Default
const app = createApp({
    render() {
        return xml`
    <column>
        <Button>普通按钮</Button>
        <Button type="text">text按钮</Button>
        <Button type="elevated">elevated按钮</Button>
        <Button type="outlined">outlined按钮</Button>
        <Button type="tonal">tonal按钮</Button>
        <IconButton><Icon src=${Home()} /></IconButton>
    </column>
        `
    }
})

startActivity(app)