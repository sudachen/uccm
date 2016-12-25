
#pragma once

#define _EVAL(_) _
#define _COMPOSE2(_0,_1)	_0##_1
#define _COMPOSE3(_0,_1,_2)	_0##_1##_2
#define _COMPOSE4(_0,_1,_2,_3) _0##_1##_2##_3
#define _ID(Name,Line)		_COMPOSE4(__Label_,Name,_,Line)
#define _LOCAL_ID(Name)     _ID(Name,__LINE__)
#define _CONCAT2(_0,_1)		_COMPOSE2(_0,_1)
#define _CONCAT3(_0,_1,_2)	_COMPOSE3(_0,_1,_2)
#define _CONSTSTR(_)        #_
#define _STR(_)             _CONSTSTR(_)

#define _ARGS_COUNT_(_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12,_13,_14,_15,...) _15
#define _ARGS_COUNT(...) _ARGS_COUNT_(__VA_ARGS__,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0)

#define _COMMA() ,
#define _SEMICOLON() ;
#define _SPACE()

#define _APPLY_1(_Q,Spacer,_0)      _Q(_0)
#define _APPLY_2(_Q,Spacer,_0,...)  _Q(_0) Spacer() _APPLY_1(_Q,Spacer,__VA_ARGS__)
#define _APPLY_3(_Q,Spacer,_0,...)  _Q(_0) Spacer() _APPLY_2(_Q,Spacer,__VA_ARGS__)
#define _APPLY_4(_Q,Spacer,_0,...)  _Q(_0) Spacer() _APPLY_3(_Q,Spacer,__VA_ARGS__)
#define _APPLY_5(_Q,Spacer,_0,...)  _Q(_0) Spacer() _APPLY_4(_Q,Spacer,__VA_ARGS__)
#define _APPLY_6(_Q,Spacer,_0,...)  _Q(_0) Spacer() _APPLY_5(_Q,Spacer,__VA_ARGS__)
#define _APPLY_7(_Q,Spacer,_0,...)  _Q(_0) Spacer() _APPLY_6(_Q,Spacer,__VA_ARGS__)
#define _APPLY_8(_Q,Spacer,_0,...)  _Q(_0) Spacer() _APPLY_7(_Q,Spacer,__VA_ARGS__)
#define _APPLY_9(_Q,Spacer,_0,...)  _Q(_0) Spacer() _APPLY_8(_Q,Spacer,__VA_ARGS__)
#define _APPLY_10(_Q,Spacer,_0,...) _Q(_0) Spacer() _APPLY_9(_Q,Spacer,__VA_ARGS__)
#define _APPLY_11(_Q,Spacer,_0,...) _Q(_0) Spacer() _APPLY_10(_Q,Spacer,__VA_ARGS__)
#define _APPLY_12(_Q,Spacer,_0,...) _Q(_0) Spacer() _APPLY_11(_Q,Spacer,__VA_ARGS__)
#define _APPLY_13(_Q,Spacer,_0,...) _Q(_0) Spacer() _APPLY_12(_Q,Spacer,__VA_ARGS__)
#define _APPLY_14(_Q,Spacer,_0,...) _Q(_0) Spacer() _APPLY_13(_Q,Spacer,__VA_ARGS__)
#define _APPLY(_Q,Spacer,...) _CONCAT2(_APPLY_,_ARGS_COUNT(__VA_ARGS__))(_Q,Spacer,__VA_ARGS__)

#define _GETITEM_0(_0,...) _0
#define _GETITEM_1(_0,_1,...) _1
#define _GETITEM_2(_0,_1,_2,...) _2
#define _GETITEM_3(_0,_1,_2,_3,...) _3
#define _GETITEM_4(_0,_1,_2,_3,_4...) _4
#define _GETITEM_5(_0,_1,_2,_3,_4,_5...) _5
#define _GETITEM_6(_0,_1,_2,_3,_4,_5,_6,...) _6
#define _GETITEM_7(_0,_1,_2,_3,_4,_5,_6,_7,...) _7
#define _GETITEM_8(_0,_1,_2,_3,_4,_5,_6,_7,_8,...) _8
#define _GETITEM_9(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,...) _9
#define _GETITEM_10(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,...) _10
#define _GETITEM_11(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,...) _11
#define _GETITEM_12(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12,...) _12
#define _GETITEM_13(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12,_13,...) _13
#define _GETITEM_14(_0,_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12,_13,_14,...) _14
#define _GETITEM(N,List) _EVAL(_GETITEM_##N List)

#define _MAP(_Q, Sps, ...) _APPLY(_Q, Sps, __VA_ARGS__)
#define _UNWRAP(x) x
#define _PUSHFRONT(_,List) (_, _UNWRAP List)
