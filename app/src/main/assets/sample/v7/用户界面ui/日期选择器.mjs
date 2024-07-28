import {
    createApp, xml, startActivity, defineComponent, ModifierExtension
} from "vue-ui";
const { padding, fillMaxSize, height, verticalScroll, fillMaxWidth } = ModifierExtension
import { showToast } from "toast";

const D1 = defineComponent({
    data() {
        return {
            state: null
        }
    },
    methods: {
        ok() {
            //返回的时间截是bigint类型，需要转成普通数值
            const date = new Date(Number(this.state.selectedDateMillis))
            showToast(`你选择的日期为：${date.getFullYear()}年，${date.getMonth() + 1}月，${date.getDate()}日`)
        }
    },
    render() {
        return xml`
        <card modifier=${[padding(5)]}>
            <column >
                <box modifier=${[height(500)]}>
                    <DatePicker
                        modifier=${[padding(0, 20, 0, 0), height(500)]}
                        yearRange=${[2022, 2025]}
                        onRender=${(s) => { this.state = s }}
                        title="选择日期" />
                </box>
                <Button onClick=${this.ok} >确认</Button>
            </column>
        </card>
        `
    }
})
const D2 = defineComponent({
    data() {
        return {
            state: null
        }
    },
    methods: {
        ok() {
            const startDate = new Date(Number(this.state.selectedStartDateMillis))
            const endDate = new Date(Number(this.state.selectedEndDateMillis))
            showToast(`你选择的日期f范围为：${startDate.getFullYear()}年，${startDate.getMonth() + 1}月，${startDate.getDate()
                }日 ~ ${endDate.getFullYear()}年，${endDate.getMonth() + 1}月，${endDate.getDate()
                }日`)
        }
    },
    render() {
        return xml`
        <card modifier=${[padding(5)]}>
             <column>
                <box modifier=${[height(500)]}>
                    <DateRangePicker
                        modifier=${[padding(0, 20, 0, 0), height(500)]}
                        yearRange=${[2022, 2025]}
                        onRender=${(s) => { this.state = s }}
                        title="选择日期" />
                </box>   
                <Button onClick=${this.ok} >确认</Button>      
            </column>
        </card>
        `
    }
})



const app = createApp({
    render() {
        return xml`
    <column modifier=${[fillMaxSize(), verticalScroll()]}>
        <${D1}/>
        <${D2}/>
    </column>
        `
    }
})

startActivity(app)