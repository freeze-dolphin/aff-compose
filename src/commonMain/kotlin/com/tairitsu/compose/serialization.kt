package com.tairitsu.compose

import com.tairitsu.compose.arcaea.antlr.UniversalAffChartParser
import com.tairitsu.compose.arcaea.antlr.UniversalAffChartVisitor
import org.antlr.v4.kotlinruntime.BaseErrorListener
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer
import org.antlr.v4.kotlinruntime.tree.AbstractParseTreeVisitor
import org.antlr.v4.kotlinruntime.tree.ErrorNode
import org.antlr.v4.kotlinruntime.tree.TerminalNode

fun Double.isInteger(): Boolean = this % 1 == 0.0

data class SerializationContext(
    val chart: Chart,
    val timingGroup: TimingGroup,
)

data class ParseContext(
    val chart: Chart,
    val timingGroup: TimingGroup,
)

class UniversalChartVisitor : UniversalAffChartVisitor<Any>, AbstractParseTreeVisitor<Any>() {

    data class Value(
        val raw: String,
        val type: ValueType,
        val stringValue: String? = null,
        val algebraicValue: Number? = null,
        val keyValuePair: Pair<String, Value>? = null,
    ) {
        val str: String get() = stringValue!!
        val num: Number get() = algebraicValue!!
        val kv: Pair<String, Value> get() = keyValuePair!!
    }

    enum class ValueType { STRING, ALGEBRAIC, KEY_VALUE }
    enum class OperatorType(val sign: Char) { ADD('+'), SUB('-'), MUL('*'), DIV('/'), POW('^'), REM('%') }

    data class Event(
        val name: String?,
        val values: List<Value>,
        val subEvents: List<Event>,
        val segment: Body?,
    )

    data class Body(val events: List<Event>)

    override fun visitChart(ctx: UniversalAffChartParser.ChartContext): Body {
        return visitBody(ctx.body())
    }

    override fun visitValue(ctx: UniversalAffChartParser.ValueContext): Value {
        requireNotNull(ctx.start)

        return when {
            ctx.String() != null -> {
                val raw = ctx.String()!!.text
                val content = raw.removeSurrounding("'", "'").removeSurrounding("\"", "\"")
                Value(raw, ValueType.STRING, stringValue = content)
            }

            ctx.Word() != null -> {
                val raw = ctx.Word()!!.text
                Value(raw, ValueType.STRING, stringValue = raw)
            }

            ctx.Int() != null -> {
                val raw = ctx.Int()!!.text
                val num = raw.toLong()
                Value(raw, ValueType.ALGEBRAIC, algebraicValue = num)
            }

            ctx.Float() != null -> {
                val raw = ctx.Float()!!.text
                val num = raw.toDouble()
                Value(raw, ValueType.ALGEBRAIC, algebraicValue = num)
            }

            ctx.Word() != null && ctx.Equal() != null && ctx.value() != null -> {
                val key = ctx.Word()!!.text
                val value = visitValue(ctx.value()!!)
                Value("$key=${value.raw}", ValueType.KEY_VALUE, keyValuePair = key to value)
            }

            else -> error("Unknown value type at ${ctx.start!!.line}:${ctx.start!!.charPositionInLine}")
        }
    }

    override fun visitValues(ctx: UniversalAffChartParser.ValuesContext): List<Value> {
        return ctx.value().map { visitValue(it) }
    }

    override fun visitEvent(ctx: UniversalAffChartParser.EventContext): Event {
        val name = ctx.Word()?.text

        val ctxValues = ctx.values()
        val values = visitValues(ctxValues)

        val ctxSubEvents = ctx.subEvents()
        val subEvents = if (ctxSubEvents != null) visitSubEvents(ctxSubEvents) else emptyList()

        val ctxSegment = ctx.segment()
        val segment = if (ctxSegment != null) visitSegment(ctxSegment) else null

        return Event(name, values, subEvents, segment)
    }

    override fun visitItem(ctx: UniversalAffChartParser.ItemContext): Event {
        return visitEvent(ctx.event())
    }

    override fun visitSubEvents(ctx: UniversalAffChartParser.SubEventsContext): List<Event> {
        return ctx.event().map { visitEvent(it) }
    }

    override fun visitSegment(ctx: UniversalAffChartParser.SegmentContext): Body {
        return visitBody(ctx.body())
    }

    override fun visitBody(ctx: UniversalAffChartParser.BodyContext): Body {
        val items = ctx.item().map { visitItem(it) }
        return Body(items)
    }

    override fun visitTerminal(node: TerminalNode): Any = Unit
    override fun visitErrorNode(node: ErrorNode): Any = Unit
    override fun defaultResult(): Any = Unit

}

interface ChartSerializer {
    fun serialize(chart: Chart): List<String>
    fun serializeTimingGroup(timingGroup: TimingGroup, ctx: SerializationContext): List<String>
    fun serializeTimedObject(timedObject: TimedObject, ctx: SerializationContext): List<String>
}

interface ChartParser {
    fun getEventParser(): (UniversalChartVisitor.Event, ParseContext) -> Unit

    fun parse(content: String): Chart
}

object ExceptionErrorListener : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?,
    ) {
        error("Unable to parse at: $line:$charPositionInLine, $msg")
    }
}



