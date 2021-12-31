// MyXml.h

#ifndef __MY_XML_H
#define __MY_XML_H

#include "MyString.h"

struct CXmlProp
{
  AString Name;
  AString Value;
};

class CXmlItem
{
public:
  AString Name;
  bool IsTag;
  CObjectVector<CXmlProp> Props;
  CObjectVector<CXmlItem> SubItems;
  
  const char * ParseItem(const char *s, int numAllowedLevels);

  bool IsTagged(const AString &tag) const throw();
  int FindProp(const AString &propName) const throw();
  AString GetPropVal(const AString &propName) const;
  AString GetSubString() const;
  const AString * GetSubStringPtr() const throw();
  int FindSubTag(const AString &tag) const throw();
  AString GetSubStringForTag(const AString &tag) const;

  void AppendTo(AString &s) const;
};

struct CXml
{
  CXmlItem Root;

  bool Parse(const char *s);
  // void AppendTo(AString &s) const;
};

#endif
