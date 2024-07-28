import typescript from '@rollup/plugin-typescript';
import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import replace from '@rollup/plugin-replace'
import json from '@rollup/plugin-json';
import fs from 'fs/promises'
import path from 'node:path'

let isDev
if (process.env.NODE_ENV === 'production') {
    isDev = false
} else {
    isDev = true
}
const dirs = await fs.readdir('./src')

const input = Object.fromEntries(dirs.map(name => [
    path.join(name, 'index'),
    path.resolve("src", name, 'index.ts')
]))

export default {
    input,
    output: {
        dir: "dist",
        format: 'es',
        entryFileNames: "[name].js"
    },
    plugins: [
        typescript({
            outDir: "dist"
        }),
        resolve(),
        commonjs(),
        json(),
        replace({
            __VUE_OPTIONS_API__: 'true',
            __VUE_PROD_DEVTOOLS__: String(isDev),
            __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: String(isDev)
        })
    ],
    external: ['lodash']
}