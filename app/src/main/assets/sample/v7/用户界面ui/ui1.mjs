import { showToast } from "toast";
import {
    createApp, ref, setDebug, watch, isRef, xml,
} from "vue-ui";
setDebug(true);
let r = ref(1);
console.warn("isRef ", isRef(r));
watch(r, (t) => {
    console.warn(t);
});
//console.log((xml`<text>ppp</text>`))

setTimeout(() => {
    r.value = 2;
}, 2000);

let app = createApp({
    data() {
        return {
            a: 1,
        };
    },
    render() {
        let click = () => {
            showToast("card click");
        };
        let modifier = {
            width: 400,
            height: 50,
            onClick() { }
        };
        console.log("uu");
        console.warn(isRef(this));
        return xml` 
    <column>
      <TopAppBar modifier=${modifier} title=${"Autox"} >
        <template #title>${"模板内容" + r.value} </template>
      </TopAppBar>
      <card r=${{}} modifier=${modifier} onClick=${click}>
            <text modifier=${modifier}>卡片内容</text>
    	</card>
       hhh 
       <Button onClick=${click} ><text>一个按钮tttttt</text></Button>
    </column>             
    `;
    },
});

app.mount("activit");
//renderActivity(h("text", "jjj"))

//console.log(app.mount.toString())
