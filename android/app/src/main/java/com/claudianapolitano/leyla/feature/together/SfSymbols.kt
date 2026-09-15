package com.claudianapolitano.leyla.feature.together

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The catalog names its icons with SF Symbols, because the backend was written
 * against the iOS client. Rather than fork the catalog per platform (which
 * would have to stay in sync forever), the names are translated here — the
 * server keeps sending one icon vocabulary and each client renders it with its
 * own art.
 *
 * Anything unrecognised falls back to the sparkle rather than an empty slot,
 * so a catalog entry added tomorrow still draws something sensible today.
 */
fun sfSymbolIcon(name: String?): ImageVector = when (name) {
    "airplane" -> Icons.Filled.Flight
    "arrow.left.arrow.right" -> Icons.Filled.Shuffle
    "arrow.clockwise", "arrow.triangle.2.circlepath" -> Icons.Filled.Refresh
    "banknote.fill", "dollarsign.circle.fill" -> Icons.Filled.AccountBalance
    "bed.double.fill" -> Icons.Filled.Hotel
    "bolt.fill" -> Icons.Filled.Bolt
    "book.fill" -> Icons.AutoMirrored.Filled.MenuBook
    "brain.head.profile" -> Icons.Filled.Psychology
    "briefcase.fill" -> Icons.Filled.Work
    "bubble.left.and.bubble.right.fill", "text.bubble.fill" -> Icons.AutoMirrored.Filled.Message
    "building.2.fill" -> Icons.Filled.Apartment
    "calendar" -> Icons.Filled.CalendarMonth
    "camera.viewfinder", "camera.rotate" -> Icons.Filled.CameraAlt
    "car.fill" -> Icons.Filled.DirectionsCar
    "cart.fill" -> Icons.Filled.ShoppingCart
    "chart.line.uptrend.xyaxis" -> Icons.Filled.TrendingUp
    "checkmark", "checkmark.circle.fill", "checkmark.seal.fill" -> Icons.Filled.CheckCircle
    "clock.fill" -> Icons.Filled.AccessTimeFilled
    "cloud.rain.fill" -> Icons.Filled.Umbrella
    "creditcard.fill" -> Icons.Filled.CreditCard
    "crown.fill", "trophy.fill" -> Icons.Filled.EmojiEvents
    "cup.and.saucer.fill" -> Icons.Filled.LocalCafe
    "diamond.fill" -> Icons.Filled.Diamond
    "dice.fill" -> Icons.Filled.Casino
    "equal.circle.fill" -> Icons.Filled.AllInclusive
    "exclamationmark.bubble.fill" -> Icons.Filled.Campaign
    "eye.fill" -> Icons.Filled.Visibility
    "face.smiling" -> Icons.Filled.Mood
    "figure.run" -> Icons.AutoMirrored.Filled.DirectionsRun
    "flag.fill", "flag.checkered" -> Icons.Filled.Flag
    "flame.fill" -> Icons.Filled.Whatshot
    "fork.knife" -> Icons.Filled.Restaurant
    "gamecontroller.fill" -> Icons.Filled.SportsEsports
    "gearshape.fill" -> Icons.Filled.Settings
    "gift.fill" -> Icons.Filled.Redeem
    "globe.americas.fill" -> Icons.Filled.Public
    "graduationcap.fill" -> Icons.Filled.School
    "hand.raised.fill" -> Icons.Filled.BackHand
    "hands.clap.fill" -> Icons.Filled.VolunteerActivism
    "heart.circle.fill", "heart.fill" -> Icons.Filled.Favorite
    "hourglass" -> Icons.Filled.HourglassEmpty
    "house.fill" -> Icons.Filled.Home
    "infinity" -> Icons.Filled.AllInclusive
    "leaf.fill" -> Icons.Filled.Spa
    "lightbulb.fill" -> Icons.Filled.Lightbulb
    "list.bullet.clipboard.fill" -> Icons.AutoMirrored.Filled.ListAlt
    "lock.fill" -> Icons.Filled.Lock
    "map.fill" -> Icons.Filled.Map
    "mappin.and.ellipse" -> Icons.Filled.Place
    "moon.stars.fill" -> Icons.Filled.NightsStay
    "paintbrush.fill", "pencil.tip.crop.circle" -> Icons.Filled.Brush
    "paintpalette.fill" -> Icons.Filled.Palette
    "pawprint.fill" -> Icons.Filled.Pets
    "person.2.fill" -> Icons.Filled.Group
    "person.3.fill" -> Icons.Filled.Groups
    "person.fill" -> Icons.Filled.Person
    "photo" -> Icons.Filled.Image
    "play.fill", "play.rectangle.fill" -> Icons.Filled.PlayArrow
    "plus" -> Icons.Filled.Add
    "questionmark.circle.fill" -> Icons.AutoMirrored.Filled.HelpOutline
    "repeat" -> Icons.Filled.Refresh
    "scalemass.fill" -> Icons.Filled.MonitorWeight
    "shield.fill" -> Icons.Filled.Security
    "shuffle" -> Icons.Filled.Shuffle
    "snowflake" -> Icons.Filled.AcUnit
    "sparkle", "sparkles" -> Icons.Filled.AutoAwesome
    "square.grid.2x2.fill" -> Icons.Filled.Widgets
    "star.fill" -> Icons.Filled.Star
    "sun.max.fill" -> Icons.Filled.WbSunny
    "sunrise.fill" -> Icons.Filled.WbTwilight
    "target" -> Icons.Filled.GpsFixed
    "timer" -> Icons.Filled.WatchLater
    else -> Icons.Filled.AutoAwesome
}
