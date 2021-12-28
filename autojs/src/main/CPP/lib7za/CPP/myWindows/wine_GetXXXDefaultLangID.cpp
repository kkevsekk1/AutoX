
#include "StdAfx.h"

#include <stdio.h>
#include <stdlib.h>
#include <locale.h>
#include <wchar.h>
#include <ctype.h>
#include <string.h>

#ifdef __APPLE__
#define  UInt32 mac_UInt32
#include <CoreFoundation/CoreFoundation.h>
#undef  UInt32
#endif // __APPLE__


// #define TRACE printf

typedef DWORD LCID;
typedef void * ULONG_PTR; /* typedef unsigned long ULONG_PTR; */

#define SORT_DEFAULT        0x0

#define LANG_NEUTRAL        0x00
#define LANG_ENGLISH        0x09

#define SUBLANG_DEFAULT     0x01    /* user default */

#define MAKELCID(l, s)       ( (l & 0xFFFF) | ((s & 0xFFFF)<<16))
#define MAKELANGID(p, s)     ((((WORD)(s))<<10) | (WORD)(p))

#define LANGIDFROMLCID(lcid) ((WORD)(lcid))

static LCID lcid_LC_MESSAGES = 0;
static LCID lcid_LC_CTYPE = 0;

struct locale_name
{
    WCHAR  win_name[128];   /* Windows name ("en-US") */
    WCHAR  lang[128];       /* language ("en") (note: buffer contains the other strings too) */
    WCHAR *country;         /* country ("US") */
    WCHAR *charset;         /* charset ("UTF-8") for Unix format only */
    WCHAR *script;          /* script ("Latn") for Windows format only */
    WCHAR *modifier;        /* modifier or sort order */
    LCID   lcid;            /* corresponding LCID */
    int    matches;         /* number of elements matching LCID (0..4) */
    UINT   codepage;        /* codepage corresponding to charset */
};
#define WINE_UNICODE_INLINE static

/***********************************************************/
typedef struct {
	const WCHAR * LOCALE_SNAME;
	const WCHAR * LOCALE_SISO639LANGNAME;
	const WCHAR * LOCALE_SISO3166CTRYNAME;
	unsigned int  LOCALE_IDEFAULTUNIXCODEPAGE;
	unsigned int  LOCALE_ILANGUAGE;
} t_info;

