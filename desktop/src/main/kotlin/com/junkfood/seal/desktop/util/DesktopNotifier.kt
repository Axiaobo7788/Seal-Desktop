package com.junkfood.seal.desktop.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DesktopNotifier {
    suspend fun sendNotification(title: String, message: String) {
        withContext(Dispatchers.IO) {
            try {
                val os = System.getProperty("os.name").lowercase()
                when {
                    os.contains("win") -> sendWindowsNotification(title, message)
                    os.contains("mac") -> sendMacNotification(title, message)
                    os.contains("nix") || os.contains("nux") || os.contains("aix") -> sendLinuxNotification(title, message)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendWindowsNotification(title: String, message: String) {
        val script = """
            [Reflection.Assembly]::LoadWithPartialName("System.Windows.Forms");
            ${'$'}notify = New-Object system.windows.forms.notifyicon;
            ${'$'}notify.icon = [System.Drawing.SystemIcons]::Information;
            ${'$'}notify.visible = ${'$'}true;
            ${'$'}notify.showBalloonTip(10, "$title", "$message", [system.windows.forms.tooltipicon]::None);
        """.trimIndent()
        val encodedBytes = java.util.Base64.getEncoder().encode(script.toByteArray(Charsets.UTF_16LE))
        val encodedString = String(encodedBytes)
        Runtime.getRuntime().exec(arrayOf("powershell", "-EncodedCommand", encodedString))
    }

    private fun sendMacNotification(title: String, message: String) {
        val script = "display notification \"$message\" with title \"$title\""
        Runtime.getRuntime().exec(arrayOf("osascript", "-e", script))
    }

    private fun sendLinuxNotification(title: String, message: String) {
        val desktop = System.getenv("XDG_CURRENT_DESKTOP")?.uppercase() ?: ""
        
        val commands = mutableListOf<Array<String>>()
        
        val safeTitle = title.replace("\"", "'")
        val safeMessage = message.replace("\"", "'")

        // 1. Try gdbus (Universally supported on modern Linux via glib2)
        commands.add(
            arrayOf(
                "gdbus",
                "call",
                "--session",
                "--dest", "org.freedesktop.Notifications",
                "--object-path", "/org/freedesktop/Notifications",
                "--method", "org.freedesktop.Notifications.Notify",
                "\"Seal\"",
                "0",
                "\"\"",
                "\"$safeTitle\"",
                "\"$safeMessage\"",
                "[]",
                "{}",
                "5000"
            )
        )

        // 2. Try Python DBus (Another universal approach if gdbus is missing)
        val pythonScript = """
import sys
try:
    import dbus
    bus = dbus.SessionBus()
    notify = dbus.Interface(bus.get_object('org.freedesktop.Notifications', '/org/freedesktop/Notifications'), 'org.freedesktop.Notifications')
    notify.Notify('Seal', 0, '', sys.argv[1], sys.argv[2], [], {}, 5000)
except Exception:
    sys.exit(1)
        """.trimIndent()
        commands.add(arrayOf("python3", "-c", pythonScript, title, message))

        // 3. Fallbacks based on desktop environment wrappers
        if (desktop.contains("KDE")) {
            commands.add(arrayOf("kdialog", "--title", title, "--passivepopup", message, "5"))
            commands.add(arrayOf("notify-send", title, message))
        } else {
            commands.add(arrayOf("notify-send", title, message))
            commands.add(arrayOf("kdialog", "--title", title, "--passivepopup", message, "5"))
        }
        commands.add(arrayOf("zenity", "--notification", "--text=$title\n$message"))

        for (command in commands) {
            try {
                val process = Runtime.getRuntime().exec(command)
                if (process.waitFor() == 0) {
                    return
                }
            } catch (e: Exception) {
                // Process failed to start, try the next one
            }
        }
    }
}
