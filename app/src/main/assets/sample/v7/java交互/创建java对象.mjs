import { loadClass } from 'java'

const File = loadClass('java.io.File')

const a = new File('/sdcard')

console.log(a.isFile());
