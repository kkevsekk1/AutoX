import { invokeDefault } from '@/java'
const media = Autox.media
export class MediaPlayer {
    private _media = media.createMediaPlayer()
    get androidMediaPlayer(): any {
        return this._media
    }
    /**
     * @returns {number}当前播放位置。单位毫秒。
     */
    get currentPosition(): number {
        return this._media.getCurrentPosition()
    }
    /**
     * @returns {number} 音乐时长。单位毫秒。
     */
    get duration(): number {
        return this._media.getDuration()
    }
    get isPlaying(): boolean {
        return this._media.isPlaying()
    }
    async play(uri: string, volume?: number, looping?: boolean) {
        this.setDataSource(uri)
        if (volume) this.setVolume(volume)
        if (looping) this.setLooping(looping)
        await this.prepare()
        this.start()
    }
    pause() {
        this._media.pause()
    }
    async prepare() {
        await invokeDefault(this._media, "prepare", [])
    }
    prepareSync(): void {
        this._media.prepare()
    }
    release() {
        this._media.release()
    }
    reset() {
        this._media.reset()
    }
    async seekTo(msec: number) {
        await invokeDefault(this._media, "seekTo", [msec])
    }
    setDataSource(path: string) {
        this._media.setDataSource(path)
    }
    setLooping(looping: boolean) {
        this._media.setLooping(looping)
    }
    setScreenOnWhilePlaying(keep: boolean) {
        this._media.setScreenOnWhilePlaying(keep)
    }
    setVolume(leftVolume: number, rightVolume?: number) {
        if (rightVolume === undefined) rightVolume = leftVolume
        this._media.setVolume(leftVolume, rightVolume)
    }
    start() {
        this._media.start()
    }
    stop() {
        this._media.stop()
    }
}

export async function playMusic(uri: string, volume?: number, looping?: boolean): Promise<MediaPlayer> {
    const media = new MediaPlayer()
    await media.play(uri, volume, looping)
    return media
}

export function scanFile(file: string): void {
    media.scanFile(file)
}