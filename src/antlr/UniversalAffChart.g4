/**
 * Based on Arcade-plus
 *
 * @source https://github.com/yojohanshinwataikei/Arcade-plus/blob/master/Assets/Scripts/Aff/ArcaeaFileFormat.g4
 * @author yojohanshinwataikei
 */
grammar UniversalAffChart;

Whitespace: [\p{White_Space}] -> skip;

LParen    : '(';
RParen    : ')';
LBrack    : '[';
RBrack    : ']';
LBrace    : '{';
RBrace    : '}';
Comma     : ',';
Semicolon : ';';
Equal     : '=';
Operator  : '+' | '-' | '*' | '/' | '%' | '^';

fragment SQUOTE     : '\'';
fragment DQUOTE     : '"';
fragment UNDERLINE  : '_';
fragment SHARP      : '#';
fragment ALPHABET   : [a-zA-Z];
fragment DIGITSTART : [1-9];
fragment ZERO       : '0';
fragment DIGIT      : DIGITSTART | ZERO;
fragment DOT        : '.';
fragment NEGATIVE   : '-';
fragment SPACE      : ' ';

chart: body EOF;

value     : String | Word | Int | Float | (Word Equal value);
values    : LParen (value (Comma value)*)? RParen;
event     : Word? values subEvents? properties? segment?;
item      : event Semicolon;
property  : Word (Equal value)?;
properties: LBrace (property (Comma property)*)? RBrace;
subEvents : LBrack (event (Comma event)*)? RBrack;
segment   : LBrace body RBrace;
body      : item*;

String : SQUOTE (SHARP | UNDERLINE | ALPHABET | DIGIT | DOT | SPACE)* SQUOTE
       | DQUOTE (SHARP | UNDERLINE | ALPHABET | DIGIT | DOT | SPACE)* DQUOTE;
Word   : (SHARP | UNDERLINE | ALPHABET) (SHARP | UNDERLINE | ALPHABET | DIGIT)*;
Int    : NEGATIVE? (ZERO | DIGITSTART DIGIT*);
Float  : Int DOT DIGIT+;