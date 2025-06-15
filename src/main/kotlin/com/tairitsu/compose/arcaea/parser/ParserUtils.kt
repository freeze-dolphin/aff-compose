package com.tairitsu.compose.arcaea.parser

import com.tairitsu.compose.arcaea.Chart
import com.tairitsu.compose.arcaea.Difficulty
import com.tairitsu.compose.arcaea.antlr.ArcCreateChartParser
import com.tairitsu.compose.arcaea.antlr.ArcaeaChartParser
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.tree.TerminalNode

class ElseBranch(val isElseBranch: Boolean) {
    fun onElse(closure: Unit.() -> Unit) {
        if (isElseBranch) {
            closure.invoke(Unit)
        }
    }
}

class Executable(val isExecutable: Boolean) {
    fun exec(closure: Unit.() -> Unit): ElseBranch {
        if (isExecutable) {
            closure.invoke(Unit)
            return ElseBranch(false)
        } else {
            return ElseBranch(true)
        }
    }

    companion object {
        infix fun Executable.and(another: Executable): Executable = Executable(this.isExecutable && another.isExecutable)

        fun all(vararg executables: Executable): Executable = Executable(executables.all { it.isExecutable })
    }
}

class ContextConditioner<out T : ParserRuleContext>(val ctx: T) {
    fun notNull(closure: T.() -> TerminalNode?): Executable = Executable(closure(ctx) != null)
    fun ruleNotNull(closure: T.() -> ParserRuleContext?): Executable = Executable(closure(ctx) != null)

    fun allNotNull(vararg closure: T.() -> TerminalNode?): Executable = Executable(closure.all {
        notNull(it).isExecutable
    })

    fun ruleAllNotNull(vararg closure: T.() -> ParserRuleContext?): Executable = Executable(closure.all {
        ruleNotNull(it).isExecutable
    })

}

interface IArcCreateChartParser {
    fun parse(): Pair<Chart, ArcCreateChartParseReport>;
    fun processCommandInvocationContext(
        ctx: ArcCreateChartParser.Command_invocationContext,
        reporter: ArcCreateChartParseReport,
        tgName: String = "main",
    ): Difficulty.() -> Unit
}

interface IArcaeaChartParser {
    fun parse(): Chart;
    fun processCommandInvocationContext(
        ctx: ArcaeaChartParser.Command_invocationContext,
        tgName: String = "main",
    ): Difficulty.() -> Unit
}
