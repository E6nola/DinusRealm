package id.dinusrealm.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hand-crafted ultra-lightweight 24dp outline vector icons
 * so the APK avoids pulling in the 30MB+ material-icons-extended library.
 */
object DinusIcons {

    private fun strokeIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply(block).build()

    val Home: ImageVector by lazy {
        strokeIcon("Home") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 10.5f); lineTo(12f, 3f); lineTo(21f, 10.5f)
                moveTo(5f, 9.5f); lineTo(5f, 20f); lineTo(19f, 20f); lineTo(19f, 9.5f)
                moveTo(9.5f, 20f); lineTo(9.5f, 14f); lineTo(14.5f, 14f); lineTo(14.5f, 20f)
            }
        }
    }

    val Calendar: ImageVector by lazy {
        strokeIcon("Calendar") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 6f); lineTo(20f, 6f); lineTo(20f, 20f); lineTo(4f, 20f); close()
                moveTo(8f, 3f); lineTo(8f, 6f)
                moveTo(16f, 3f); lineTo(16f, 6f)
                moveTo(4f, 10f); lineTo(20f, 10f)
            }
        }
    }

    val Academic: ImageVector by lazy {
        strokeIcon("Academic") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(2f, 9f); lineTo(12f, 4f); lineTo(22f, 9f); lineTo(12f, 14f); close()
                moveTo(6f, 11.2f); lineTo(6f, 16.5f); curveTo(6f, 18.2f, 8.7f, 19.5f, 12f, 19.5f); curveTo(15.3f, 19.5f, 18f, 18.2f, 18f, 16.5f); lineTo(18f, 11.2f)
                moveTo(22f, 9f); lineTo(22f, 15f)
            }
        }
    }

    val Wallet: ImageVector by lazy {
        strokeIcon("Wallet") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 7f); lineTo(19f, 7f); lineTo(19f, 19f); lineTo(3f, 19f); close()
                moveTo(3f, 7f); lineTo(5f, 4f); lineTo(17f, 4f)
                moveTo(15f, 13f); lineTo(21f, 13f); lineTo(21f, 15f); lineTo(15f, 15f); close()
            }
        }
    }

    val CheckCircle: ImageVector by lazy {
        strokeIcon("CheckCircle") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 3f); curveTo(17f, 3f, 21f, 7f, 21f, 12f); curveTo(21f, 17f, 17f, 21f, 12f, 21f); curveTo(7f, 21f, 3f, 17f, 3f, 12f); curveTo(3f, 7f, 7f, 3f, 12f, 3f)
                moveTo(8.5f, 12.2f); lineTo(10.8f, 14.5f); lineTo(15.8f, 9.5f)
            }
        }
    }

    val Bell: ImageVector by lazy {
        strokeIcon("Bell") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(18f, 16f); lineTo(18f, 10f); curveTo(18f, 6.7f, 15.3f, 4f, 12f, 4f); curveTo(8.7f, 4f, 6f, 6.7f, 6f, 10f); lineTo(6f, 16f); lineTo(4f, 18f); lineTo(20f, 18f); close()
                moveTo(10f, 18f); curveTo(10f, 19.5f, 10.9f, 20.5f, 12f, 20.5f); curveTo(13.1f, 20.5f, 14f, 19.5f, 14f, 18f)
            }
        }
    }

    val Clock: ImageVector by lazy {
        strokeIcon("Clock") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 3f); curveTo(17f, 3f, 21f, 7f, 21f, 12f); curveTo(21f, 17f, 17f, 21f, 12f, 21f); curveTo(7f, 21f, 3f, 17f, 3f, 12f); curveTo(3f, 7f, 7f, 3f, 12f, 3f)
                moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15.2f, 14f)
            }
        }
    }

    val MapPin: ImageVector by lazy {
        strokeIcon("MapPin") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 21f); curveTo(12f, 21f, 18.5f, 14.8f, 18.5f, 10f); curveTo(18.5f, 6.4f, 15.6f, 3.5f, 12f, 3.5f); curveTo(8.4f, 3.5f, 5.5f, 6.4f, 5.5f, 10f); curveTo(5.5f, 14.8f, 12f, 21f, 12f, 21f)
                moveTo(12f, 8.2f); curveTo(13f, 8.2f, 13.8f, 9f, 13.8f, 10f); curveTo(13.8f, 11f, 13f, 11.8f, 12f, 11.8f); curveTo(11f, 11.8f, 10.2f, 11f, 10.2f, 10f); curveTo(10.2f, 9f, 11f, 8.2f, 12f, 8.2f)
            }
        }
    }

    val User: ImageVector by lazy {
        strokeIcon("User") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 4f); curveTo(14.2f, 4f, 16f, 5.8f, 16f, 8f); curveTo(16f, 10.2f, 14.2f, 12f, 12f, 12f); curveTo(9.8f, 12f, 8f, 10.2f, 8f, 8f); curveTo(8f, 5.8f, 9.8f, 4f, 12f, 4f)
                moveTo(4.5f, 20f); curveTo(4.5f, 16.5f, 7.8f, 14.5f, 12f, 14.5f); curveTo(16.2f, 14.5f, 19.5f, 16.5f, 19.5f, 20f)
            }
        }
    }

    val Settings: ImageVector by lazy {
        strokeIcon("Settings") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 9f); curveTo(13.7f, 9f, 15f, 10.3f, 15f, 12f); curveTo(15f, 13.7f, 13.7f, 15f, 12f, 15f); curveTo(10.3f, 15f, 9f, 13.7f, 9f, 12f); curveTo(9f, 10.3f, 10.3f, 9f, 12f, 9f)
                moveTo(12f, 3f); lineTo(12f, 5.2f)
                moveTo(12f, 18.8f); lineTo(12f, 21f)
                moveTo(3f, 12f); lineTo(5.2f, 12f)
                moveTo(18.8f, 12f); lineTo(21f, 12f)
                moveTo(5.6f, 5.6f); lineTo(7.2f, 7.2f)
                moveTo(16.8f, 16.8f); lineTo(18.4f, 18.4f)
                moveTo(18.4f, 5.6f); lineTo(16.8f, 7.2f)
                moveTo(7.2f, 16.8f); lineTo(5.6f, 18.4f)
            }
        }
    }

    val Refresh: ImageVector by lazy {
        strokeIcon("Refresh") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(20f, 12f); curveTo(20f, 16.4f, 16.4f, 20f, 12f, 20f); curveTo(7.6f, 20f, 4f, 16.4f, 4f, 12f); curveTo(4f, 7.6f, 7.6f, 4f, 12f, 4f); curveTo(15f, 4f, 17.6f, 5.6f, 19f, 8f)
                moveTo(19f, 4f); lineTo(19f, 8f); lineTo(15f, 8f)
            }
        }
    }

    val Copy: ImageVector by lazy {
        strokeIcon("Copy") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9f, 9f); lineTo(20f, 9f); lineTo(20f, 20f); lineTo(9f, 20f); close()
                moveTo(5f, 15f); lineTo(4f, 15f); lineTo(4f, 4f); lineTo(15f, 4f); lineTo(15f, 5f)
            }
        }
    }

    val ChevronRight: ImageVector by lazy {
        strokeIcon("ChevronRight") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9f, 6f); lineTo(15f, 12f); lineTo(9f, 18f)
            }
        }
    }

    val Back: ImageVector by lazy {
        strokeIcon("Back") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(15f, 6f); lineTo(9f, 12f); lineTo(15f, 18f)
            }
        }
    }

    val Sun: ImageVector by lazy {
        strokeIcon("Sun") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 8f); curveTo(14.2f, 8f, 16f, 9.8f, 16f, 12f); curveTo(16f, 14.2f, 14.2f, 16f, 12f, 16f); curveTo(9.8f, 16f, 8f, 14.2f, 8f, 12f); curveTo(8f, 9.8f, 9.8f, 8f, 12f, 8f)
                moveTo(12f, 3f); lineTo(12f, 5f)
                moveTo(12f, 19f); lineTo(12f, 21f)
                moveTo(3f, 12f); lineTo(5f, 12f)
                moveTo(19f, 12f); lineTo(21f, 12f)
            }
        }
    }

    val Moon: ImageVector by lazy {
        strokeIcon("Moon") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(20f, 14.5f); curveTo(18.8f, 15f, 17.5f, 15.2f, 16.1f, 15f); curveTo(12.2f, 14.4f, 9.4f, 10.9f, 9.8f, 7f); curveTo(9.9f, 5.7f, 10.4f, 4.5f, 11.2f, 3.5f); curveTo(7f, 4f, 3.8f, 7.6f, 3.8f, 12f); curveTo(3.8f, 16.7f, 7.6f, 20.5f, 12.3f, 20.5f); curveTo(16f, 20.5f, 19f, 18f, 20f, 14.5f)
            }
        }
    }

    val LogOut: ImageVector by lazy {
        strokeIcon("LogOut") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(10f, 4f); lineTo(5f, 4f); lineTo(5f, 20f); lineTo(10f, 20f)
                moveTo(16f, 8f); lineTo(20f, 12f); lineTo(16f, 16f)
                moveTo(10f, 12f); lineTo(20f, 12f)
            }
        }
    }

    val Shield: ImageVector by lazy {
        strokeIcon("Shield") {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 3f); lineTo(19f, 6f); lineTo(19f, 11.5f); curveTo(19f, 16f, 15.8f, 19.5f, 12f, 21f); curveTo(8.2f, 19.5f, 5f, 16f, 5f, 11.5f); lineTo(5f, 6f); close()
                moveTo(9.5f, 12f); lineTo(11.3f, 13.8f); lineTo(14.8f, 10.2f)
            }
        }
    }
}
