
#include <uccm/board.h>
#include <string.h>
#include <ctype.h>
#include <math.h>

bool uccm$criticalEnter()
{
#if defined __nRF5x_UC__ && defined SOFTDEVICE_PRESENT
    uint8_t nestedCriticalReqion = 0;
    sd_nvic_critical_region_enter(&nestedCriticalReqion);
    return nestedCriticalReqion;
#else
    uint32_t primask = __get_PRIMASK();
    if ( primask )
    {
        return true;
    }
    else
    {
        __disable_irq();
        return false;
    }
#endif
}

bool uccm$criticalExit(bool nested)
{
#if defined __nRF5x_UC__ && defined SOFTDEVICE_PRESENT
    sd_nvic_critical_region_exit(nested);
#else
    if ( !nested ) __enable_irq();
#endif
}

void ucFatalError(uint32_t where)
{
  ucError("FATAL ERROR occured");
  for(;;) __WFE();
}

__Weak
void ucPutS(const char* text, bool complete)
{
    (void)text;
}

struct { uint8_t c; char bf[15]; bool complete; } uccm$printBuf = { 0, };

__Inline
void uccm$printChar(char c)
{
    if ( uccm$printBuf.c == sizeof(uccm$printBuf.bf)-1 )
    {
        ucPutS(uccm$printBuf.bf,uccm$printBuf.complete);
        uccm$printBuf.c = 0;
    }
    uccm$printBuf.bf[uccm$printBuf.c++] = c;
}

__Inline
void uccm$printStr(const char *s)
{
    while (*s) uccm$printChar(*s++);
}

__Inline
void uccm$flush()
{
    if ( uccm$printBuf.c )
    {
        uccm$printBuf.bf[uccm$printBuf.c] = 0;
        ucPutS(uccm$printBuf.bf,uccm$printBuf.complete);
        uccm$printBuf.c = 0;
    }
}

struct UcFormatOpt
{
    int width1;
    int width2;
    bool zfiller;
    bool uppercase;
    char fmt;
};

void uccm$printUnsigned10(uint32_t value)
{
    int i = 0, q;

    char bf[11];

    while(value)
    {
        q = value%10;
        value/=10;
        bf[i++] = '0'+q;
    }

    while ( i-- ) uccm$printChar(bf[i]);
}

void uccm$printUnsigned16(uint32_t value, size_t width)
{
    int i, skip, q;
    static const char f0[] = "0123456789abcdef";

    if ( width > 8 ) width = 8;
    skip = 8 - width;
    if ( skip == 8 ) skip = 7;
    for ( i = 0; i < 8; ++i )
    {
        q = (value >> (4*8-4)) & 0x0f;
        if ( q || !skip )
        {
            skip = 0;
            uccm$printChar(f0[q]);
        }
        else --skip;
        value <<= 4;
    }
}

void uccm$printInteger(int sign,UcFormatOpt *opt,UcFormatParam *param)
{
    if ( opt->fmt == 'd' || opt->fmt == 'u' || opt->fmt == '?' )
    {
        if ( sign < 0 && (opt->fmt == 'd' || opt->fmt == '?') && param->v.i < 0 )
        {
            uccm$printChar('-');
            uccm$printUnsigned10(-param->v.i);
        }
        else
            uccm$printUnsigned10(param->v.u);
    }
    else if ( opt->fmt == 'x' )
        uccm$printUnsigned16(param->v.u,opt->zfiller?opt->width1:0);
    else if ( opt->fmt == 'p' )
    {
        uccm$printChar('#');
        uccm$printUnsigned16(param->v.u,opt->zfiller?opt->width1:0);
    }
    else
        uccm$printStr("<invalid>");
}

void uccm$print32u(UcFormatOpt *opt,UcFormatParam *param)
{
    uccm$printInteger(0,opt,param);
}

void uccm$print32i(UcFormatOpt *opt,UcFormatParam *param)
{
    uccm$printInteger(-1,opt,param);
}

void uccm$printOneChar(UcFormatOpt *opt,UcFormatParam *param)
{
    if ( opt->fmt == '?' || opt->fmt == 'c' )
        uccm$printChar((char)param->v.u);
    else
        uccm$printInteger(0,opt,param);
}

void uccm$printCstr(UcFormatOpt *opt,UcFormatParam *param)
{
    const char *s = param->v.str;
    if ( s == NULL ) uccm$printStr("<null>");
    uccm$printStr(s);
}

void uccm$printPtr(UcFormatOpt *opt,UcFormatParam *param)
{
    UcFormatOpt opt1 = *opt;
    if ( opt->fmt == '?' ) opt1.fmt = 'p';
    opt1.zfiller = true;
    opt1.width1 = 8;
    uccm$printInteger(0,&opt1,param);
}

void uccm$print32f(UcFormatOpt *opt,UcFormatParam *param)
{
    int n = 6;
    float v = param->v.f;
    if ( v < 0 ) { v = -v; uccm$printChar('-'); }
    uccm$printUnsigned10((uint32_t)v);
    if ( opt->width2 )
    {
        v = v - floor(v);
        if ( opt->width2 > 0 )
        {
            n = opt->width2;
            while ( n-- ) v *= 10;
        }
        else
        {
            n = 6;
            while ( n-- && (v - floor(v)) > 0.00001 ) v *= 10;
        }
        uccm$printUnsigned10((uint32_t)v);
    }
}

void ucPrintF(size_t argno, const char *fmt, int flags, UcFormatParam *params)
{
    UcFormatOpt opt;

    bool nested = uccm$criticalEnter();
    uccm$printBuf.complete = !!(flags&2);

    for ( int j = 0 ; *fmt ; )
    {
        if ( *fmt == '%' && fmt[1] && fmt[1] != '%' )
        {
            memset(&opt,0,sizeof(opt));
            opt.width1 = -1;
            opt.width2 = -1;
            ++fmt;
            if ( *fmt == '0' && isdigit(fmt[1]) ) { opt.zfiller = true; ++fmt; }
            if ( isdigit(*fmt) ) opt.width1 = strtol(fmt,(char**)&fmt,10);
            if ( *fmt == '.' )
            {
                ++fmt;
                if ( isdigit(*fmt) ) opt.width2 = strtol(fmt,(char**)&fmt,10);
            }
            if ( isupper(*fmt) ) opt.uppercase = true;
            opt.fmt = tolower(*fmt++);
            if ( j < argno )
            {
                params[j].print(&opt,params+j);
                ++j;
            }
            else
                uccm$printStr("<no param>");

        }
        else if ( *fmt == '%' && fmt[1] == '%' )
        {
            uccm$printChar('%');
            fmt+=2;
        }
        else
        {
            uccm$printChar(*fmt);
            ++fmt;
        }
    }

    if ( flags&1 ) uccm$printChar('\n');

    uccm$flush();
    uccm$criticalExit(nested);
}

void uccm$assertFailed(const char *text, const char *file, int line)
{
    ucError("ASSERT: %? \n\tat %?:%?", $s(text), $s(file), $i(line));
    ucFatalError(UC_ERROR_IN_ASSERT);
}
