/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
#include "config.h"

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/time.h> /* gettimeofday */
#include <dirent.h>
#include <unistd.h>
#include <time.h>

#include <windows.h>

// #define TRACEN(u) u;
#define TRACEN(u)  /* */

typedef LONG NTSTATUS;
#define STATUS_SUCCESS                   0x00000000

#define TICKSPERSEC        10000000
#define TICKSPERMSEC       10000
#define SECSPERDAY         86400
#define SECSPERHOUR        3600
#define SECSPERMIN         60
#define MINSPERHOUR        60
#define HOURSPERDAY        24
#define EPOCHWEEKDAY       1  /* Jan 1, 1601 was Monday */
#define DAYSPERWEEK        7
#define EPOCHYEAR          1601
#define DAYSPERNORMALYEAR  365
#define DAYSPERLEAPYEAR    366
#define MONSPERYEAR        12
#define DAYSPERQUADRICENTENNIUM (365 * 400 + 97)
#define DAYSPERNORMALCENTURY (365 * 100 + 24)
#define DAYSPERNORMALQUADRENNIUM (365 * 4 + 1)

/* 1601 to 1970 is 369 years plus 89 leap days */
#define SECS_1601_TO_1970  ((369 * 365 + 89) * (ULONGLONG)SECSPERDAY)
#define TICKS_1601_TO_1970 (SECS_1601_TO_1970 * TICKSPERSEC)
/* 1601 to 1980 is 379 years plus 91 leap days */
#define SECS_1601_TO_1980  ((379 * 365 + 91) * (ULONGLONG)SECSPERDAY)
#define TICKS_1601_TO_1980 (SECS_1601_TO_1980 * TICKSPERSEC)
typedef short CSHORT;

static LONG TIME_GetBias() {
  time_t utc = time(NULL);
  struct tm *ptm = localtime(&utc);
  int localdaylight = ptm->tm_isdst; /* daylight for local timezone */
  ptm = gmtime(&utc);
  ptm->tm_isdst = localdaylight; /* use local daylight, not that of Greenwich */
  LONG bias = (int)(mktime(ptm)-utc);
  TRACEN((printf("TIME_GetBias %ld\n",(long)bias)))
  return bias;
}

static inline void RtlSystemTimeToLocalTime( const LARGE_INTEGER *SystemTime,
                                      LARGE_INTEGER *LocalTime ) {
  LONG bias = TIME_GetBias();
  LocalTime->QuadPart = SystemTime->QuadPart - bias * (LONGLONG)TICKSPERSEC;
}

void WINAPI RtlSecondsSince1970ToFileTime( DWORD Seconds, FILETIME * ft ) {
  ULONGLONG secs = Seconds * (ULONGLONG)TICKSPERSEC + TICKS_1601_TO_1970;
  ft->dwLowDateTime  = (DWORD)secs;
  ft->dwHighDateTime = (DWORD)(secs >> 32);
  TRACEN((printf("RtlSecondsSince1970ToFileTime %lx => %lx %lx\n",(long)Seconds,(long)ft->dwHighDateTime,(long)ft->dwLowDateTime)))
}

/*
void WINAPI RtlSecondsSince1970ToTime( DWORD Seconds, LARGE_INTEGER *Time )
{
    ULONGLONG secs = Seconds * (ULONGLONG)TICKSPERSEC + TICKS_1601_TO_1970;
    // Time->u.LowPart  = (DWORD)secs; Time->u.HighPart = (DWORD)(secs >> 32);
    Time->QuadPart = secs;
}
 */

BOOL WINAPI DosDateTimeToFileTime( WORD fatdate, WORD fattime, FILETIME * ft)
{
    struct tm newtm;
#ifndef ENV_HAVE_TIMEGM
    struct tm *gtm;
    time_t time1, time2;
#endif

    TRACEN((printf("DosDateTimeToFileTime\n")))

    newtm.tm_sec  = (fattime & 0x1f) * 2;
    newtm.tm_min  = (fattime >> 5) & 0x3f;
    newtm.tm_hour = (fattime >> 11);
    newtm.tm_mday = (fatdate & 0x1f);
    newtm.tm_mon  = ((fatdate >> 5) & 0x0f) - 1;
    newtm.tm_year = (fatdate >> 9) + 80;
    newtm.tm_isdst = -1;
#ifdef ENV_HAVE_TIMEGM
    RtlSecondsSince1970ToFileTime( timegm(&newtm), ft );
#else
    newtm.tm_isdst = 0;
    time1 = mktime(&newtm);
    gtm = gmtime(&time1);
    time2 = mktime(gtm);
    RtlSecondsSince1970ToFileTime( 2*time1-time2, ft );
#endif
    TRACEN((printf("DosDateTimeToFileTime(%ld,%ld) => %lx %lx\n",
          (long)fatdate,(long)fattime,
          (long)ft->dwHighDateTime,(long)ft->dwLowDateTime)))

    return TRUE;
}

