import typescript from '@rollup/plugin-typescript';
import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import json from '@rollup/plugin-json';
import fs from 'fs/promises'
import path from 'node:path'

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
    ],
    external: ['lodash']
}