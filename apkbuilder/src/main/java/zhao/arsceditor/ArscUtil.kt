package zhao.arsceditor

import zhao.arsceditor.ResDecoder.ARSCCallBack
import zhao.arsceditor.ResDecoder.data.ResTable
import java.io.*
import java.util.*

class ArscUtil {

    companion object {
        // 存储资源Configs的集合
        var Configs: MutableList<String>? = null
        const val TAG = "ArscUtil"

        // 存储资源种类的集合
        var Types: MutableList<String>? = null

        // 资源类型常量
        const val ARSC = 0
        const val AXML = 1
        const val DEX = 2
    }

    // 存储字符串的集合
    var txtOriginal: MutableList<String> = ArrayList()

    // 存储修改后的字符串的集合
    var txtTranslated: MutableList<String> = ArrayList()

    // 存储字符串在资源中对应的键
    var txtTranslatedKey: MutableList<String> = ArrayList()

    // 存储资源的集合
    private val resources: MutableList<Map<String, String>> = ArrayList()

    // ARSC解析器
    private val mAndRes: AndrolibResources by lazy { AndrolibResources() }

    // 字符串是否修改
    var isChanged = false

    // 资源类型
    private val ResType = 0

    @Throws(IOException::class)
    private fun open(
        resFile: String,
        callback: ((config: String, type: String, key: String, value: String, id: Int) -> Unit)? = null
    ) {
        FileInputStream(resFile).use { input->
            when {
                resFile.endsWith(".arsc") -> {
                    open(input, ARSC, callback)
                }
                resFile.endsWith(".xml") -> {
                    open(input, AXML)
                }
                resFile.endsWith(".dex") -> {
                    open(input, DEX)
                }
                else -> {
                    throw IOException("Unsupported FileType")
                }
            }
        }

    }

    private fun open(
        resInputStream: InputStream,
        resType: Int,
        callback: ((config: String, type: String, key: String, value: String, id: Int) -> Unit)? = null
    ) {
        // 如果储存资源类型的列表未初始化
        if (Types == null) {
            // 初始化储存资源类型的列表
            Types = ArrayList()
        }

        // 实现资源回调接口
        val callback = ARSCCallBack { config, type, key, value, id ->
            if (key == null || type == null) return@ARSCCallBack
            callback?.invoke(config, type, key, value, id)
            // 这里是为了出去一些不能编辑的字符串
            // 初始化键值映射
            val values: MutableMap<String, String> = HashMap()
            // 向映射中添加资源的键
            values[MyObj.NAME] = key
            // 向映射中添加资源的值
            values[MyObj.VALUE] = value
            // 向映射中添加资源的种类
            values[MyObj.TYPE] = type
            // 向映射中添加资源的Config
            values[MyObj.CONFIG] = config
            // 向资源中添加该映射
            resources.add(values)
            // 如果资源种类集合中不存在该种类
            if (Types?.contains(type) != true) // 向其中添加该种类
                Types?.add(type)
        }
        try {
            mAndRes.decodeARSC(getResTable(resInputStream), callback)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Types?.sort()
    }

    // 获取ARSC文件的ResTable的方法
    @Throws(IOException::class)
    fun getResTable(ARSCStream: InputStream?): ResTable {
        return mAndRes.getResTable(ARSCStream)
    }

    // 一个储存键的类
    internal object MyObj {
        const val NAME = "name"
        const val VALUE = "value"
        const val TYPE = "type"
        const val CONFIG = "config"
    }

    fun openArsc(
        filename: String,
        callback: ((config: String, type: String, key: String, value: String, id: Int) -> Unit)? = null
    ) {
        try {
            open(filename, callback)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun saveArsc(oldFileName: String, newFileName: String) {
        val oldFile = File(oldFileName)
        val newFile = File(newFileName)
        oldFile.inputStream().use { input ->
            newFile.outputStream().use { out ->
                mAndRes.mARSCDecoder.write(out, input, txtOriginal, txtTranslated)
            }
        }
        isChanged = false
    }

    fun getResouces(key: String, cfg: String) {
        // 如果储存Config的列表未初始化
        if (Configs == null) {
            // 初始化Config列表
            Configs = ArrayList()
        }

        // 检查是否发生改变
        for (str in txtTranslated) {
            if (str != "") isChanged = true
            break
        }
        if (isChanged) {
            // 排序整理修改后的内容，以方便一一写入
            for (i in txtOriginal.indices) mAndRes.mARSCDecoder.mTableStrings.sortStringBlock(
                txtOriginal[i], txtTranslated[i]
            )
        }

        // 清除几个列表中的元素
        txtOriginal.clear()
        txtTranslated.clear()
        txtTranslatedKey.clear()
        Configs!!.clear()
        for (resource in resources) {
            // 获取资源的键
            val name = resource.getValue(MyObj.NAME)
            // 获取资源的值
            val value = resource.getValue(MyObj.VALUE)
            // 获取资源类型
            val type = resource[MyObj.TYPE]
            // 获取资源分支
            val config = resource[MyObj.CONFIG]
            //            System.out.println("NAME: " + NAME + " VALUE: " + VALUE + " TYPE: " + TYPE + " CONGIG: " + CONFIG);
            // 如果资源的Config开头存在-符号，并且Config列表中不存在该资源的Config元素，并且资源种类是params[0]的值
            if (config!!.startsWith("-") && !Configs!!.contains(config.substring(1)) && type == key) // 向Config列表中添加元素
                Configs!!.add(config.substring(1))
            else if (!config.startsWith("-") && !Configs!!.contains(config) && type == key)
                Configs!!.add(config)

            // 如果Config列表中存在该资源的Config元素
            if (type == key){
                // 如果Config是params[1]的值或者是
                if(config == cfg || config == "-$cfg"){
                    // 向储存字符串的列表中添加字符串成员
                    txtOriginal.add(value)
                    // 向储存修改后的字符串的列表中添加空成员
                    txtTranslated.add("")
                    // 向储存资源的键的列表添加键
                    txtTranslatedKey.add(name)
                }
            }
        }
        Configs?.sort()
    }

    fun changeResource(key: String, value: String) {
        val position = txtTranslatedKey.indexOf(key)
        if (position == -1) return
        println("txtTranslated: " + txtOriginal[position])
        txtTranslated.removeAt(position)
        // 向当前位置添加新的内容，以此实现文本的更新
        txtTranslated.add(position, value)
    }

}