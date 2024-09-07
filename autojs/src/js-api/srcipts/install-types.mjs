#!/usr/bin/env node
import inquirer from 'inquirer'
import fs from 'fs-extra'
import { fileURLToPath } from 'url'

const def = {
    "compilerOptions": {
        "target": "ESNext",
        "useDefineForClassFields": true,
        "module": "ESNext",
        "moduleResolution": "Node",
        "strict": true,
        "declaration": true,
        "resolveJsonModule": true,
        "isolatedModules": false,
        "esModuleInterop": true,
        "lib": ["ESNext"],
        "skipLibCheck": true,
    },
    "include": []
}
let tsconfig
let exits = false;
try {
    tsconfig = JSON.parse(await fs.readFile('tsconfig.json'))
    exits = true
} catch (e) { tsconfig = def }
if (!tsconfig.compilerOptions.typeRoots) {
    tsconfig.compilerOptions.typeRoots = ["node_modules/@types"]
}
tsconfig.compilerOptions.typeRoots.push(
    fileURLToPath(new URL('../', import.meta.url))
)

const { type, src } = await inquirer.prompt([{
    type: 'list',
    name: 'type',
    message: '选择你的项目类型:',
    default: 'js',
    choices: ['js', 'ts'],
}, {
    type: 'input',
    name: 'src',
    message: '输入你的代码位置',
    default: './src'
}
])
if (type == 'js') {
    tsconfig.compilerOptions.allowJs = true
    tsconfig.compilerOptions.checkJs = true
    if (!exits) {
        tsconfig.include.push(src + '/**/*.js')
        tsconfig.include.push(src + '/**/*.mjs')
        tsconfig.include.push(src + '/**/*.cjs')
    }
}
if (type == 'ts') {
    tsconfig.compilerOptions.allowJs = true
    tsconfig.compilerOptions.checkJs = true
    if (!exits) {
        tsconfig.include.push(src + '/**/*.ts')
        tsconfig.include.push(src + '/**/*.d.ts')
    }
}


await fs.writeFile('tsconfig.json', JSON.stringify(tsconfig, null, 2))
console.log('操作完成!');
