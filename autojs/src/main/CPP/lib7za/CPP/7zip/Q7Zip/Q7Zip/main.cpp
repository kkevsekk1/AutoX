
#include <QString>

#include <QtGui>

#include <QApplication>

#include "q7filemanager.h"

#include "Common/MyString.h"

extern int  utilZip_init(void);
extern int  utilZip_setPath(const wchar_t *path);
extern int utilZip_GetNumberOfItems();
extern UString utilZip_GetItemName(int ind);

void logMsg(const QString & text);

int main(int argc, char *argv[])
{
    QApplication a(argc, argv);

    Q7FileManager w;

    // w.setSourceModel(createMailModel(&w));
#if (QT_VERSION >= QT_VERSION_CHECK(5, 0, 0))
    QString path =  QDir::homePath();
#else
    QString path = QDesktopServices::storageLocation(QDesktopServices::HomeLocation);
#endif
    w.setDir(path);


    utilZip_init();

    std::wstring wstr = path.toStdWString ();
    const wchar_t * wpath = wstr.c_str();
    utilZip_setPath(wpath);

     int nb = utilZip_GetNumberOfItems();

     logMsg(QString("nb=%1").arg(nb));

     for(int i = 0 ;i < nb ; i++)
     {
        UString ustr = utilZip_GetItemName(i);

        QString str = QString::fromWCharArray((const wchar_t *)ustr);

        logMsg(QString("%1 : \"%2\"").arg(i).arg(str));
     }


    w.show();

    return a.exec();
}