static t_info g_langInfo[] = {
	{ L"af-ZA" , L"af" , L"ZA" , 28591 , 0x0436 },  /* afk.nls */
	{ L"ar-SA" , L"ar" , L"SA" , 28596 , 0x0401 },  /* ara.nls */
	{ L"ar-LB" , L"ar" , L"LB" , 28596 , 0x3001 },  /* arb.nls */
	{ L"ar-EG" , L"ar" , L"EG" , 28596 , 0x0c01 },  /* are.nls */
	{ L"ar-DZ" , L"ar" , L"DZ" , 28596 , 0x1401 },  /* arg.nls */
	{ L"ar-BH" , L"ar" , L"BH" , 28596 , 0x3c01 },  /* arh.nls */
	{ L"ar-IQ" , L"ar" , L"IQ" , 28596 , 0x0801 },  /* ari.nls */
	{ L"ar-JO" , L"ar" , L"JO" , 28596 , 0x2c01 },  /* arj.nls */
	{ L"ar-KW" , L"ar" , L"KW" , 28596 , 0x3401 },  /* ark.nls */
	{ L"ar-LY" , L"ar" , L"LY" , 28596 , 0x1001 },  /* arl.nls */
	{ L"ar-MA" , L"ar" , L"MA" , 28596 , 0x1801 },  /* arm.nls */
	{ L"ar-OM" , L"ar" , L"OM" , 28596 , 0x2001 },  /* aro.nls */
	{ L"ar-QA" , L"ar" , L"QA" , 28596 , 0x4001 },  /* arq.nls */
	{ L"ar-SY" , L"ar" , L"SY" , 28596 , 0x2801 },  /* ars.nls */
	{ L"ar-TN" , L"ar" , L"TN" , 28596 , 0x1c01 },  /* art.nls */
	{ L"ar-AE" , L"ar" , L"AE" , 28596 , 0x3801 },  /* aru.nls */
	{ L"ar-YE" , L"ar" , L"YE" , 28596 , 0x2401 },  /* ary.nls */
	{ L"az-AZ" , L"az" , L"AZ" , 28595 , 0x082c },  /* aze.nls */
	{ L"az-Latn-AZ" , L"az" , L"AZ" , 28599 , 0x042c },  /* azl.nls */
	{ L"be-BY" , L"be" , L"BY" , 1251 , 0x0423 },  /* bel.nls */
	{ L"bg-BG" , L"bg" , L"BG" , 1251 , 0x0402 },  /* bgr.nls */
	{ L"br-FR" , L"br" , L"FR" , 28605 , 0x0493 },  /* brf.nls */
	{ L"ca-ES" , L"ca" , L"ES" , 28605 , 0x0403 },  /* cat.nls */
	{ L"zh-CN" , L"zh" , L"CN" , 936 , 0x0804 },  /* chs.nls */
	{ L"zh-TW" , L"zh" , L"TW" , 950 , 0x0404 },  /* cht.nls */
	{ L"kw-GB" , L"kw" , L"GB" , 28605 , 0x04891 },  /* cor.nls */
	{ L"cs-CZ" , L"cs" , L"CZ" , 28592 , 0x0405 },  /* csy.nls */
	{ L"cy-GB" , L"cy" , L"GB" , 28604 , 0x0492 },  /* cym.nls */
	{ L"da-DK" , L"da" , L"DK" , 28605 , 0x0406 },  /* dan.nls */
	{ L"de-AT" , L"de" , L"AT" , 28605 , 0x0c07 },  /* dea.nls */
	{ L"de-LI" , L"de" , L"LI" , 28605 , 0x1407 },  /* dec.nls */
	{ L"de-LU" , L"de" , L"LU" , 28605 , 0x1007 },  /* del.nls */
	{ L"de-CH" , L"de" , L"CH" , 28605 , 0x0807 },  /* des.nls */
	{ L"de-DE" , L"de" , L"DE" , 28605 , 0x0407 },  /* deu.nls */
	{ L"dv-MV" , L"dv" , L"MV" , 65001 , 0x0465 },  /* div.nls */
	{ L"el-GR" , L"el" , L"GR" , 28597 , 0x0408 },  /* ell.nls */
	{ L"en-AU" , L"en" , L"AU" , 28591 , 0x0c09 },  /* ena.nls */
	{ L"en-CB" , L"en" , L"CB" , 28591 , 0x2409 },  /* enb.nls */
	{ L"en-CA" , L"en" , L"CA" , 28591 , 0x1009 },  /* enc.nls */
	{ L"en-GB" , L"en" , L"GB" , 28605 , 0x0809 },  /* eng.nls */
	{ L"en-IE" , L"en" , L"IE" , 28605 , 0x1809 },  /* eni.nls */
	{ L"en-JM" , L"en" , L"JM" , 28591 , 0x2009 },  /* enj.nls */
	{ L"en-BZ" , L"en" , L"BZ" , 28591 , 0x2809 },  /* enl.nls */
	{ L"en-PH" , L"en" , L"PH" , 28591 , 0x3409 },  /* enp.nls */
	{ L"en-ZA" , L"en" , L"ZA" , 28591 , 0x1c09 },  /* ens.nls */
	{ L"en-TT" , L"en" , L"TT" , 28591 , 0x2c09 },  /* ent.nls */
	{ L"en-US" , L"en" , L"US" , 28591 , 0x0409 },  /* enu.nls */
	{ L"en-ZW" , L"en" , L"ZW" , 28591 , 0x3009 },  /* enw.nls */
	{ L"en-NZ" , L"en" , L"NZ" , 28591 , 0x1409 },  /* enz.nls */
	{ L"eo" , L"eo" , L"" , 65001 , 0x048f },  /* eox.nls */
	{ L"es-PA" , L"es" , L"PA" , 28591 , 0x180a },  /* esa.nls */
	{ L"es-BO" , L"es" , L"BO" , 28591 , 0x400a },  /* esb.nls */
	{ L"es-CR" , L"es" , L"CR" , 28591 , 0x140a },  /* esc.nls */
	{ L"es-DO" , L"es" , L"DO" , 28591 , 0x1c0a },  /* esd.nls */
	{ L"es-SV" , L"es" , L"SV" , 28591 , 0x440a },  /* ese.nls */
	{ L"es-EC" , L"es" , L"EC" , 28591 , 0x300a },  /* esf.nls */
	{ L"es-GT" , L"es" , L"GT" , 28591 , 0x100a },  /* esg.nls */
	{ L"es-HN" , L"es" , L"HN" , 28591 , 0x480a },  /* esh.nls */
	{ L"es-NI" , L"es" , L"NI" , 28591 , 0x4c0a },  /* esi.nls */
	{ L"es-C" , L"es" , L"C" , 28591 , 0x340a },  /* esl.nls */
	{ L"es-MX" , L"es" , L"MX" , 28591 , 0x080a },  /* esm.nls */
	{ L"es-ES_modern" , L"es" , L"ES" , 28605 , 0x0c0a },  /* esn.nls */
	{ L"es-CO" , L"es" , L"CO" , 28591 , 0x240a },  /* eso.nls */
	{ L"es-ES" , L"es" , L"ES" , 28605 , 0x040a },  /* esp.nls */
	{ L"es-PE" , L"es" , L"PE" , 28591 , 0x280a },  /* esr.nls */
	{ L"es-AR" , L"es" , L"AR" , 28591 , 0x2c0a },  /* ess.nls */
	{ L"es-PR" , L"es" , L"PR" , 28591 , 0x500a },  /* esu.nls */
	{ L"es-VE" , L"es" , L"VE" , 28591 , 0x200a },  /* esv.nls */
	{ L"es-UY" , L"es" , L"UY" , 28591 , 0x380a },  /* esy.nls */
	{ L"es-PY" , L"es" , L"PY" , 28591 , 0x3c0a },  /* esz.nls */
	{ L"et-EE" , L"et" , L"EE" , 28605 , 0x0425 },  /* eti.nls */
	{ L"eu-ES" , L"eu" , L"ES" , 28605 , 0x042d },  /* euq.nls */
	{ L"fa-IR" , L"fa" , L"IR" , 65001 , 0x0429 },  /* far.nls */
	{ L"fi-FI" , L"fi" , L"FI" , 28605 , 0x040b },  /* fin.nls */
	{ L"fo-FO" , L"fo" , L"FO" , 28605 , 0x0438 },  /* fos.nls */
	{ L"fr-FR" , L"fr" , L"FR" , 28605 , 0x040c },  /* fra.nls */
	{ L"fr-BE" , L"fr" , L"BE" , 28605 , 0x080c },  /* frb.nls */
	{ L"fr-CA" , L"fr" , L"CA" , 28591 , 0x0c0c },  /* frc.nls */
	{ L"fr-LU" , L"fr" , L"LU" , 28605 , 0x140c },  /* frl.nls */
	{ L"fr-MC" , L"fr" , L"MC" , 28605 , 0x180c },  /* frm.nls */
	{ L"fr-CH" , L"fr" , L"CH" , 28605 , 0x100c },  /* frs.nls */
	{ L"ga-IE" , L"ga" , L"IE" , 28605 , 0x043c },  /* gae.nls */
	{ L"gd-GB" , L"gd" , L"GB" , 28605 , 0x083c },  /* gdh.nls */
	{ L"gv-GB" , L"gv" , L"GB" , 28605 , 0x0c3c },  /* gdv.nls */
	{ L"gl-ES" , L"gl" , L"ES" , 28605 , 0x0456 },  /* glc.nls */
	{ L"gu-IN" , L"gu" , L"IN" , 65001 , 0x0447 },  /* guj.nls */
	{ L"he-I" , L"he" , L"I" , 28598 , 0x040d },  /* heb.nls */
	{ L"hi-IN" , L"hi" , L"IN" , 65001 , 0x0439 },  /* hin.nls */
	{ L"hr-HR" , L"hr" , L"HR" , 28592 , 0x041a },  /* hrv.nls */
	{ L"hu-HU" , L"hu" , L"HU" , 28592 , 0x040e },  /* hun.nls */
	{ L"hy-AM" , L"hy" , L"AM" , 65001 , 0x042b },  /* hye.nls */
	{ L"id-ID" , L"id" , L"ID" , 28591 , 0x0421 },  /* ind.nls */
	{ L"is-IS" , L"is" , L"IS" , 28605 , 0x040f },  /* isl.nls */
	{ L"it-IT" , L"it" , L"IT" , 28605 , 0x0410 },  /* ita.nls */
	{ L"it-CH" , L"it" , L"CH" , 28605 , 0x0810 },  /* its.nls */
	{ L"ja-JP" , L"ja" , L"JP" , 20932 , 0x0411 },  /* jpn.nls */
	{ L"kn-IN" , L"kn" , L"IN" , 65001 , 0x044b },  /* kan.nls */
	{ L"ka-GE" , L"ka" , L"GE" , 65001 , 0x0437 },  /* kat.nls */
	{ L"kk-KZ" , L"kk" , L"KZ" , 28595 , 0x043f },  /* kkz.nls */
	{ L"kok-IN" , L"kok" , L"IN" , 65001 , 0x0457 },  /* knk.nls */
	{ L"ko-KR" , L"ko" , L"KR" , 949 , 0x0412 },  /* kor.nls */
	{ L"ky-KG" , L"ky" , L"KG" , 28595 , 0x0440 },  /* kyr.nls */
	{ L"lt-LT" , L"lt" , L"LT" , 28603 , 0x0427 },  /* lth.nls */
	{ L"lv-LV" , L"lv" , L"LV" , 28603 , 0x0426 },  /* lvi.nls */
	{ L"mr-IN" , L"mr" , L"IN" , 65001 , 0x044e },  /* mar.nls */
	{ L"mk-MK" , L"mk" , L"MK" , 28595 , 0x042f },  /* mki.nls */
	{ L"mn-MN" , L"mn" , L"MN" , 28595 , 0x0450 },  /* mon.nls */
	{ L"ms-BN" , L"ms" , L"BN" , 28591 , 0x083e },  /* msb.nls */
	{ L"ms-MY" , L"ms" , L"MY" , 28591 , 0x043e },  /* msl.nls */
	{ L"nl-BE" , L"nl" , L"BE" , 28605 , 0x0813 },  /* nlb.nls */
	{ L"nl-N" , L"nl" , L"N" , 28605 , 0x0413 },  /* nld.nls */
	{ L"nl-SR" , L"nl" , L"SR" , 28605 , 0x0c13 },  /* nls.nls */
	{ L"nn-NO" , L"nn" , L"NO" , 28605 , 0x0814 },  /* non.nls */
	{ L"nb-NO" , L"nb" , L"NO" , 28605 , 0x0414 },  /* nor.nls */
	{ L"pa-IN" , L"pa" , L"IN" , 65001 , 0x0446 },  /* pan.nls */
	{ L"pl-P" , L"pl" , L"P" , 28592 , 0x0415 },  /* plk.nls */
	{ L"pt-BR" , L"pt" , L"BR" , 28591 , 0x0416 },  /* ptb.nls */
	{ L"pt-PT" , L"pt" , L"PT" , 28605 , 0x0816 },  /* ptg.nls */
	{ L"rm-CH" , L"rm" , L"CH" , 28605 , 0x0417 },  /* rmc.nls */
	{ L"ro-RO" , L"ro" , L"RO" , 28592 , 0x0418 },  /* rom.nls */
	{ L"ru-RU" , L"ru" , L"RU" , 20866 , 0x0419 },  /* rus.nls */
	{ L"sa-IN" , L"sa" , L"IN" , 65001 , 0x044f },  /* san.nls */
	{ L"sk-SK" , L"sk" , L"SK" , 28592 , 0x041b },  /* sky.nls */
	{ L"sl-SI" , L"sl" , L"SI" , 28592 , 0x0424 },  /* slv.nls */
	{ L"sq-A" , L"sq" , L"A" , 28592 , 0x041c },  /* sqi.nls */
	{ L"sr-SP" , L"sr" , L"SP" , 28595 , 0x0c1a },  /* srb.nls */
	{ L"sr-Latn-SP" , L"sr" , L"SP" , 28592 , 0x081a },  /* srl.nls */
	{ L"sv-SE" , L"sv" , L"SE" , 28605 , 0x041d },  /* sve.nls */
	{ L"sv-FI" , L"sv" , L"FI" , 28605 , 0x081d },  /* svf.nls */
	{ L"sw-KE" , L"sw" , L"KE" , 28591 , 0x0441 },  /* swk.nls */
	{ L"syr-SY" , L"syr" , L"SY" , 65001 , 0x045a },  /* syr.nls */
	{ L"ta-IN" , L"ta" , L"IN" , 65001 , 0x0449 },  /* tam.nls */
	{ L"te-IN" , L"te" , L"IN" , 65001 , 0x044a },  /* tel.nls */
	{ L"th-TH" , L"th" , L"TH" , 874 , 0x041e },  /* tha.nls */
	{ L"tr-TR" , L"tr" , L"TR" , 28599 , 0x041f },  /* trk.nls */
	{ L"tt-TA" , L"tt" , L"TA" , 28595 , 0x0444 },  /* ttt.nls */
	{ L"uk-UA" , L"uk" , L"UA" , 21866 , 0x0422 },  /* ukr.nls */
	{ L"ur-PK" , L"ur" , L"PK" , 1256 , 0x0420 },  /* urd.nls */
	{ L"uz-UZ" , L"uz" , L"UZ" , 28595 , 0x0843 },  /* uzb.nls */
	{ L"uz-Latn-UZ" , L"uz" , L"UZ" , 28605 , 0x0443 },  /* uzl.nls */
	{ L"vi-VN" , L"vi" , L"VN" , 1258 , 0x042a },  /* vit.nls */
	{ L"wa-BE" , L"wa" , L"BE" , 28605 , 0x0490 },  /* wal.nls */
	{ L"zh-HK" , L"zh" , L"HK" , 950 , 0x0c04 },  /* zhh.nls */
	{ L"zh-SG" , L"zh" , L"SG" , 936 , 0x1004 },  /* zhi.nls */
	{ L"zh-MO" , L"zh" , L"MO" , 950 , 0x1404 },  /* zhm.nls */
	{ 0 , 0 , 0 , 0, 0 }
};

