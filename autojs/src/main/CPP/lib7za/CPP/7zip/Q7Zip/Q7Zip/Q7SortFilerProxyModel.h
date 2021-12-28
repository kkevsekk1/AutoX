#ifndef Q7SORTFILTERPROXYMODEL_H
#define Q7SORTFILTERPROXYMODEL_H

#include <QDate>
#include <QSortFilterProxyModel>

class Q7SortFilerProxyModel : public QSortFilterProxyModel
{
    Q_OBJECT

public:
    Q7SortFilerProxyModel(QObject *parent = 0);

/*
    QDate filterMinimumDate() const { return minDate; }
    void setFilterMinimumDate(const QDate &date);

    QDate filterMaximumDate() const { return maxDate; }
    void setFilterMaximumDate(const QDate &date);
*/
protected:
    bool filterAcceptsRow(int sourceRow, const QModelIndex &sourceParent) const;
    // bool lessThan(const QModelIndex &left, const QModelIndex &right) const;

private:
    /*
    bool dateInRange(const QDate &date) const;

    QDate minDate;
    QDate maxDate;
    */
};

#endif
