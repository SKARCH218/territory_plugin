package kr.skarch.territory_Plugin.commands

import kr.skarch.territory_Plugin.Territory_Plugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TerritoryTabCompleter(private val plugin: Territory_Plugin) : TabCompleter {

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (sender !is Player) return null

        return when (args.size) {
            1 -> {
                // First argument - command list
                val commands = mutableListOf(
                    "info", "upgrade", "team", "stats", "ranking",
                    "find", "stones", "history", "score", "scoreNow"
                )

                // Admin commands
                if (sender.hasPermission("territory.admin")) {
                    commands.addAll(listOf("stone", "scroll", "reload", "endwar", "startwar"))
                }

                commands.filter { it.startsWith(args[0].lowercase()) }
            }

            2 -> {
                // Second argument - depends on first command
                when (args[0].lowercase()) {
                    "stats", "stones", "history", "endwar" -> {
                        // Team names
                        plugin.configManager.getTeamIds()
                            .filter { it.startsWith(args[1].lowercase()) }
                    }

                    "score" -> {
                        // War numbers (1, 2, 3, ...)
                        val warCount = plugin.databaseManager.getWarCount()
                        (1..warCount).map { it.toString() }
                            .filter { it.startsWith(args[1]) }
                    }

                    else -> null
                }
            }

            else -> null
        }
    }
}

