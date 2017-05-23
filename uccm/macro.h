
#pragma once

#define C_EVAL(_) _
#define C_COMPOSE2(_0,_1)       _0##_1
#define C_COMPOSE3(_0,_1,_2)	_0##_1##_2
#define C_COMPOSE4(_0,_1,_2,_3) _0##_1##_2##_3
#define C_ID(Name,Line)         C_COMPOSE4(__Label_,Name,_,Line)
#define C_LOCAL_ID(Name)        C_ID(Name,__LINE__)
#define C_CONCAT2(_0,_1)        C_COMPOSE2(_0,_1)
#define C_CONCAT3(_0,_1,_2)     C_COMPOSE3(_0,_1,_2)
#define C_CONCAT4(_0,_1,_2,_3)	C_COMPOSE4(_0,_1,_2,_3)
#define C_CONSTSTR(_)           #_
#define C_STR(_)                C_CONSTSTR(_)

// empty list has only one element NIL
// maximum list length is 16
#define C_COUNT_ELEMNTS(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12,_13,_14,_15,_16,_17,...) _17
#define C_LIST_LENGTH(...) C_COUNT_ELEMNTS( __VA_ARGS__, 16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0)

#define C_COMMA() ,
#define C_SEMICOLON() ;
#define C_SPACE()

#define C_APPLY_0(_Q,_N,Spacer,_0,...)
#define C_APPLY_1(_Q,_N,Spacer,_0,...)  _Q(_0,_N)
#define C_APPLY_2(_Q,_N,Spacer,_0,...)  _Q(_0,_N) Spacer() C_APPLY_1(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_3(_Q,_N,Spacer,_0,...)  _Q(_0,_N) Spacer() C_APPLY_2(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_4(_Q,_N,Spacer,_0,...)  _Q(_0,_N) Spacer() C_APPLY_3(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_5(_Q,_N,Spacer,_0,...)  _Q(_0,_N) Spacer() C_APPLY_4(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_6(_Q,_N,Spacer,_0,...)  _Q(_0,_N) Spacer() C_APPLY_5(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_7(_Q,_N,Spacer,_0,...)  _Q(_0,_N) Spacer() C_APPLY_6(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_8(_Q,_N,Spacer,_0,...)  _Q(_0,_N) Spacer() C_APPLY_7(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_9(_Q,_N,Spacer,_0,...)  _Q(_0,_N) Spacer() C_APPLY_8(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_10(_Q,_N,Spacer,_0,...) _Q(_0,_N) Spacer() C_APPLY_9(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_11(_Q,_N,Spacer,_0,...) _Q(_0,_N) Spacer() C_APPLY_10(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_12(_Q,_N,Spacer,_0,...) _Q(_0,_N) Spacer() C_APPLY_11(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_13(_Q,_N,Spacer,_0,...) _Q(_0,_N) Spacer() C_APPLY_12(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_14(_Q,_N,Spacer,_0,...) _Q(_0,_N) Spacer() C_APPLY_13(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_15(_Q,_N,Spacer,_0,...) _Q(_0,_N) Spacer() C_APPLY_14(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY_16(_Q,_N,Spacer,_0,...) _Q(_0,_N) Spacer() C_APPLY_15(_Q,(_N+1),Spacer,__VA_ARGS__)
#define C_APPLY(_Q,_N,Spacer,...)       C_CONCAT2(C_APPLY_,C_LIST_LENGTH(__VA_ARGS__))(_Q,_N,Spacer, __VA_ARGS__)

#define C_GETITEM_0(_0,...) _0
#define C_GETITEM_1(_0,_1,...) _1
#define C_GETITEM_2(_0,_1,_2,...) _2
#define C_GETITEM_3(_0,_1,_2,_3,...) _3
#define C_GETITEM_4(_0,_1,_2,_3,_4...) _4
#define C_GETITEM_5(_0,_1,_2,_3,_4,_5...) _5
#define C_GETITEM_6(_0,_1,_2,_3,_4,_5,_6,...) _6
#define C_GETITEM_7(_0,_1,_2,_3,_4,_5,_6,_7,...) _7
#define C_GETITEM_8(_0,_1,_2,_3,_4,_5,_6,_7,_8,...) _8
#define C_GETITEM_9(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,...) _9
#define C_GETITEM_10(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,...) _10
#define C_GETITEM_11(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,...) _11
#define C_GETITEM_12(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12,...) _12
#define C_GETITEM_13(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12,_13,...) _13
#define C_GETITEM_14(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12,_13,_14,...) _14
#define C_GETITEM_15(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12,_13,_14,_15,...) _15
#define C_GETITEM(N,List) C_EVAL(C_GETITEM_##N List)

#define C_MAP(_Q, Sps, ...) C_APPLY(_Q, 0, Sps, __VA_ARGS__)
#define C_UNWRAP(x) x
#define C_PUSHFRONT(_,List) (_, C_UNWRAP List)

/**
  T - type of node
  L - ptr to list
  O - processing list node
  E - end of list aka NULL or barrier
  */
#define C_SLIST_UNLINK(T,L,O,E) \
    for ( T **__l = &(L); *__l != (E); __l = &(*__l)->next ) \
        {if ( *__l == (O) ) { *__l = (*__l)->next; (O)->next = NULL; break; }}

/**
  P - predicate
  T - type of node
  L - ptr to list
  E - end of list aka NULL or barrier
  */
#define C_SLIST_UNLINK_WHEN(P,T,L,E) \
    for ( T **__l = &(L); *__l != (E);  ) \
    {   T *_ = *__l; \
        if ( P ) { *__l = (*__l)->next;} \
        else __l = &(*__l)->next; \
    }

/**
  P - predicate
  T - type of node
  L - ptr to list
  E - end of list aka NULL or barrier
  */
#define C_SLIST_UNLINK_WHEN_ONCE(P,T,L,E) \
    for ( T **__l = &(L); *__l != (E);  ) \
    {   T *_ = *__l; \
        if ( P ) { *__l = (*__l)->next; break; } \
        else __l = &(*__l)->next; \
    }

/**
  L - ptr to list
  O - processing list node
  E - end of list aka NULL or barrier
  */
#define C_SLIST_LINK_FRONT(L,O,E) do { (O)->next = (L); (L) = (O); } while(0)

/**
  T - type of node
  L - ptr to list
  O - processing list node
  E - end of list aka NULL or barrier
  */
#define C_SLIST_LINK_BACK(T,L,O,E) \
    do { T **__l = &(L); \
        while ( *__l != (E) ) __l = &(*__l)->next; \
        (O)->next = *__l; *__l = (O); \
    } while(0)

/**
  P - predicate
  T - type of node
  L - ptr to list
  O - processing list node
  E - end of list aka NULL or barrier
  */
#define C_SLIST_LINK_WHEN(P,T,L,O,E) \
    do { T **__l = &(L); T *_ = (L);\
        while ( *__l != (E) && !(P) ) { __l = &(*__l)->next; _ = *__l; }\
        (O)->next = *__l; *__l = (O); \
    } while(0)


#define C_BASE_OF(Type,Field,Ptr) \
    (Type*)((uintptr_t)(Ptr) - (uintptr_t)(&((Type*)0)->Field))