/***********************************************************/
WINE_UNICODE_INLINE WCHAR *strchrW( const WCHAR *str, WCHAR ch )
{
    do { if (*str == ch) return (WCHAR *)(ULONG_PTR)str; } while (*str++);
    return NULL;
}

WINE_UNICODE_INLINE WCHAR *strpbrkW( const WCHAR *str, const WCHAR *accept )
{
    for ( ; *str; str++) if (strchrW( accept, *str )) return (WCHAR *)(ULONG_PTR)str;
    return NULL;
}


/***********************************************************/

WINE_UNICODE_INLINE unsigned int strlenW( const WCHAR *str )
{
    const WCHAR *s = str;
    while (*s) s++;
    return s - str;
}

WINE_UNICODE_INLINE WCHAR *strcpyW( WCHAR *dst, const WCHAR *src )
{
    WCHAR *p = dst;
    while ((*p++ = *src++));
    return dst;
}

WINE_UNICODE_INLINE WCHAR *strcatW( WCHAR *dst, const WCHAR *src )
{
    strcpyW( dst + strlenW(dst), src );
    return dst;
}

WINE_UNICODE_INLINE int strcmpW( const WCHAR *str1, const WCHAR *str2 )
{
    while (*str1 && (*str1 == *str2)) { str1++; str2++; }
    return *str1 - *str2;
}


