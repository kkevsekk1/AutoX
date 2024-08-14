import { getRunningEngines } from 'engines'

getRunningEngines().forEach(engine => {
    engine.emit('test', 789012)
})