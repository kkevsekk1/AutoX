#-------------------------------------------------
#
# Project created by QtCreator 2011-07-10T18:45:42
#
#-------------------------------------------------

QT += core gui

greaterThan(QT_MAJOR_VERSION, 4): QT += widgets

TARGET = Q7Zip
TEMPLATE = app


SOURCES += main.cpp\
        q7filemanager.cpp \
    Q7SortFilerProxyModel.cpp

HEADERS  += q7filemanager.h \
    Q7SortFilerProxyModel.h

INCLUDEPATH +=  ../../../
INCLUDEPATH += ../../../include_windows

#INCLUDEPATH += ../../../myWindows
#INCLUDEPATH +=  ../../../
#INCLUDEPATH += ../../../include_windows
#INCLUDEPATH += ../FileManager

#DEFINES += _FILE_OFFSET_BITS=64 _LARGEFILE_SOURCE NDEBUG _REENTRANT ENV_UNIX BREAK_HANDLER UNICODE _UNICODE
#DEFINES += -DLANG -DNEW_FOLDER_INTERFACE -DEXTERNAL_CODECS

DEFINES +=  UNICODE  _UNICODE  UNIX_USE_WIN_FILE

LIBS += -L../util7zip -lutil7zip