WINE_UNICODE_INLINE LPWSTR lstrcpynW( LPWSTR dst, LPCWSTR src, int n )
{
    {
        LPWSTR d = dst;
        LPCWSTR s = src;
        UINT count = n;

        while ((count > 1) && *s)
        {
            count--;
            *d++ = *s++;
        }
        if (count) *d = 0;
    }
    return dst;
}

/* Copy Ascii string to Unicode without using codepages */
static inline void strcpynAtoW( WCHAR *dst, const char *src, size_t n )
{
    while (n > 1 && *src)
    {
        *dst++ = (unsigned char)*src++;
        n--;
    }
    if (n) *dst = 0;
}

/*******************************************************/

/* Charset to codepage map, sorted by name. */
static const struct charset_entry
{
    const char *charset_name;
    UINT        codepage;
} charset_names[] =
{
    { "BIG5", 950 },
    { "CP1250", 1250 },
    { "CP1251", 1251 },
    { "CP1252", 1252 },
    { "CP1253", 1253 },
    { "CP1254", 1254 },
    { "CP1255", 1255 },
    { "CP1256", 1256 },
    { "CP1257", 1257 },
    { "CP1258", 1258 },
    { "CP932", 932 },
    { "CP936", 936 },
    { "CP949", 949 },
    { "CP950", 950 },
    { "EUCJP", 20932 },
    { "GB2312", 936 },
    { "IBM037", 37 },
    { "IBM1026", 1026 },
    { "IBM424", 424 },
    { "IBM437", 437 },
    { "IBM500", 500 },
    { "IBM850", 850 },
    { "IBM852", 852 },
    { "IBM855", 855 },
    { "IBM857", 857 },
    { "IBM860", 860 },
    { "IBM861", 861 },
    { "IBM862", 862 },
    { "IBM863", 863 },
    { "IBM864", 864 },
    { "IBM865", 865 },
    { "IBM866", 866 },
    { "IBM869", 869 },
    { "IBM874", 874 },
    { "IBM875", 875 },
    { "ISO88591", 28591 },
    { "ISO885910", 28600 },
    { "ISO885913", 28603 },
    { "ISO885914", 28604 },
    { "ISO885915", 28605 },
    { "ISO885916", 28606 },
    { "ISO88592", 28592 },
    { "ISO88593", 28593 },
    { "ISO88594", 28594 },
    { "ISO88595", 28595 },
    { "ISO88596", 28596 },
    { "ISO88597", 28597 },
    { "ISO88598", 28598 },
    { "ISO88599", 28599 },
    { "KOI8R", 20866 },
    { "KOI8U", 21866 },
    { "UTF8", CP_UTF8 }
};

