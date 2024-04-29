// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

grammar Properties;

parse
    : line* EOF
    ;

line
    : Space* (keyValue | LineBreak)
    ;

keyValue
    : key separatorAndValue eol
    ;

key
    : keyChar+
    ;

keyChar
    : AlphaNum
    ;

separatorAndValue
    : (Space | Equals) chars+
    ;

chars
    : AlphaNum
    | Space
    | Equals
    ;

eol
    : LineBreak
    | EOF
    ;

Equals
    : '='
    ;

LineBreak
    : '\r\n'? '\n'
    | '\r\n'
    ;

Space
    : ' '
    | '\t'
    | '\f'
    ;

AlphaNum
    : 'a' ..'z'
    | 'A' ..'Z'
    | '0' ..'9'
    ;