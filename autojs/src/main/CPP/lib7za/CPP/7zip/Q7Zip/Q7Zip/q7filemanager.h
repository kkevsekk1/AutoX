#ifndef Q7FILEMANAGER_H
#define Q7FILEMANAGER_H

#include <QMainWindow>

QT_BEGIN_NAMESPACE
class QAbstractItemModel;
class QCheckBox;
class QComboBox;
class QDateEdit;
class QGroupBox;
class QLabel;
class QLineEdit;
class QTreeView;
class QTextEdit;
class QPushButton;
class QPlainTextEdit;
class QStandardItemModel;
class QModelIndex;
QT_END_NAMESPACE

class Q7SortFilerProxyModel;

class Q7FileManager : public QMainWindow
{
    Q_OBJECT

public:
    Q7FileManager(QWidget *parent = 0);
    ~Q7FileManager();

    void setSourceModel(QAbstractItemModel *model);

    void setDir(const QString & dirPath);

private:

    QWidget * buildLineEdit();

    QPushButton * m_btnParent;

    QString m_dirPath;
    QLineEdit* m_pathEdit;

    Q7SortFilerProxyModel *m_listView_proxyModel;
    QTreeView * m_listView;
    QStandardItemModel * m_dirModel;

    QPlainTextEdit * m_logMsg;

private slots:
    void on_Parent();
    void item_doubleClicked(const QModelIndex & ind);
};

#endif // Q7FILEMANAGER_H
