import { showToast } from "toast";
import { createApp, xml } from "vue-ui";
let app = createApp({
    data() {
        return {
            a: 1,
            enabled: true,
        };
    },
    methods: {
        add() {
            showToast("+1");
            this.a++;
            if (this.a > 5) {
                this.enabled = false;
            }
        },
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
setTimeout(() => {
    let id = setInterval(() => {
        data.a++;
        if (data.a > 100) {
            clearInterval(id);
        }
    }, 5);
}, 5000);
