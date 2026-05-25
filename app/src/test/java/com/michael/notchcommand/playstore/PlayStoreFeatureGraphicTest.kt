package com.michael.notchcommand.playstore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Category(PlayStoreScreenshotTests::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PlayStoreFeatureGraphicTest {

  @Test
  @Config(qualifiers = "w1024dp-h500dp-mdpi")
  fun feature_graphic() {
    capturePlayStoreImage("feature-graphic.png") {
      FeatureGraphicContent()
    }
  }
}

@Composable
fun FeatureGraphicContent() {
  val bgGradient = Brush.radialGradient(
    colors = listOf(Color(0xFF1E1035), Color(0xFF0B0314)),
    radius = 1200f
  )
  
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(bgGradient)
      .padding(40.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      modifier = Modifier.fillMaxSize(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(
        modifier = Modifier.weight(1.2f),
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = "NotchCommand",
          fontSize = 54.sp,
          fontWeight = FontWeight.Black,
          color = Color.White,
          fontFamily = FontFamily.SansSerif
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
          text = "Interactive Camera Ring & Status Visualizer",
          fontSize = 20.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF13E280),
          fontFamily = FontFamily.SansSerif
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
          text = "Transform your front camera punch-hole into an interactive shortcut launcher, gesture command center, and glowing battery status ring.",
          fontSize = 14.sp,
          fontWeight = FontWeight.Light,
          color = Color.White.copy(alpha = 0.7f),
          lineHeight = 20.sp,
          fontFamily = FontFamily.SansSerif
        )
      }
      
      Spacer(modifier = Modifier.width(32.dp))
      
      Box(
        modifier = Modifier
          .weight(0.8f)
          .fillMaxHeight(),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(220.dp)
            .border(
              width = 8.dp,
              brush = Brush.sweepGradient(
                colors = listOf(
                  Color(0xFF13E280),
                  Color(0xFFFFA500),
                  Color(0xFFFF3D00),
                  Color(0xFF13E280)
                )
              ),
              shape = CircleShape
            )
            .drawBehind {
              drawCircle(
                color = Color(0xFF13E280).copy(alpha = 0.15f),
                radius = size.minDimension / 2f + 16.dp.toPx()
              )
            },
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(140.dp)
              .clip(CircleShape)
              .background(Color(0xFF020005))
              .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    colors = listOf(Color(0xFF0F081D), Color(0xFF2C194D))
                  )
                )
            )
          }
        }
      }
    }
  }
}
