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
```

```ts
    //ts
    //递归查找
    function find(predicate: (result: MlKitOcrResult) => boolean): MlKitOcrResult

    //递归查找，level:层级
    function find(level: number, predicate: (MlKitOcrResult) => boolean): MlKitOcrResult

    //递归过滤
    function filter(predicate: (result: MlKitOcrResult) => boolean): []MlKitOcrResult

    //递归过滤，level:层级
    function filter(level: number, predicate: (result: MlKitOcrResult) => boolean): []MlKitOcrResult

    //转数组，level:层级
    function toArray(level: number): List<MlKitOcrResult>

    //转数组
    function toArray(): List<MlKitOcrResult> 
```