/*
BOOL WINAPI DosDateTimeToFileTime( WORD fatdate, WORD fattime, FILETIME * ft) {
  struct tm newtm;

  TRACEN((printf("DosDateTimeToFileTime\n")))

  memset(&newtm,0,sizeof(newtm));
  newtm.tm_sec  = (fattime & 0x1f) * 2;
  newtm.tm_min  = (fattime >> 5) & 0x3f;
  newtm.tm_hour = (fattime >> 11);
  newtm.tm_mday = (fatdate & 0x1f);
  newtm.tm_mon  = ((fatdate >> 5) & 0x0f) - 1;
  newtm.tm_year = (fatdate >> 9) + 80;
  newtm.tm_isdst = -1;

  time_t time1 = mktime(&newtm);
  LONG   bias  = TIME_GetBias();
  RtlSecondsSince1970ToFileTime( time1 - bias, ft );


  TRACEN((printf("DosDateTimeToFileTime(%ld,%ld) t1=%ld => %lx %lx\n",
        (long)fatdate,(long)fattime,(long)time1,
	(long)ft->dwHighDateTime,(long)ft->dwLowDateTime)))

  return TRUE;
}
*/

BOOLEAN WINAPI RtlTimeToSecondsSince1970( const LARGE_INTEGER *Time, DWORD *Seconds ) {
  ULONGLONG tmp = Time->QuadPart;
  TRACEN((printf("RtlTimeToSecondsSince1970-1 %llx\n",tmp)))
  tmp /= TICKSPERSEC;
  tmp -= SECS_1601_TO_1970;
  TRACEN((printf("RtlTimeToSecondsSince1970-2 %llx\n",tmp)))
  if (tmp > 0xffffffff) return FALSE;
  *Seconds = (DWORD)tmp;
  return TRUE;
}

BOOL WINAPI FileTimeToDosDateTime( const FILETIME *ft, WORD *fatdate, WORD *fattime ) {
  LARGE_INTEGER       li;
  ULONG               t;
  time_t              unixtime;
  struct tm*          tm;
  WORD fat_d,fat_t;

  TRACEN((printf("FileTimeToDosDateTime\n")))
  li.QuadPart = ft->dwHighDateTime;
  li.QuadPart = (li.QuadPart << 32) | ft->dwLowDateTime;
  RtlTimeToSecondsSince1970( &li, &t );
  unixtime = t; /* unixtime = t; * FIXME unixtime = t - TIME_GetBias(); */

  tm = gmtime( &unixtime );

  fat_t = (tm->tm_hour << 11) + (tm->tm_min << 5) + (tm->tm_sec / 2);
  fat_d = ((tm->tm_year - 80) << 9) + ((tm->tm_mon + 1) << 5) + tm->tm_mday;
  if (fattime)
    *fattime = fat_t;
  if (fatdate)
    *fatdate = fat_d;

  TRACEN((printf("FileTimeToDosDateTime : %lx %lx => %d %d\n",
	(long)ft->dwHighDateTime,(long)ft->dwLowDateTime,(unsigned)fat_d,(unsigned)fat_t)))

  return TRUE;
}

BOOL WINAPI FileTimeToLocalFileTime( const FILETIME *utcft, FILETIME * localft ) {
  LARGE_INTEGER local, utc;

  TRACEN((printf("FileTimeToLocalFileTime\n")))
  utc.QuadPart = utcft->dwHighDateTime;
  utc.QuadPart = (utc.QuadPart << 32) | utcft->dwLowDateTime;
  RtlSystemTimeToLocalTime( &utc, &local );
  localft->dwLowDateTime = (DWORD)local.QuadPart;
  localft->dwHighDateTime = (DWORD)(local.QuadPart >> 32);

  return TRUE;
}

typedef struct _TIME_FIELDS {
  CSHORT Year;
  CSHORT Month;
  CSHORT Day;
  CSHORT Hour;
  CSHORT Minute;
  CSHORT Second;
  CSHORT Milliseconds;
  CSHORT Weekday;
} TIME_FIELDS;

static const int MonthLengths[2][MONSPERYEAR] =
{
   { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 },
   { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 }
};

static inline int IsLeapYear(int Year) {
  return Year % 4 == 0 && (Year % 100 != 0 || Year % 400 == 0) ? 1 : 0;
}

