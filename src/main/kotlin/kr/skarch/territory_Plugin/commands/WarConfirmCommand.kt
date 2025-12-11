package kr.skarch.territory_Plugin.commands

import kr.skarch.territory_Plugin.Territory_Plugin
import kr.skarch.territory_Plugin.listeners.WarDeclarationListener
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class WarConfirmCommand(
    private val plugin: Territory_Plugin,
    private val warListener: WarDeclarationListener
) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§c사용법: /war-confirm <yes|no>")
            return true
        }

        when (args[0].lowercase()) {
            "yes" -> warListener.confirmWar(sender, true)
            "no" -> warListener.confirmWar(sender, false)
            else -> sender.sendMessage("§c사용법: /war-confirm <yes|no>")
        }

        return true
    }
}

