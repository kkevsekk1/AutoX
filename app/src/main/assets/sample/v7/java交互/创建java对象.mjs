import { loadClass, java } from 'java'

const File = loadClass('java.io.File')

const a = new File('/sdcard')
const b = new java.io.File('/sdcard')

console.log(a.isFile());
console.log(b.isFile());