static int charset_cmp( const void *name, const void *entry )
{
    const struct charset_entry *charset = (const struct charset_entry *)entry;
    return strcasecmp( (const char *)name, charset->charset_name );
}

static UINT find_charset( const WCHAR *name )
{
    const struct charset_entry *entry;
    char charset_name[16];
    size_t i, j;

    /* remove punctuation characters from charset name */
    for (i = j = 0; name[i] && j < sizeof(charset_name)-1; i++)
        if (isalnum((unsigned char)name[i])) charset_name[j++] = name[i];
    charset_name[j] = 0;

    entry = (const struct charset_entry *)bsearch( charset_name, charset_names,
                     sizeof(charset_names)/sizeof(charset_names[0]),
                     sizeof(charset_names[0]), charset_cmp );
    if (entry) return entry->codepage;

    return 0;
}
/*******************************************************/

static BOOL find_locale_id_callback(/* LPCWSTR name, ? */ const t_info * tab,  struct locale_name *data)
{
    // WCHAR buffer[128];
    int matches = 0;
    WORD LangID = tab->LOCALE_ILANGUAGE & 0xFFFF; /* FIXME */
    LCID lcid = MAKELCID( LangID, SORT_DEFAULT );  /* FIXME: handle sort order */

    if (PRIMARYLANGID(LangID) == LANG_NEUTRAL) return TRUE; /* continue search */

    /* first check exact name */
    if (data->win_name[0] && tab->LOCALE_SNAME[0])
        /* GetLocaleInfoW( lcid, LOCALE_SNAME | LOCALE_NOUSEROVERRIDE,
                        buffer, sizeof(buffer)/sizeof(WCHAR) )) */
    {
        if (!strcmpW( data->win_name, tab->LOCALE_SNAME ))
        {
            matches = 4;  /* everything matches */
            goto done;
        }
    }

    /*if (!GetLocaleInfoW( lcid, LOCALE_SISO639LANGNAME | LOCALE_NOUSEROVERRIDE,
                         buffer, sizeof(buffer)/sizeof(WCHAR) )) */
    if (tab->LOCALE_SISO639LANGNAME[0] == 0)
        return TRUE;

    if (strcmpW( tab->LOCALE_SISO639LANGNAME , data->lang )) return TRUE;
    matches++;  /* language name matched */

    if (data->country)
    {
         /* if (GetLocaleInfoW( lcid, LOCALE_SISO3166CTRYNAME|LOCALE_NOUSEROVERRIDE,
                            buffer, sizeof(buffer)/sizeof(WCHAR) )) */
        if (tab->LOCALE_SISO3166CTRYNAME[0])
        {
            if (strcmpW(tab->LOCALE_SISO3166CTRYNAME , data->country )) goto done;
            matches++;  /* country name matched */
        }
    }
    else  /* match default language */
    {
        if (SUBLANGID(LangID) == SUBLANG_DEFAULT) matches++;
    }

    if (data->codepage)
    {
        UINT unix_cp;
        /* if (GetLocaleInfoW( lcid, LOCALE_IDEFAULTUNIXCODEPAGE | LOCALE_RETURN_NUMBER,
                            (LPWSTR)&unix_cp, sizeof(unix_cp)/sizeof(WCHAR) )) */
	unix_cp = tab->LOCALE_IDEFAULTUNIXCODEPAGE;
        {
            if (unix_cp == data->codepage) matches++;
        }
    }

    /* FIXME: check sort order */

done:
    if (matches > data->matches)
    {
        data->lcid = lcid;
        data->matches = matches;
    }
    return (data->matches < 4);  /* no need to continue for perfect match */
}


