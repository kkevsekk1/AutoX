package org.autojs.autojs.ui.explorer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.snackbar.Snackbar
import com.stardust.pio.PFiles
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.Pref
import org.autojs.autojs.model.explorer.Explorer
import org.autojs.autojs.model.explorer.ExplorerChangeEvent
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.ExplorerFileItem
import org.autojs.autojs.model.explorer.ExplorerItem
import org.autojs.autojs.model.explorer.ExplorerPage
import org.autojs.autojs.model.explorer.ExplorerProjectPage
import org.autojs.autojs.model.explorer.ExplorerSampleItem
import org.autojs.autojs.model.explorer.ExplorerSamplePage
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.model.script.Scripts.edit
import org.autojs.autojs.model.script.Scripts.openByOtherApps
import org.autojs.autojs.model.script.Scripts.run
import org.autojs.autojs.model.script.Scripts.send
import org.autojs.autojs.theme.widget.ThemeColorSwipeRefreshLayout
import org.autojs.autojs.tool.Observers
import org.autojs.autojs.ui.build.BuildActivity
import org.autojs.autojs.ui.common.ScriptLoopDialog
import org.autojs.autojs.ui.common.ScriptOperations
import org.autojs.autojs.ui.viewmodel.ExplorerItemList
import org.autojs.autojs.ui.viewmodel.ExplorerItemList.SortConfig
import org.autojs.autojs.ui.widget.BindableViewHolder
import org.autojs.autojs.workground.WrapContentGridLayoutManger
import org.autojs.autoxjs.R
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Stack

