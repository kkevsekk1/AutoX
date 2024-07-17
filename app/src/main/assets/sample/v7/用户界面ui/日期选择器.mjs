import {
    createApp, xml, startActivity, defineComponent, ModifierExtension
} from "vue-ui";
const { padding, fillMaxSize, height, fillMaxWidth } = ModifierExtension
import { showToast } from "toast";

const D1 = defineComponent({
    data() {
        return {
            show: false,
            state: null
        }
    },
    methods: {
        ok() {
            this.show = false
            //返回的时间截是bigint类型，需要转成普通数值
            const date = new Date(Number(this.state.selectedDateMillis))
            showToast(`你选择的日期为：${date.getFullYear()}年，${date.getMonth() + 1}月，${date.getDate()}日`)
        }
    },
    render() {
        return xml`
    ${this.show && xml`
    <Dialog onDismissRequest=${() => { this.show = false }}>
        <card >
            <column >
                <box>
                    <DatePicker
                        modifier=${[padding(0, 20, 0, 0), height(500)]}
                        yearRange=${[2022, 2025]}
                        onRender=${(s) => { this.state = s }}
                        title="选择日期" />
                </box>
                <row modifier=${[fillMaxWidth()]} horizontalArrangement="end">
                    <Button onClick=${this.ok} type="text">确认</Button>
                </row>
            </column>
        </card>
    </Dialog>
    `}
    <Button onClick=${() => { this.show = true }}>选择日期</Button>
        `
    }
})
const D2 = defineComponent({
    data() {
        return {
            show: false,
            state: null
        }
    },
    methods: {
        ok() {
            this.show = false
            const startDate = new Date(Number(this.state.selectedStartDateMillis))
            const endDate = new Date(Number(this.state.selectedEndDateMillis))
            showToast(`你选择的日期f范围为：${startDate.getFullYear()}年，${startDate.getMonth() + 1}月，${startDate.getDate()
                }日 ~ ${endDate.getFullYear()}年，${endDate.getMonth() + 1}月，${endDate.getDate()
                }日`)
        }
    },
    render() {
        return xml`
        ${this.show && xml`
            <Dialog onDismissRequest=${() => { this.show = false }}>
                <card>
                    <column>
                        <box>
                            <DateRangePicker
                                modifier=${[padding(0, 20, 0, 0), height(500)]}
                                yearRange=${[2022, 2025]}
                                onRender=${(s) => { this.state = s }}
                                title="选择日期" />
                        </box>
                        <row modifier=${[fillMaxWidth()]} horizontalArrangement="end">
                            <Button onClick=${this.ok} type="text">确认</Button>
                        </row>
                    </column>
                </card>
            </Dialog>
            `}
        <Button onClick=${() => { this.show = true }}>选择范围日期</Button>
        `
    }
})



const app = createApp({
    render() {
        return xml`
    <column modifier=${[fillMaxSize()]}>
        <${D1}/>
        <${D2}/>
    </column>
        `
    }
})

startActivity(app)