/***********************************************************************
 *		parse_locale_name
 *
 * Parse a locale name into a struct locale_name, handling both Windows and Unix formats.
 * Unix format is: lang[_country][.charset][@modifier]
 * Windows format is: lang[-script][-country][_modifier]
 */
static void parse_locale_name( const WCHAR *str, struct locale_name *name )
{
    static const WCHAR sepW[] = {'-','_','.','@',0};
    static const WCHAR winsepW[] = {'-','_',0};
    static const WCHAR posixW[] = {'P','O','S','I','X',0};
    static const WCHAR cW[] = {'C',0};
    static const WCHAR latinW[] = {'l','a','t','i','n',0};
    static const WCHAR latnW[] = {'-','L','a','t','n',0};
    WCHAR *p;
    int ind;

    // TRACE("%s\n", debugstr_w(str));

    name->country = name->charset = name->script = name->modifier = NULL;
    name->lcid = MAKELCID( MAKELANGID(LANG_ENGLISH,SUBLANG_DEFAULT), SORT_DEFAULT );
    name->matches = 0;
    name->codepage = 0;
    name->win_name[0] = 0;
    lstrcpynW( name->lang, str, sizeof(name->lang)/sizeof(WCHAR) );

    if (!(p = strpbrkW( name->lang, sepW )))
    {
        if (!strcmpW( name->lang, posixW ) || !strcmpW( name->lang, cW ))
        {
            name->matches = 4;  /* perfect match for default English lcid */
            return;
        }
        strcpyW( name->win_name, name->lang );
    }
    else if (*p == '-')  /* Windows format */
    {
        strcpyW( name->win_name, name->lang );
        *p++ = 0;
        name->country = p;
        if (!(p = strpbrkW( p, winsepW ))) goto done;
        if (*p == '-')
        {
            *p++ = 0;
            name->script = name->country;
            name->country = p;
            if (!(p = strpbrkW( p, winsepW ))) goto done;
        }
        *p++ = 0;
        name->modifier = p;
    }
    else  /* Unix format */
    {
        if (*p == '_')
        {
            *p++ = 0;
            name->country = p;
            p = strpbrkW( p, sepW + 2 );
        }
        if (p && *p == '.')
        {
            *p++ = 0;
            name->charset = p;
            p = strchrW( p, '@' );
        }
        if (p)
        {
            *p++ = 0;
            name->modifier = p;
        }

        if (name->charset)
            name->codepage = find_charset( name->charset );

        /* rebuild a Windows name if possible */

        if (name->charset) goto done;  /* can't specify charset in Windows format */
        if (name->modifier && strcmpW( name->modifier, latinW ))
            goto done;  /* only Latn script supported for now */
        strcpyW( name->win_name, name->lang );
        if (name->modifier) strcatW( name->win_name, latnW );
        if (name->country)
        {
            p = name->win_name + strlenW(name->win_name);
            *p++ = '-';
            strcpyW( p, name->country );
        }
    }
done:
    ;

/* DEBUG
    printf("EnumResourceLanguagesW(...):\n");
    printf("  name->win_name=%ls\n", name->win_name);
    printf("  name->lang=%ls\n", name->lang);
    printf("  name->country=%ls\n", name->country);
    printf("  name->codepage=%d\n", name->codepage);
*/
//    EnumResourceLanguagesW( kernel32_handle, (LPCWSTR)RT_STRING, (LPCWSTR)LOCALE_ILANGUAGE,
//                            find_locale_id_callback, (LPARAM)name );

    ind = 0;
    while (g_langInfo[ind].LOCALE_SNAME)
    {
	    BOOL ret = find_locale_id_callback(&g_langInfo[ind],name);
	    if (ret == FALSE) 
		    break;

	    ind++;
    }
}