static inline VOID WINAPI RtlTimeToTimeFields(
  const LARGE_INTEGER *liTime,
  TIME_FIELDS * TimeFields) {
	int SecondsInDay;
        long int cleaps, years, yearday, months;
	long int Days;
	LONGLONG Time;

	/* Extract millisecond from time and convert time into seconds */
	TimeFields->Milliseconds =
            (CSHORT) (( liTime->QuadPart % TICKSPERSEC) / TICKSPERMSEC);
	Time = liTime->QuadPart / TICKSPERSEC;

	/* The native version of RtlTimeToTimeFields does not take leap seconds
	 * into account */

	/* Split the time into days and seconds within the day */
	Days = Time / SECSPERDAY;
	SecondsInDay = Time % SECSPERDAY;

	/* compute time of day */
	TimeFields->Hour = (CSHORT) (SecondsInDay / SECSPERHOUR);
	SecondsInDay = SecondsInDay % SECSPERHOUR;
	TimeFields->Minute = (CSHORT) (SecondsInDay / SECSPERMIN);
	TimeFields->Second = (CSHORT) (SecondsInDay % SECSPERMIN);

	/* compute day of week */
	TimeFields->Weekday = (CSHORT) ((EPOCHWEEKDAY + Days) % DAYSPERWEEK);

        /* compute year, month and day of month. */
        cleaps=( 3 * ((4 * Days + 1227) / DAYSPERQUADRICENTENNIUM) + 3 ) / 4;
        Days += 28188 + cleaps;
        years = (20 * Days - 2442) / (5 * DAYSPERNORMALQUADRENNIUM);
        yearday = Days - (years * DAYSPERNORMALQUADRENNIUM)/4;
        months = (64 * yearday) / 1959;
        /* the result is based on a year starting on March.
         * To convert take 12 from Januari and Februari and
         * increase the year by one. */
        if( months < 14 ) {
            TimeFields->Month = months - 1;
            TimeFields->Year = years + 1524;
        } else {
            TimeFields->Month = months - 13;
            TimeFields->Year = years + 1525;
        }
        /* calculation of day of month is based on the wonderful
         * sequence of INT( n * 30.6): it reproduces the 
         * 31-30-31-30-31-31 month lengths exactly for small n's */
        TimeFields->Day = yearday - (1959 * months) / 64 ;
}


BOOL WINAPI FileTimeToSystemTime( const FILETIME *ft, SYSTEMTIME * syst ) {
  TIME_FIELDS tf;
  LARGE_INTEGER t;

  TRACEN((printf("FileTimeToSystemTime\n")))
  t.QuadPart = ft->dwHighDateTime;
  t.QuadPart = (t.QuadPart << 32) | ft->dwLowDateTime;
  RtlTimeToTimeFields(&t, &tf);

  syst->wYear = tf.Year;
  syst->wMonth = tf.Month;
  syst->wDay = tf.Day;
  syst->wHour = tf.Hour;
  syst->wMinute = tf.Minute;
  syst->wSecond = tf.Second;
  syst->wMilliseconds = tf.Milliseconds;
  syst->wDayOfWeek = tf.Weekday;
  return TRUE;
}


static inline NTSTATUS WINAPI RtlLocalTimeToSystemTime( const LARGE_INTEGER *LocalTime,
    LARGE_INTEGER *SystemTime) {

  TRACEN((printf("RtlLocalTimeToSystemTime\n")))
  LONG bias = TIME_GetBias();
  SystemTime->QuadPart = LocalTime->QuadPart + bias * (LONGLONG)TICKSPERSEC;
  return STATUS_SUCCESS;
}

BOOL WINAPI LocalFileTimeToFileTime( const FILETIME *localft, FILETIME * utcft ) {
  LARGE_INTEGER local, utc;

  TRACEN((printf("LocalFileTimeToFileTime\n")))
  local.QuadPart = localft->dwHighDateTime;
  local.QuadPart = (local.QuadPart << 32) | localft->dwLowDateTime;
  RtlLocalTimeToSystemTime( &local, &utc );
  utcft->dwLowDateTime = (DWORD)utc.QuadPart;
  utcft->dwHighDateTime = (DWORD)(utc.QuadPart >> 32);

  return TRUE;
}

/*********************************************************************
 *      GetSystemTime                                   (KERNEL32.@)
 *
 * Get the current system time.
 *
 * RETURNS
 *  Nothing.
 */
VOID WINAPI GetSystemTime(SYSTEMTIME * systime) /* [O] Destination for current time */
{
  FILETIME ft;
  LARGE_INTEGER t;

  TRACEN((printf("GetSystemTime\n")))

  struct timeval now;
  gettimeofday( &now, 0 );
  t.QuadPart  = now.tv_sec * (ULONGLONG)TICKSPERSEC + TICKS_1601_TO_1970;
  t.QuadPart += now.tv_usec * 10;

  ft.dwLowDateTime  = (DWORD)(t.QuadPart);
  ft.dwHighDateTime = (DWORD)(t.QuadPart >> 32);
  FileTimeToSystemTime(&ft, systime);
}

