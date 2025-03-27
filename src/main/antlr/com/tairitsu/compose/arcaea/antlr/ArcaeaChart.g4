grammar ArcaeaChart;

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

// Keywords
K_timing           : 'timing';
K_hold             : 'hold';
K_timinggroup      : 'timinggroup';
K_arc              : 'arc';
K_scenecontrol     : 'scenecontrol';
K_camera           : 'camera';
K_arctap           : 'arctap';
K_sc_trackhide     : 'trackhide';
K_sc_trackshow     : 'trackshow';
K_sc_trackdisplay  : 'trackdisplay';
K_sc_redline       : 'redline';
K_sc_arcahvdistort : 'arcahvdistort';
K_sc_arcahvdebris  : 'arcahvdebris';
K_sc_hidegroup     : 'hidegroup';
K_sc_enwidencamera : 'enwidencamera';
K_sc_enwidenlanes  : 'enwidenlanes';

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

K_tg_noinput     : 'noinput';
K_tg_fadingholds : 'fadingholds';
K_tg_anglex      : 'anglex';
K_tg_angley      : 'angley';

K_designant : 'designant';

K_audiooffset              : 'AudioOffset';
K_timingpointdensityfactor : 'TimingPointDensityFactor';
K_version                  : 'Version';

// Vocabularies
fragment Digit      : '0' .. '9';
fragment Lower      : 'a' .. 'z';
fragment Upper      : 'A' .. 'Z';
fragment Alpha      : (Lower | Upper);
fragment StringPart : (Lower | Upper | Digit);

Int     : [-]? Digit+;
Float   : [-]? Digit+ DOT Digit+;
Boolean : ('true' | 'false');
Version : Digit (DOT Digit)+;

HeaderIdentifier : (Upper) (Lower)+;
LaneOrd          : '0' ..'5';

// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

chart_
    : header command_invocation+
    ;

arctap
    : K_arctap LPAREN Int RPAREN
    ;

header
    : (header_item+ divider)?
    ;

header_item
    : K_audiooffset COLON Int
    | K_timingpointdensityfactor COLON (Float | Int)
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

enum_scenecontrol_type_argument
    : K_sc_trackhide
    | K_sc_trackshow
    | K_sc_trackdisplay
    | K_sc_redline
    | K_sc_arcahvdistort
    | K_sc_arcahvdebris
    | K_sc_hidegroup
    | K_sc_enwidencamera
    | K_sc_enwidenlanes
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

enum_timinggroup_effects
    : K_tg_noinput
    | K_tg_fadingholds
    | K_tg_anglex
    | K_tg_angley
    ;

enum_camera_ease_type
    : K_curve_l
    | K_curve_s
    | K_curve_qi
    | K_curve_qo
    | K_camera_reset
    ;

single_timinggroup_argument
    : enum_timinggroup_effects Int?
    ;

compound_timinggroup_argument
    : LPAREN RPAREN // no effect
    | LPAREN (single_timinggroup_argument (UNDERLINE | RPAREN) | compound_timinggroup_argument)*
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
        Int COMMA Int COMMA Float COMMA Float COMMA enum_arcnote_curve_type COMMA Float COMMA Float COMMA Int COMMA (Hitsound | Alphas) COMMA (Boolean | K_designant)
    ) RPAREN SEMICOLON
    // with arctap(s)
    | K_arc LPAREN (
        Int COMMA Int COMMA Float COMMA Float COMMA enum_arcnote_curve_type COMMA Float COMMA Float COMMA Int COMMA (Hitsound | Alphas) COMMA (Boolean | K_designant)
    ) RPAREN compound_arctap_argument SEMICOLON
    ;

cmd_scenecontrol
    : K_scenecontrol LPAREN (Int COMMA enum_scenecontrol_type_argument (COMMA Float)? (COMMA Int)?) RPAREN SEMICOLON
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

Hitsound : StringPart+ '_wav';
Alphas : Alpha+;

NEWLINE: [\r\n] -> skip;
WS: [ \t\r\n]+ -> skip;

