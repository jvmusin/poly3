import kotlinx.css.*
import styled.StyleSheet

object Styles : StyleSheet("Styles") {
    val module by css {
        margin(0.5.em)
        padding(0.5.em)
        backgroundColor = Color.aliceBlue
    }

    val problemDetailsTable by css {
        width = 100.pct
        tableLayout = TableLayout.fixed
        marginLeft = LinearDimension.auto
        marginRight = LinearDimension.auto
    }
}