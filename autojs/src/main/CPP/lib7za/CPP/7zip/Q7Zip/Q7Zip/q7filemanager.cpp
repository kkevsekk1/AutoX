#include "q7filemanager.h"

#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QPushButton>
#include <QLineEdit>
#include <QPlainTextEdit>
#include <QTreeView>
#include <QAbstractItemModel>
#include <QStandardItemModel>
#include <QDesktopServices>
#include <QUrl>


#include <QDir>
#include <QFileIconProvider>

#include "Q7SortFilerProxyModel.h"

static QPlainTextEdit * gbl_logMsg;

void logMsg(const QString & text)
{
    gbl_logMsg->appendPlainText(text);
}

static QStandardItemModel *createEmptyModel(QObject *parent)
{
    QStandardItemModel *model = new QStandardItemModel(0, 7, parent);

    model->setHeaderData(0, Qt::Horizontal, QObject::tr("Name"));
    model->setHeaderData(1, Qt::Horizontal, QObject::tr("Size"));
    model->setHeaderData(2, Qt::Horizontal, QObject::tr("Modified"));
    model->setHeaderData(3, Qt::Horizontal, QObject::tr("Created"));
    model->setHeaderData(4, Qt::Horizontal, QObject::tr("Accessed"));
    model->setHeaderData(5, Qt::Horizontal, QObject::tr("Attributes"));
    model->setHeaderData(6, Qt::Horizontal, QObject::tr("Packed Size"));
    // model->setHeaderData(7, Qt::Horizontal, QObject::tr("Comment"));

    return model;
}

Q7FileManager::Q7FileManager(QWidget *parent)
    : QMainWindow(parent)
{
    this->setWindowTitle(tr("Q7zip"));

    QWidget * centralWidget = new QWidget;
    QVBoxLayout * layout = new QVBoxLayout(centralWidget);

    m_listView = new QTreeView();
    m_logMsg = new QPlainTextEdit;
    m_logMsg->setReadOnly(true);
    gbl_logMsg = m_logMsg;


    //////////////////////

    m_listView_proxyModel = new Q7SortFilerProxyModel(this);
    m_listView_proxyModel->setDynamicSortFilter(true);

    m_listView = new QTreeView;
    m_listView->setRootIsDecorated(false);
    m_listView->setAlternatingRowColors(true);
    m_listView->setModel(m_listView_proxyModel);
    m_listView->setSortingEnabled(m_listView_proxyModel);
    m_listView->sortByColumn(0, Qt::AscendingOrder);

    connect(m_listView,SIGNAL(doubleClicked(QModelIndex)) , this, SLOT(item_doubleClicked(QModelIndex)));

    layout->addWidget( buildLineEdit() );
    layout->addWidget(m_listView);
    layout->addWidget(m_logMsg);


    setCentralWidget(centralWidget);


    m_dirModel = createEmptyModel(this);
    this->setSourceModel( m_dirModel );

    resize(800,600);

    logMsg("line1");
    logMsg("Ok");
}

QWidget * Q7FileManager::buildLineEdit()
{
    QWidget * w = new QWidget;
    QHBoxLayout * layout = new QHBoxLayout(w);

    m_btnParent = new QPushButton(tr("Up"));
    connect(m_btnParent,SIGNAL(clicked()),this,SLOT(on_Parent()));

    m_pathEdit = new QLineEdit();

    layout->addWidget(m_btnParent);
    layout->addWidget(m_pathEdit);


    return w;
}

void Q7FileManager::on_Parent()
{
    QDir dir(m_dirPath);

    dir.cdUp();
    this->setDir(dir.absolutePath());
}

void Q7FileManager::item_doubleClicked(const QModelIndex & ind)
{
    // FIXME : how to have the column 0 ?


    QModelIndex ind2 = m_listView_proxyModel->mapToSource(ind);

    // QStandardItem * item = m_dirModel->itemFromIndex(ind2); // ->text();

    QStandardItem * item = m_dirModel->item(ind2.row(),0);

    if (item)
    {
        QString name = item->text(); // items.value(0).toString();
        QFileInfo fi( m_dirPath, name );
        if (fi.isDir())
        {
            logMsg(tr(" DIR  : %1").arg(name));
            this->setDir(fi.absoluteFilePath());
        }
        else
        {
            QUrl url("file://"+fi.absoluteFilePath(),QUrl::TolerantMode);

            bool bret = QDesktopServices::openUrl(url);
            logMsg(tr(" FILE : %1 => %2").arg(name).arg(bret));
        }
    }
}

void Q7FileManager::setDir(const QString & dirPath)
{
    m_dirPath = dirPath;
    m_pathEdit->setText(m_dirPath);


    // FIXME - clear
    m_dirModel = createEmptyModel(this);


    QDir dir(dirPath);
    dir.setFilter(QDir::AllEntries | QDir::Hidden | QDir::NoDotAndDotDot); // QDir::NoSymLinks
    // dir.setSorting(QDir::Size | QDir::Reversed);

    QFileInfoList list = dir.entryInfoList();
    for (int i = 0; i < list.size(); ++i) {
        QFileInfo fileInfo = list.at(i);


        m_dirModel->insertRow(0);
        /*
              QStyle * style = this->style();
        if (fileInfo.isDir())
        {
            QIcon icon = style->standardIcon ( QStyle::SP_DirIcon );
            m_dirModel->setItem(0, 0, new QStandardItem ( icon, fileInfo.fileName() ));
        }
        else
        {
            QIcon icon = style->standardIcon ( QStyle::SP_FileIcon );
            m_dirModel->setItem(0, 0, new QStandardItem ( icon, fileInfo.fileName() ));
        }
        */
        QStandardItem * item = new QStandardItem ( QFileIconProvider().icon(fileInfo) , fileInfo.fileName() );
        item->setEditable(false);
        m_dirModel->setItem(0, 0, item);

        m_dirModel->setData(m_dirModel->index(0, 1), fileInfo.size());
        m_dirModel->item(0,1)->setEditable(false);

        m_dirModel->setData(m_dirModel->index(0, 2), fileInfo.lastModified());
        m_dirModel->item(0,2)->setEditable(false);

        m_dirModel->setData(m_dirModel->index(0, 3), fileInfo.created());
        m_dirModel->item(0,3)->setEditable(false);

        m_dirModel->setData(m_dirModel->index(0, 4), fileInfo.lastRead());
        m_dirModel->item(0,4)->setEditable(false);

        if (fileInfo.isDir())  m_dirModel->setData(m_dirModel->index(0, 5), "drw-r-----");  // FIXME
        else                   m_dirModel->setData(m_dirModel->index(0, 5), "-rw-r-----");  // FIXME
        m_dirModel->item(0,5)->setEditable(false);

        m_dirModel->setData(m_dirModel->index(0, 6), fileInfo.size());  // FIXME
        m_dirModel->item(0,6)->setEditable(false);
    }

    // FIXME
    m_listView_proxyModel->setSourceModel(m_dirModel);
}

void Q7FileManager::setSourceModel(QAbstractItemModel *model)
{
    m_listView_proxyModel->setSourceModel(model);
}

Q7FileManager::~Q7FileManager()
{

}
