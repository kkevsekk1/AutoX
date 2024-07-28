## MlKitOcrResult

```kotlin
    //kotlin
    //递归查找
    fun find(predicate: (MlKitOcrResult) -> Boolean): MlKitOcrResult?

    //递归查找，level:层级
    fun find(level: Int, predicate: (MlKitOcrResult) -> Boolean): MlKitOcrResult?

    //递归过滤
    fun filter(predicate: (MlKitOcrResult) -> Boolean): List<MlKitOcrResult>

    //递归过滤，level:层级
    fun filter(level: Int, predicate: (MlKitOcrResult) -> Boolean): List<MlKitOcrResult>

    //转数组，level:层级
    fun toArray(level: Int): List<MlKitOcrResult>

    //转数组
    fun toArray(): List<MlKitOcrResult>

    //对对象本身排序
    fun sort()

    //排序，返回新对象
    fun sorted(): GoogleMLKitOcrResult
}
```