/********************************/

static UINT setup_unix_locales(void)
{
    struct locale_name locale_name;
    // WCHAR buffer[128];
    WCHAR ctype_buff[128];
    char *locale;
    UINT unix_cp = 0;

    if ((locale = setlocale( LC_CTYPE, NULL )))
    {
        strcpynAtoW( ctype_buff, locale, sizeof(ctype_buff)/sizeof(WCHAR) );
        parse_locale_name( ctype_buff, &locale_name );
        lcid_LC_CTYPE = locale_name.lcid;
        unix_cp = locale_name.codepage;
    }
    if (!lcid_LC_CTYPE)  /* this one needs a default value */
        lcid_LC_CTYPE = MAKELCID( MAKELANGID(LANG_ENGLISH,SUBLANG_DEFAULT), SORT_DEFAULT );

#if 0
    TRACE( "got lcid %04x (%d matches) for LC_CTYPE=%s\n",
           locale_name.lcid, locale_name.matches, debugstr_a(locale) );

#define GET_UNIX_LOCALE(cat) do \
    if ((locale = setlocale( cat, NULL ))) \
    { \
        strcpynAtoW( buffer, locale, sizeof(buffer)/sizeof(WCHAR) ); \
        if (!strcmpW( buffer, ctype_buff )) lcid_##cat = lcid_LC_CTYPE; \
        else { \
            parse_locale_name( buffer, &locale_name );  \
            lcid_##cat = locale_name.lcid; \
            TRACE( "got lcid %04x (%d matches) for " #cat "=%s\n",        \
                   locale_name.lcid, locale_name.matches, debugstr_a(locale) ); \
        } \
    } while (0)

    GET_UNIX_LOCALE( LC_COLLATE );
    GET_UNIX_LOCALE( LC_MESSAGES );
    GET_UNIX_LOCALE( LC_MONETARY );
    GET_UNIX_LOCALE( LC_NUMERIC );
    GET_UNIX_LOCALE( LC_TIME );
#ifdef LC_PAPER
    GET_UNIX_LOCALE( LC_PAPER );
#endif
#ifdef LC_MEASUREMENT
    GET_UNIX_LOCALE( LC_MEASUREMENT );
#endif
#ifdef LC_TELEPHONE
    GET_UNIX_LOCALE( LC_TELEPHONE );
#endif

#undef GET_UNIX_LOCALE

#endif // #if 0

    return unix_cp;
}

/********************************/

