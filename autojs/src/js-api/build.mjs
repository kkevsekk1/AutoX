import { execSync } from 'child_process'


execSync('npm install', { stdio: 'inherit' })

execSync('npm run build', { stdio: 'inherit' })
