import { series } from 'gulp'
import { rm } from 'fs/promises'
import fs from 'fs/promises'
import { rollup } from 'rollup'
import { loadConfigFile } from 'rollup/loadConfigFile'

export async function clear(cb) {
    await rm('./dist', { recursive: true, force: true })
    cb()
}

export async function createPackageFile(cb) {
    const modules = await fs.readdir('./dist')
    await Promise.all(modules.map(async function (module) {
        const path = "./dist/" + module
        const stat = await fs.stat(path)
        if (stat.isDirectory()) {
            await fs.writeFile(path + "/package.json", JSON.stringify({
                name: module,
                version: '0.0.0',
                type: "module",
                main: "index.js"
            }, undefined, 2))
        }
    })
    )
    cb()
}
async function createRootPackageFile(cb) {
    const n = JSON.parse(await fs.readFile('./package.json', 'utf8'))
    const packageFile = {
        name: n.name,
        version: n.version,
        description: n.description,
        type: "module",
        license: n.license,
    }
    await fs.writeFile('./dist/package.json', JSON.stringify(packageFile, undefined, 2))
}

export const build = series(
    clear,
    async function rollupBuild(cb) {
        const { options, warnings } = await loadConfigFile('./rollup.config.mjs')
        warnings.flush()
        for (const option of options) {
            const bundle = await rollup(option)
            await Promise.all(option.output.map(bundle.write))
        }
    },
    createPackageFile,
    createRootPackageFile,
)