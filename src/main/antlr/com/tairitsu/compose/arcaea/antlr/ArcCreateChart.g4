grammar ArcCreateChart;

// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 0, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine true, allowShortBlocksOnASingleLine true, minEmptyLines 0, alignSemicolons ownLine
// $antlr-format alignColons trailing, singleLineOverrulesHangingColon true, alignLexerCommands true, alignLabels true, alignTrailers true

// Symbols
UNDERLINE : '_';
DASH      : '-';
DOT       : '.';
COMMA     : ',';
LPAREN    : '(';
RPAREN    : ')';
LSQUARE   : '[';
RSQUARE   : ']';
LCURL     : '{';
RCURL     : '}';
SEMICOLON : ';';
COLON     : ':';
EQL_SGN   : '=';

// Keywords
K_timing       : 'timing';
K_hold         : 'hold';
K_timinggroup  : 'timinggroup';
K_arc          : 'arc';
K_scenecontrol : 'scenecontrol';
K_camera       : 'camera';
K_arctap       : 'arctap';

K_curve_s    : 's';
K_curve_b    : 'b';
K_curve_si   : 'si';
K_curve_so   : 'so';
K_curve_sisi : 'sisi';
K_curve_siso : 'siso';
K_curve_sosi : 'sosi';
K_curve_soso : 'soso';

K_curve_l      : 'l';
K_curve_qi     : 'qi';
K_curve_qo     : 'qo';
K_camera_reset : 'reset';

K_timinggroup_name: 'name';

K_audiooffset              : 'AudioOffset';
K_timingpointdensityfactor : 'TimingPointDensityFactor';
K_version                  : 'Version';

// Vocabularies
fragment Digit : '0' .. '9';
fragment Lower : 'a' .. 'z';
fragment Upper : 'A' .. 'Z';
fragment Alpha : (Lower | Upper);
Int            : [-]? Digit+;
Float          : [-]? Digit+ DOT Digit+;
Boolean        : ('true' | 'false');
Version        : Digit (DOT Digit)+;

HeaderIdentifier : (Upper) (Lower)+;
LaneOrd          : '0' ..'5';

// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

chart_
    : header command_invocation+
    ;

hitsound
    : Lowers
    | 'none'
    ;

arctap
    : K_arctap LPAREN Int RPAREN
    ;

header
    : (header_item+ divider)?
    ;

header_item
    : K_audiooffset COLON Int
    | K_timingpointdensityfactor COLON Int
    | K_version COLON Version
    | K_version COLON (Float | Int)
    | HeaderIdentifier COLON Int
    ;

divider
    : DASH
    ;

command_invocation
    : cmd_timing
    | cmd_note
    | cmd_hold
    | cmd_arc
    | cmd_scenecontrol
    | cmd_timinggroup
    | cmd_camera
    ;

enum_arcnote_curve_type
    : K_curve_s
    | K_curve_b
    | K_curve_si
    | K_curve_so
    | K_curve_sisi
    | K_curve_siso
    | K_curve_sosi
    | K_curve_soso
    ;

compound_arctap_argument
    : LSQUARE (arctap (COMMA | RSQUARE) | compound_arctap_argument)*
    ;

enum_camera_ease_type
    : K_curve_l
    | K_curve_s
    | K_curve_qi
    | K_curve_qo
    | K_camera_reset
    ;

single_timinggroup_argument
    : K_timinggroup_name EQL_SGN String
    | Lowers (EQL_SGN Float)?
    ;

compound_timinggroup_argument
    : LPAREN RPAREN // no effect
    | LPAREN (single_timinggroup_argument (COMMA | RPAREN) | compound_timinggroup_argument)*
    ;

cmd_timing
    : K_timing LPAREN (Int COMMA Float COMMA Float) RPAREN SEMICOLON
    ;

cmd_note
    : LPAREN (Int COMMA Int) RPAREN SEMICOLON
    ;

cmd_hold
    : K_hold LPAREN (Int COMMA Int COMMA Int) RPAREN SEMICOLON
    ;

cmd_arc
    // without arctap
    : K_arc LPAREN (
        Int COMMA Int COMMA Float COMMA Float COMMA enum_arcnote_curve_type COMMA Float COMMA Float COMMA Int COMMA hitsound COMMA Boolean
    ) RPAREN SEMICOLON
    // with arctap(s)
    | K_arc LPAREN (
        Int COMMA Int COMMA Float COMMA Float COMMA enum_arcnote_curve_type COMMA Float COMMA Float COMMA Int COMMA hitsound COMMA Boolean
    ) RPAREN compound_arctap_argument SEMICOLON
    ;

cmd_scenecontrol
    : K_scenecontrol LPAREN (Int (COMMA Lowers) ((COMMA (String | Int | Float | Alphas))+)?) RPAREN SEMICOLON
    ;

cmd_timinggroup
    : K_timinggroup compound_timinggroup_argument LCURL command_invocation+ RCURL SEMICOLON
    ;

cmd_camera
    : K_camera LPAREN (
        Int COMMA Float COMMA Float COMMA Float COMMA Float COMMA Float COMMA Float COMMA enum_camera_ease_type COMMA Int
    ) RPAREN SEMICOLON
    ;

// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 0, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine true, allowShortBlocksOnASingleLine true, minEmptyLines 0, alignSemicolons ownLine
// $antlr-format alignColons trailing, singleLineOverrulesHangingColon true, alignLexerCommands true, alignLabels true, alignTrailers true

Lowers : Lower+;
Alphas : Alpha+;

String: '"' (Digit | Alpha | DOT | DASH | UNDERLINE)+ '"';

NEWLINE: [\r\n] -> skip;

WS: [ \t\r\n]+ -> skip;