static void LOCALE_Init(void)
{
	/*
    extern void __wine_init_codepages( const union cptable *ansi_cp, const union cptable *oem_cp,
                                       const union cptable *unix_cp );
				       */

    // UINT ansi_cp = 1252, oem_cp = 437, mac_cp = 10000, unix_cp;
    UINT unix_cp = 0;

#ifdef __APPLE__
    /* MacOS doesn't set the locale environment variables so we have to do it ourselves */
    CFArrayRef preferred_locales, all_locales;
    CFStringRef user_language_string_ref = NULL;
    char user_locale[50];

    CFLocaleRef user_locale_ref = CFLocaleCopyCurrent();
    CFStringRef user_locale_string_ref = CFLocaleGetIdentifier( user_locale_ref );

    CFStringGetCString( user_locale_string_ref, user_locale, sizeof(user_locale), kCFStringEncodingUTF8 );
    CFRelease( user_locale_ref );
    if (!strchr( user_locale, '.' )) strcat( user_locale, ".UTF-8" );
    unix_cp = CP_UTF8;  /* default to utf-8 even if we don't get a valid locale */
    setenv( "LANG", user_locale, 0 );
    // TRACE( "setting locale to '%s'\n", user_locale );

    /* We still want to set the retrieve the preferred language as chosen in
       System Preferences.app, because it can differ from CFLocaleCopyCurrent().
    */
    all_locales = CFLocaleCopyAvailableLocaleIdentifiers();
    preferred_locales = CFBundleCopyLocalizationsForPreferences( all_locales, NULL );
    if (preferred_locales && CFArrayGetCount( preferred_locales ))
        user_language_string_ref = (CFStringRef)CFArrayGetValueAtIndex( preferred_locales, 0 ); // FIXME
    CFRelease( all_locales );
#endif /* __APPLE__ */

    // FIXME setlocale( LC_ALL, "" );

    unix_cp = setup_unix_locales();
    if (!lcid_LC_MESSAGES) lcid_LC_MESSAGES = lcid_LC_CTYPE;

#ifdef __APPLE__
    /* Override lcid_LC_MESSAGES with user_language if LC_MESSAGES is set to default */
    if (lcid_LC_MESSAGES == lcid_LC_CTYPE && user_language_string_ref)
    {
        struct locale_name locale_name;
        WCHAR buffer[128];
        CFStringGetCString( user_language_string_ref, user_locale, sizeof(user_locale), kCFStringEncodingUTF8 );
        strcpynAtoW( buffer, user_locale, sizeof(buffer)/sizeof(WCHAR) );
        parse_locale_name( buffer, &locale_name );
        lcid_LC_MESSAGES = locale_name.lcid;
        // TRACE( "setting lcid_LC_MESSAGES to '%s'\n", user_locale );
    }
    if (preferred_locales)
        CFRelease( preferred_locales );
#endif

#if 0 // FIXME	
    NtSetDefaultUILanguage( LANGIDFROMLCID(lcid_LC_MESSAGES) );
    NtSetDefaultLocale( TRUE, lcid_LC_MESSAGES );
    NtSetDefaultLocale( FALSE, lcid_LC_CTYPE );

    ansi_cp = get_lcid_codepage( LOCALE_USER_DEFAULT );
    GetLocaleInfoW( LOCALE_USER_DEFAULT, LOCALE_IDEFAULTMACCODEPAGE | LOCALE_RETURN_NUMBER,
                    (LPWSTR)&mac_cp, sizeof(mac_cp)/sizeof(WCHAR) );
    GetLocaleInfoW( LOCALE_USER_DEFAULT, LOCALE_IDEFAULTCODEPAGE | LOCALE_RETURN_NUMBER,
                    (LPWSTR)&oem_cp, sizeof(oem_cp)/sizeof(WCHAR) );
    if (!unix_cp)
        GetLocaleInfoW( LOCALE_USER_DEFAULT, LOCALE_IDEFAULTUNIXCODEPAGE | LOCALE_RETURN_NUMBER,
                        (LPWSTR)&unix_cp, sizeof(unix_cp)/sizeof(WCHAR) );

    if (!(ansi_cptable = wine_cp_get_table( ansi_cp )))
        ansi_cptable = wine_cp_get_table( 1252 );
    if (!(oem_cptable = wine_cp_get_table( oem_cp )))
        oem_cptable  = wine_cp_get_table( 437 );
    if (!(mac_cptable = wine_cp_get_table( mac_cp )))
        mac_cptable  = wine_cp_get_table( 10000 );
    if (unix_cp != CP_UTF8)
    {
        if (!(unix_cptable = wine_cp_get_table( unix_cp )))
            unix_cptable  = wine_cp_get_table( 28591 );
    }

    __wine_init_codepages( ansi_cptable, oem_cptable, unix_cptable );

    TRACE( "ansi=%03d oem=%03d mac=%03d unix=%03d\n",
           ansi_cptable->info.codepage, oem_cptable->info.codepage,
           mac_cptable->info.codepage, unix_cp );

    setlocale(LC_NUMERIC, "C");  /* FIXME: oleaut32 depends on this */
#endif
}

LANGID GetUserDefaultLangID(void)
{
    // return LANGIDFROMLCID(GetUserDefaultLCID());
    if (lcid_LC_MESSAGES == 0) LOCALE_Init();
    return LANGIDFROMLCID(lcid_LC_MESSAGES);
}

LANGID GetSystemDefaultLangID(void)
{
    // return LANGIDFROMLCID(GetSystemDefaultLCID());
    if (lcid_LC_MESSAGES == 0) LOCALE_Init();
    return LANGIDFROMLCID(lcid_LC_MESSAGES);
}

#ifdef TEST
int main()
{
	LANGID langID;
	WORD primLang;
	WORD subLang;

	setlocale( LC_ALL, "" );

	langID = GetUserDefaultLangID();
	printf("langID=0x%x\n",langID);

	primLang = (WORD)(PRIMARYLANGID(langID));
	subLang = (WORD)(SUBLANGID(langID));

	printf("primLang=%d subLang=%d\n",(unsigned)primLang,(unsigned)subLang);

	return 0;
}
#endif

