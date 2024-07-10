import { showToast } from "toast";
import { createApp, xml } from "vue-ui";

let app = createApp({
    data() {
        return {};
    },
    methods: {
    },
    render() {
        return xml` 
<column>
    ${"这是一个数字: " + this.a}
    <Button enabled=${this.enabled} 
       onClick=${this.add}>按钮</Buttom>
</column>
      `;
    },
});
let data = app.mount("activit");