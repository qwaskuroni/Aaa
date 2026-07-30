package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GuideScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121824))
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("btn_guide_back")) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "How to Use Visual Auto Clicker",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        GuideStepCard(
            stepNumber = "1",
            title = "Grant Overlay & Accessibility Permissions",
            description = "Enable 'Display over other apps' so the floating control bar can draw over target applications. Enable 'Visual Auto Clicker' in Android Accessibility Settings to allow programmatically dispatching tap & swipe gestures."
        )

        Spacer(modifier = Modifier.height(12.dp))

        GuideStepCard(
            stepNumber = "2",
            title = "Position Floating Target Badges",
            description = "Tap '+' to add target points. Drag target circles (1, 2, 3...) anywhere on screen over buttons or game UI elements you want to tap automatically."
        )

        Spacer(modifier = Modifier.height(12.dp))

        GuideStepCard(
            stepNumber = "3",
            title = "Configure Actions (Text Input, Back, Swipes, Delays)",
            description = "Tap on any target badge (#1, #2, #3...) to customize its action type. Select 'Tap' for clicking buttons (e.g. Back button or Send button), or 'Text Input' to automatically type message text into input fields! Set custom delays in milliseconds between steps."
        )

        Spacer(modifier = Modifier.height(12.dp))

        GuideStepCard(
            stepNumber = "4",
            title = "Sequential Continuous Execution & Looping",
            description = "Press Play ▶ on the floating bar. The clicker executes Target 1 -> Target 2 -> Target 3 (Text Input) -> Target 4 (Send) in exact sequence. Once the final target finishes, it seamlessly loops back to Target 1 continuously!"
        )
    }
}

@Composable
fun GuideStepCard(stepNumber: String, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
