import {
    createApp, xml, startActivity, Icons, reactive, computed, ref, ModifierExtension
} from "vue-ui";
import { showToast } from "toast";
const { background, width, padding, height, fillMaxSize } = ModifierExtension
const { Person, Star, Home, Close, Settings } = Icons.Default

const modifier = [padding(0, 5)]
const tabs = [{
    text: "Settings",
    icon: Settings,
    enabled: true,
}, {
    text: "Home",
    icon: Home,
    enabled: false
}, {
    text: "Star",
    icon: Star,
    enabled: true,
}
]
function Tab(tabs) {
    return tabs.map((tab, i) => {
        function click() {
            selectedTabIndex.value = i
        }
        const selected = selectedTabIndex.value == i
        return xml`
    <Tab onClick=${click} selected=${selected} 
        enabled=${tab.enabled} text=${tab.text} icon=${tab.icon}/>
    `
    })
}
function Tab2(tabs) {
    return tabs.map((tab, i) => {
        function click() {
            selectedTabIndex.value = i
        }
        const selected = selectedTabIndex.value == i
        const textColor = tab.enabled ? "#ffcc88" : "#000000"
        const iconColor = tab.enabled ? "#ee44aa" : "#000000"
        return xml`
    <Tab onClick=${click} selected=${selected} 
        enabled=${tab.enabled}>
        <template #text>
            <text color=${textColor}>${tab.text}</text>
        </template>
        <template #icon>
            <Icon src=${tab.icon} tint=${iconColor} />
        </template>
    </Tab>
    `
    })
}
function LeadingIconTab(tabs) {
    return tabs.map((tab, i) => {
        function click() {
            selectedTabIndex.value = i
        }
        const selected = selectedTabIndex.value == i
        return xml`
    <Tab onClick=${click} selected=${selected} 
        type="leadingIcon"
        enabled=${tab.enabled} text=${tab.text} icon=${tab.icon}/>
    `
    })
}

const selectedTabIndex = ref(0)
const app = createApp({
    render() {
        return xml`
    <column>
       <TabRow selectedTabIndex=${selectedTabIndex.value} modifier=${modifier}>
            ${Tab(tabs)}
       </TabRow>
       <TabRow type="scrollable" selectedTabIndex=${selectedTabIndex.value} modifier=${modifier}>
            ${Tab(tabs)}
       </TabRow>
       <TabRow type="primary" selectedTabIndex=${selectedTabIndex.value} modifier=${modifier}>
            ${LeadingIconTab(tabs)}
       </TabRow>
       <TabRow type="primaryScrollable" selectedTabIndex=${selectedTabIndex.value} modifier=${modifier}>
            ${LeadingIconTab(tabs)}
       </TabRow>
       <TabRow type="secondary" selectedTabIndex=${selectedTabIndex.value} modifier=${modifier}>
            ${Tab2(tabs)}
       </TabRow>
       <TabRow type="secondaryScrollable" selectedTabIndex=${selectedTabIndex.value} modifier=${modifier}>
            ${Tab2(tabs)}
       </TabRow>
    </column>
        `
    }
})

startActivity(app)