open class ExplorerViewKt : ThemeColorSwipeRefreshLayout, OnRefreshListener,
    PopupMenu.OnMenuItemClickListener, ViewTreeObserver.OnGlobalFocusChangeListener {

    private var onItemClickListener: ((view: View, item: ExplorerItem?) -> Unit)? = null
    private var onItemOperatedListener: ((item: ExplorerItem?) -> Unit)? = null
    private var explorerItemList = ExplorerItemList()
    protected var explorerItemListView: RecyclerView? = null
        private set
    private var projectToolbar: ExplorerProjectToolbar? = null
    private val explorerAdapter: ExplorerAdapter = ExplorerAdapter()
    private var filter: ((ExplorerItem) -> Boolean)? = null
    protected var selectedItem: ExplorerItem? = null
    private var explorer: Explorer? = null
    private val pageStateHistory = Stack<ExplorerPageState>()
    private var currentPageState = ExplorerPageState()
    private var dirSortMenuShowing = false
    private var directorySpanSize1 = 2
    val currentPage get() = currentPageState.currentPage

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    fun setRootPage(page: ExplorerPage?) {
        pageStateHistory.clear()
        setCurrentPageState(ExplorerPageState(page))
        loadItemList()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && canGoBack()) {
            goBack()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun setCurrentPageState(currentPageState: ExplorerPageState) {
        this.currentPageState = currentPageState
        if (this.currentPageState.currentPage is ExplorerProjectPage) {
            projectToolbar!!.visibility = VISIBLE
            projectToolbar!!.setProject(currentPageState.currentPage!!.toScriptFile())
        } else {
            projectToolbar!!.visibility = GONE
        }
    }

    protected fun enterDirectChildPage(childItemGroup: ExplorerPage?) {
        currentPageState.scrollY =
            (explorerItemListView!!.layoutManager as LinearLayoutManager?)!!.findLastCompletelyVisibleItemPosition()
        pageStateHistory.push(currentPageState)
        setCurrentPageState(ExplorerPageState(childItemGroup))
        loadItemList()
    }

    fun setOnItemClickListener(listener: (view: View, item: ExplorerItem?) -> Unit) {
        this.onItemClickListener = listener
    }

    var sortConfig: SortConfig?
        get() = explorerItemList.sortConfig
        set(sortConfig) {
            explorerItemList.sortConfig = sortConfig
        }

    fun setExplorer(explorer: Explorer?, rootPage: ExplorerPage?) {
        if (this.explorer != null) this.explorer!!.unregisterChangeListener(this)
        this.explorer = explorer
        setRootPage(rootPage)
        this.explorer!!.registerChangeListener(this)
    }

    fun setExplorer(explorer: Explorer?, rootPage: ExplorerPage?, currentPage: ExplorerPage) {
        if (this.explorer != null) this.explorer!!.unregisterChangeListener(this)
        this.explorer = explorer
        pageStateHistory.clear()
        setCurrentPageState(ExplorerPageState(rootPage))
        this.explorer!!.registerChangeListener(this)
        enterChildPage(currentPage)
    }

    fun enterChildPage(childPage: ExplorerPage) {
        val root = currentPageState.currentPage!!.toScriptFile()
        var dir = childPage.toScriptFile()
        val dirs = Stack<ScriptFile>()
        while (dir != root) {
            dir = dir!!.parentFile
            if (dir == null) {
                break
            }
            dirs.push(dir)
        }
        var parent: ExplorerDirPage? = null
        while (!dirs.empty()) {
            dir = dirs.pop()
            val dirPage = ExplorerDirPage(dir, parent)
            pageStateHistory.push(ExplorerPageState(dirPage))
            parent = dirPage
        }
        setCurrentPageState(ExplorerPageState(childPage))
        loadItemList()
    }

    fun setOnItemOperatedListener(listener: (item: ExplorerItem?) -> Unit) {
        this.onItemOperatedListener = listener
    }

    fun canGoBack(): Boolean {
        return !pageStateHistory.empty()
    }

    fun goBack() {
        setCurrentPageState(pageStateHistory.pop())
        loadItemList()
    }

    fun setDirectorySpanSize(directorySpanSize: Int) {
        directorySpanSize1 = directorySpanSize
    }

    fun setFilter(filter: (ExplorerItem) -> Boolean) {
        this.filter = filter
        reload()
    }

    fun reload() {
        loadItemList()
    }

    override fun isFocused(): Boolean {
        return true
    }

    private fun init() {
        Log.d(
            LOG_TAG, "item bg = " + Integer.toHexString(
                ContextCompat.getColor(
                    context, R.color.item_background
                )
            )
        )
        setOnRefreshListener(this)
        inflate(context, R.layout.explorer_view, this)
        explorerItemListView = findViewById(R.id.explorer_item_list)
        projectToolbar = findViewById(R.id.project_toolbar)
        initExplorerItemListView()
    }

    private fun initExplorerItemListView() {
        explorerItemListView!!.adapter = explorerAdapter
        val manager = WrapContentGridLayoutManger(context, 2)
        manager.setDebugInfo("ExplorerView")
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                //For directories
                return if (position > positionOfCategoryDir && position < positionOfCategoryFile()) {
                    directorySpanSize1
                } else 2
                //For files and category
            }
        }
        explorerItemListView!!.layoutManager = manager
    }

    private fun positionOfCategoryFile(): Int {
        return if (currentPageState.dirsCollapsed) 1 else explorerItemList.groupCount() + 1
    }

    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun loadItemList() {
        isRefreshing = true
        explorer!!.fetchChildren(currentPageState.currentPage)
            .subscribeOn(Schedulers.io())
            .flatMapObservable { page: ExplorerPage? ->
                currentPageState.currentPage = page
                Observable.fromIterable(page)
            }
            .filter { f: ExplorerItem -> if (filter == null) true else filter!!.invoke(f) }
            .collectInto(explorerItemList.cloneConfig()) { obj: ExplorerItemList, item: ExplorerItem? ->
                obj.add(
                    item
                )
            }
            .observeOn(Schedulers.computation())
            .doOnSuccess { obj: ExplorerItemList -> obj.sort() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { list: ExplorerItemList ->
                explorerItemList = list
                explorerAdapter.notifyDataSetChanged()
                isRefreshing = false
                post { explorerItemListView!!.scrollToPosition(currentPageState.scrollY) }
            }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onExplorerChange(event: ExplorerChangeEvent) {
        if (event.action == ExplorerChangeEvent.ALL) {
            setRootPage(ExplorerDirPage.createRoot(Pref.getScriptDirPath()))
            return
        }
        val currentDirPath = currentPageState.currentPage!!.path
        val changedDirPath = event.page.path
        val item = event.item
        val changedItemPath = item?.path
        if (currentDirPath == changedItemPath || currentDirPath == changedDirPath &&
            event.action == ExplorerChangeEvent.CHILDREN_CHANGE
        ) {
            loadItemList()
            return
        }
        if (currentDirPath == changedDirPath) {
            val i: Int
            when (event.action) {
                ExplorerChangeEvent.CHANGE -> {
                    i = explorerItemList.update(item, event.newItem)
                    if (i >= 0) {
                        explorerAdapter.notifyItemChanged(item, i)
                    }
                }

                ExplorerChangeEvent.CREATE -> {
                    explorerItemList.insertAtFront(event.newItem)
                    explorerAdapter.notifyItemInserted(event.newItem, 0)
                }

                ExplorerChangeEvent.REMOVE -> {
                    i = explorerItemList.remove(item)
                    if (i >= 0) {
                        explorerAdapter.notifyItemRemoved(item, i)
                    }
                }
            }
        }
    }

    override fun onRefresh() {
        explorer!!.notifyChildrenChanged(currentPageState.currentPage)
        projectToolbar!!.refresh()
    }

    val currentDirectory: ScriptFile?
        get() = currentPage?.toScriptFile()

    @SuppressLint("CheckResult")
    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.rename -> ScriptOperations(context, this, currentPage)
                .rename(selectedItem as ExplorerFileItem?)
                .subscribe(Observers.emptyObserver())

            R.id.delete -> ScriptOperations(context, this, currentPage)
                .delete(selectedItem!!.toScriptFile())

            R.id.run_repeatedly -> {
                ScriptLoopDialog(context, selectedItem!!.toScriptFile())
                    .show()
                notifyOperated()
            }

            R.id.create_shortcut -> ScriptOperations(context, this, currentPage)
                .createShortcut(selectedItem!!.toScriptFile())

            R.id.open_by_other_apps -> {
                openByOtherApps(selectedItem!!.toScriptFile())
                notifyOperated()
            }

            R.id.send -> {
                send(selectedItem!!.toScriptFile())
                notifyOperated()
            }

            R.id.timed_task -> {
                ScriptOperations(context, this, currentPage)
                    .timedTask(selectedItem!!.toScriptFile())
                notifyOperated()
            }

            R.id.action_build_apk -> {
                BuildActivity.start(context, selectedItem!!.path)
                notifyOperated()
            }

            R.id.action_sort_by_date -> sort(ExplorerItemList.SORT_TYPE_DATE, dirSortMenuShowing)
            R.id.action_sort_by_type -> sort(ExplorerItemList.SORT_TYPE_TYPE, dirSortMenuShowing)
            R.id.action_sort_by_name -> sort(ExplorerItemList.SORT_TYPE_NAME, dirSortMenuShowing)
            R.id.action_sort_by_size -> sort(ExplorerItemList.SORT_TYPE_SIZE, dirSortMenuShowing)
            R.id.reset -> {
                Explorers.Providers.workspace().resetSample(
                    selectedItem!!.toScriptFile()
                ).observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Snackbar.make(
                            this,
                            R.string.text_reset_succeed,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }, Observers.toastMessage())
            }

            else -> return false
        }
        return true
    }

    protected fun notifyOperated() {
        onItemOperatedListener?.invoke(selectedItem)
    }

    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun sort(sortType: Int, isDir: Boolean) {
        isRefreshing = true
        Observable.fromCallable {
            if (isDir) {
                explorerItemList.sortItemGroup(sortType)
            } else {
                explorerItemList.sortFile(sortType)
            }
            explorerItemList
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                explorerAdapter.notifyDataSetChanged()
                isRefreshing = false
            }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (explorer != null) explorer!!.registerChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        explorer!!.unregisterChangeListener(this)
    }

    protected open fun onCreateViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        viewType: Int
    ): BindableViewHolder<Any> {
        return when (viewType) {
            VIEW_TYPE_ITEM -> {
                ExplorerItemViewHolder(
                    inflater.inflate(
                        R.layout.script_file_list_file,
                        parent,
                        false
                    )
                )
            }

            VIEW_TYPE_PAGE -> {
                ExplorerPageViewHolder(
                    inflater.inflate(
                        R.layout.script_file_list_directory,
                        parent,
                        false
                    )
                )
            }

            else -> {
                CategoryViewHolder(
                    inflater.inflate(
                        R.layout.script_file_list_category,
                        parent,
                        false
                    )
                )
            }
        }
    }

    private inner class ExplorerAdapter : RecyclerView.Adapter<BindableViewHolder<Any>>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindableViewHolder<Any> {
            val inflater = LayoutInflater.from(context)
            return this@ExplorerViewKt.onCreateViewHolder(inflater, parent, viewType)
        }

        override fun onBindViewHolder(holder: BindableViewHolder<Any>, position: Int) {
            val positionOfCategoryFile = positionOfCategoryFile()
            if (position == positionOfCategoryDir || position == positionOfCategoryFile) {
                holder.bind(position == positionOfCategoryDir, position)
                return
            }
            if (position < positionOfCategoryFile) {
                holder.bind(explorerItemList.getItemGroup(position - 1), position)
                return
            }
            holder.bind(
                explorerItemList.getItem(position - positionOfCategoryFile - 1),
                position
            )
        }

        override fun getItemViewType(position: Int): Int {
            val positionOfCategoryFile = positionOfCategoryFile()
            return if (position == positionOfCategoryDir || position == positionOfCategoryFile) {
                VIEW_TYPE_CATEGORY
            } else if (position < positionOfCategoryFile) {
                VIEW_TYPE_PAGE
            } else {
                VIEW_TYPE_ITEM
            }
        }

        fun getItemPosition(item: ExplorerItem?, i: Int): Int {
            return if (item is ExplorerPage) {
                i + positionOfCategoryDir + 1
            } else i + positionOfCategoryFile() + 1
        }

        fun notifyItemChanged(item: ExplorerItem?, i: Int) {
            notifyItemChanged(getItemPosition(item, i))
        }

        fun notifyItemRemoved(item: ExplorerItem?, i: Int) {
            notifyItemRemoved(getItemPosition(item, i))
        }

        fun notifyItemInserted(item: ExplorerItem?, i: Int) {
            notifyItemInserted(getItemPosition(item, i))
        }

        override fun getItemCount(): Int {
            var count = 0
            if (!currentPageState.dirsCollapsed) {
                count += explorerItemList.groupCount()
            }
            if (!currentPageState.filesCollapsed) {
                count += explorerItemList.itemCount()
            }
            return count + 2
        }
    }

    @SuppressLint("NonConstantResourceId")
    inner class ExplorerItemViewHolder internal constructor(itemView: View) :
        BindableViewHolder<Any>(itemView) {
        val name: TextView
        val firstChar: TextView
        val desc: TextView
        val options: View
        val edit: View
        val run: View
        val firstCharBackground: GradientDrawable
        private var explorerItem: ExplorerItem? = null
        override fun bind(item: Any, position: Int) {
            if (item !is ExplorerItem) return
            explorerItem = item
            name.text = ExplorerViewHelper.getDisplayName(item)
            desc.text = PFiles.getHumanReadableSize(item.size)
            firstChar.text = ExplorerViewHelper.getIconText(item)
            firstCharBackground.setColor(ExplorerViewHelper.getIconColor(item))
            edit.visibility = if (item.isEditable) VISIBLE else GONE
            run.visibility = if (item.isExecutable) VISIBLE else GONE
        }

        fun onItemClick() {
            onItemClickListener?.invoke(itemView, explorerItem)
            notifyOperated()
        }

        fun run() {
            run(ScriptFile(explorerItem!!.path))
            notifyOperated()
        }

        fun edit() {
            edit(context, ScriptFile(explorerItem!!.path))
            notifyOperated()
        }

        fun showOptionMenu() {
            selectedItem = explorerItem
            val popupMenu = PopupMenu(context, options)
            popupMenu.inflate(R.menu.menu_script_options)
            val menu = popupMenu.menu
            if (!explorerItem!!.isExecutable) {
                menu.removeItem(R.id.run_repeatedly)
                menu.removeItem(R.id.more)
            }
            if (!explorerItem!!.canDelete()) {
                menu.removeItem(R.id.delete)
            }
            if (!explorerItem!!.canRename()) {
                menu.removeItem(R.id.rename)
            }
            if (explorerItem !is ExplorerSampleItem) {
                menu.removeItem(R.id.reset)
            }
            popupMenu.setOnMenuItemClickListener(this@ExplorerViewKt)
            popupMenu.show()
        }

        init {
            name = itemView.findViewById(R.id.name)
            firstChar = itemView.findViewById(R.id.first_char)
            desc = itemView.findViewById(R.id.desc)
            options = itemView.findViewById(R.id.more)
            options.setOnClickListener { showOptionMenu() }
            edit = itemView.findViewById(R.id.edit)
            edit.setOnClickListener { edit() }
            run = itemView.findViewById(R.id.run)
            run.setOnClickListener { run() }
            itemView.findViewById<View>(R.id.item).setOnClickListener { onItemClick() }
            firstCharBackground = firstChar.background as GradientDrawable
        }
    }

    @SuppressLint("NonConstantResourceId")
    inner class ExplorerPageViewHolder internal constructor(itemView: View) :
        BindableViewHolder<Any>(itemView) {
        val name: TextView
        val options: View
        val icon: ImageView
        private var explorerPage: ExplorerPage? = null
        override fun bind(data: Any, position: Int) {
            if (data !is ExplorerPage) return
            name.text = ExplorerViewHelper.getDisplayName(data)
            icon.setImageResource(ExplorerViewHelper.getIcon(data))
            options.visibility =
                if (data is ExplorerSamplePage) GONE else VISIBLE
            explorerPage = data
        }


        fun onItemClick() {
            enterDirectChildPage(explorerPage)
        }

        fun showOptionMenu() {
            selectedItem = explorerPage
            val popupMenu = PopupMenu(context, options)
            popupMenu.inflate(R.menu.menu_dir_options)
            popupMenu.setOnMenuItemClickListener(this@ExplorerViewKt)
            popupMenu.show()
        }

        init {
            name = itemView.findViewById(R.id.name)
            options = itemView.findViewById(R.id.more)
            icon = itemView.findViewById(R.id.icon)
            options.setOnClickListener { showOptionMenu() }
            itemView.findViewById<View>(R.id.item).setOnClickListener { onItemClick() }
        }
    }

    @SuppressLint("NonConstantResourceId")
    internal inner class CategoryViewHolder(itemView: View) :
        BindableViewHolder<Any>(itemView) {
        val title: TextView
        val sort: ImageView
        val sortOrder: ImageView
        val goBack: ImageView
        val arrow: ImageView
        private var isDir = false
        override fun bind(isDirCategory: Any, position: Int) {
            if (isDirCategory !is Boolean) return
            title.setText(if (isDirCategory) R.string.text_directory else R.string.text_file)
            isDir = isDirCategory
            if (isDirCategory && canGoBack()) {
                goBack.visibility = VISIBLE
            } else {
                goBack.visibility = GONE
            }
            if (isDirCategory) {
                arrow.rotation = if (currentPageState.dirsCollapsed) -90f else 0.toFloat()
                sortOrder.setImageResource(if (explorerItemList.isDirSortedAscending) R.drawable.ic_ascending_order else R.drawable.ic_descending_order)
            } else {
                arrow.rotation = if (currentPageState.filesCollapsed) -90f else 0.toFloat()
                sortOrder.setImageResource(if (explorerItemList.isFileSortedAscending) R.drawable.ic_ascending_order else R.drawable.ic_descending_order)
            }
        }

        fun changeSortOrder() {
            if (isDir) {
                sortOrder.setImageResource(if (explorerItemList.isDirSortedAscending) R.drawable.ic_ascending_order else R.drawable.ic_descending_order)
                explorerItemList.isDirSortedAscending = !explorerItemList.isDirSortedAscending
                sort(explorerItemList.dirSortType, isDir)
            } else {
                sortOrder.setImageResource(if (explorerItemList.isFileSortedAscending) R.drawable.ic_ascending_order else R.drawable.ic_descending_order)
                explorerItemList.isFileSortedAscending = !explorerItemList.isFileSortedAscending
                sort(explorerItemList.fileSortType, isDir)
            }
        }

        fun showSortOptions() {
            val popupMenu = PopupMenu(context, sort)
            popupMenu.inflate(R.menu.menu_sort_options)
            popupMenu.setOnMenuItemClickListener(this@ExplorerViewKt)
            dirSortMenuShowing = isDir
            popupMenu.show()
        }

        fun back() {
            if (canGoBack()) {
                goBack()
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        fun collapseOrExpand() {
            if (isDir) {
                currentPageState.dirsCollapsed = !currentPageState.dirsCollapsed
            } else {
                currentPageState.filesCollapsed = !currentPageState.filesCollapsed
            }
            explorerAdapter.notifyDataSetChanged()
        }

        init {
            title = itemView.findViewById(R.id.title)
            sort = itemView.findViewById(R.id.sort)
            sortOrder = itemView.findViewById(R.id.order)
            goBack = itemView.findViewById(R.id.back)
            arrow = itemView.findViewById(R.id.collapse)
            sortOrder.setOnClickListener {
                changeSortOrder()
            }
            sort.setOnClickListener {
                showSortOptions()
            }
            goBack.setOnClickListener {
                back()
            }
            itemView.findViewById<View>(R.id.title_container).setOnClickListener {
                collapseOrExpand()
            }

        }
    }

    private class ExplorerPageState {
        var currentPage: ExplorerPage? = null
        var dirsCollapsed = false
        var filesCollapsed = false
        var scrollY = 0

        constructor()
        constructor(page: ExplorerPage?) {
            currentPage = page
        }
    }

    companion object {
        private const val LOG_TAG = "ExplorerView"
        const val VIEW_TYPE_ITEM = 0
        const val VIEW_TYPE_PAGE = 1

        //category是类别，也即"文件", "文件夹"那两个
        protected const val VIEW_TYPE_CATEGORY = 2
        private const val positionOfCategoryDir = 0
    }

    override fun onGlobalFocusChanged(oldView: View, newView: View) {
        newView.setOnKeyListener { _, _, event ->
            Log.d("TAG", "dispatchKeyEvent: ")
            if (event.keyCode == KeyEvent.KEYCODE_BACK && canGoBack()) {
                goBack()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    }
}