/******************************************************************************
 *       RtlTimeFieldsToTime [NTDLL.@]
 *
 * Convert a TIME_FIELDS structure into a time.
 *
 * PARAMS
 *   ftTimeFields [I] TIME_FIELDS structure to convert.
 *   Time         [O] Destination for the converted time.
 *
 * RETURNS
 *   Success: TRUE.
 *   Failure: FALSE.
 */
static BOOLEAN WINAPI RtlTimeFieldsToTime(
  TIME_FIELDS * tfTimeFields,
  LARGE_INTEGER *Time)
{
  int month, year, cleaps, day;

  TRACEN((printf("RtlTimeFieldsToTime\n")))

	/* FIXME: normalize the TIME_FIELDS structure here */
        /* No, native just returns 0 (error) if the fields are not */
        if( tfTimeFields->Milliseconds< 0 || tfTimeFields->Milliseconds > 999 ||
                tfTimeFields->Second < 0 || tfTimeFields->Second > 59 ||
                tfTimeFields->Minute < 0 || tfTimeFields->Minute > 59 ||
                tfTimeFields->Hour < 0 || tfTimeFields->Hour > 23 ||
                tfTimeFields->Month < 1 || tfTimeFields->Month > 12 ||
                tfTimeFields->Day < 1 ||
                tfTimeFields->Day > MonthLengths
                    [ tfTimeFields->Month ==2 || IsLeapYear(tfTimeFields->Year)]
                    [ tfTimeFields->Month - 1] ||
                tfTimeFields->Year < 1601 )
            return FALSE;

        /* now calculate a day count from the date
         * First start counting years from March. This way the leap days
         * are added at the end of the year, not somewhere in the middle.
         * Formula's become so much less complicate that way.
         * To convert: add 12 to the month numbers of Jan and Feb, and 
         * take 1 from the year */
        if(tfTimeFields->Month < 3) {
            month = tfTimeFields->Month + 13;
            year = tfTimeFields->Year - 1;
        } else {
            month = tfTimeFields->Month + 1;
            year = tfTimeFields->Year;
        }
        cleaps = (3 * (year / 100) + 3) / 4;   /* nr of "century leap years"*/
        day =  (36525 * year) / 100 - cleaps + /* year * dayperyr, corrected */
                 (1959 * month) / 64 +         /* months * daypermonth */
                 tfTimeFields->Day -          /* day of the month */
                 584817 ;                      /* zero that on 1601-01-01 */
        /* done */
        
        Time->QuadPart = (((((LONGLONG) day * HOURSPERDAY +
            tfTimeFields->Hour) * MINSPERHOUR +
            tfTimeFields->Minute) * SECSPERMIN +
            tfTimeFields->Second ) * 1000 +
            tfTimeFields->Milliseconds ) * TICKSPERMSEC;

        return TRUE;
}

/*********************************************************************
 *      SystemTimeToFileTime                            (KERNEL32.@)
 */
BOOL WINAPI SystemTimeToFileTime( const SYSTEMTIME *syst, FILETIME * ft ) {
  TIME_FIELDS tf;
  LARGE_INTEGER t;

  TRACEN((printf("SystemTimeToFileTime\n")))

  tf.Year = syst->wYear;
  tf.Month = syst->wMonth;
  tf.Day = syst->wDay;
  tf.Hour = syst->wHour;
  tf.Minute = syst->wMinute;
  tf.Second = syst->wSecond;
  tf.Milliseconds = syst->wMilliseconds;

  RtlTimeFieldsToTime(&tf, &t);
  ft->dwLowDateTime = (DWORD)t.QuadPart;
  ft->dwHighDateTime = (DWORD)(t.QuadPart>>32);
  return TRUE;
}

/***********************************************************************
 *              GetSystemTimeAsFileTime  (KERNEL32.@)
 *
 *  Get the current time in utc format.
 *
 *  RETURNS
 *   Nothing.
 */
VOID WINAPI GetSystemTimeAsFileTime(
    FILETIME * time) /* [out] Destination for the current utc time */
{
    LARGE_INTEGER t;
/*
    NtQuerySystemTime( &t );
*/
    struct timeval now;

    gettimeofday( &now, 0 );
    t.QuadPart  = now.tv_sec * (ULONGLONG)TICKSPERSEC + TICKS_1601_TO_1970;
    t.QuadPart += now.tv_usec * 10;

    time->dwLowDateTime  = (DWORD)(t.QuadPart);
    time->dwHighDateTime = (DWORD)(t.QuadPart >> 32);
}

DWORD WINAPI GetTickCount(VOID) { // Retrieves the number of milliseconds
  timeval v;
  if (gettimeofday(&v, 0) == 0)
  {
    return (DWORD)(v.tv_sec * (UInt64)1000 + v.tv_usec / 1000);
  }  
  return (DWORD)time(0)*1000; 
}

