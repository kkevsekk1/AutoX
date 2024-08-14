在第二代 api，ui 构建不再使用传统的 xml 和 e4x,
也不在是基于 view 的系统，而是在 js 端使用[vue3](https://cn.vuejs.org/guide/introduction.html)和[htm](https://github.com/developit/htm)作为模板引擎，在 java 端使用 Jetpack Compose，
要注意的是虽然是使用 vue 但并不代表 vue 所有特性都可用，也不代表就能够使用 web 中的各种 ui 框架，准确的来说是使用了 vue 的核心`@vue/runtime-core`,而实现了一套从 vue 渲染到安卓 Jetpack Compose 的渲染器。

## 入门

想要创建一个界面首先你需要从`vue-ui`模块导入各种函数，如

```js
import { createApp, xml, startActivity, ModifierExtension } from "vue-ui";
```

使用`createApp`创建一个 app 实例，与 vue 的方法相同，不过其中必须包含`render`函数，
用于创建 Vnode。

```js
const app = createApp({
  render() {
    return xml`
    <column>
        UI内容
    </column>
    `;
  },
});
```

`render`渲染函数会在数据变化时调用许多次，你不能在这个函数中做任何与创建 Vnode 无关的事。
创建好后使用`startActivity`函数将打开一个界面并显示。

```js
startActivity(app);
```

`startActivity`会返回一个`Promise`,你可以通过`await`等待 Activity 启动完成并得到
Activity 实例。
与第 1 代 api 不同，你无需在脚本最前面加上`'ui';`，因为脚本线程始终与 ui 线程分离，
这样你还可以通过多次调用`startActivity`启动多个 Activity，但必须是不同的 app 实例。

## 组件
