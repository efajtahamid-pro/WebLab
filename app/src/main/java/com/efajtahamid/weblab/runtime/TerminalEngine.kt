package com.efajtahamid.weblab.runtime

import com.efajtahamid.weblab.security.PathValidator
import java.io.File

/**
 * Executes real filesystem commands against a project's sandbox, and hands off
 * `node`/`npm`/`npx` invocations to [NodeRuntimeManager]. Every command that
 * touches a path is resolved through [PathValidator] first — there is no way
 * to `cd ../..` or `rm ../../something` out of the project directory.
 */
class TerminalEngine(
    private val projectRoot: File,
    private val runtimeManager: NodeRuntimeManager
) {
    /** Current working directory, expressed relative to the project root ("" = root). */
    var currentRelativeDir: String = ""
        private set

    private fun resolveCwd(): File = PathValidator.resolveSafe(projectRoot, currentRelativeDir)

    sealed class CommandOutcome {
        data class Immediate(val text: String, val isError: Boolean = false) : CommandOutcome()
        data class LaunchedProcess(val processId: String) : CommandOutcome()
        object Cleared : CommandOutcome()
    }

    suspend fun execute(rawLine: String): CommandOutcome {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) return CommandOutcome.Immediate("")

        val tokens = tokenize(trimmed)
        val cmd = tokens.firstOrNull() ?: return CommandOutcome.Immediate("")
        val args = tokens.drop(1)

        return try {
            when (cmd) {
                "pwd" -> CommandOutcome.Immediate("/${currentRelativeDir}".let { if (it == "/") "/" else it })
                "cd" -> cd(args)
                "ls" -> ls(args)
                "mkdir" -> mkdir(args)
                "rm" -> rm(args)
                "cp" -> cp(args)
                "mv" -> mv(args)
                "cat" -> cat(args)
                "echo" -> CommandOutcome.Immediate(args.joinToString(" "))
                "clear" -> CommandOutcome.Cleared
                "node" -> launchNode(args)
                "npm" -> launchNpm(args)
                "npx" -> launchNpx(args)
                else -> CommandOutcome.Immediate("command not found: $cmd", isError = true)
            }
        } catch (e: PathValidator.UnsafePathException) {
            CommandOutcome.Immediate(e.message ?: "Path escapes project sandbox", isError = true)
        } catch (e: Exception) {
            CommandOutcome.Immediate(e.message ?: "Command failed", isError = true)
        }
    }

    private fun tokenize(line: String): List<String> {
        // Minimal shell-style tokenizer supporting "quoted strings".
        val regex = Regex("\"([^\"]*)\"|'([^']*)'|(\\S+)")
        return regex.findAll(line).map { it.groupValues[1].ifEmpty { it.groupValues[2].ifEmpty { it.groupValues[3] } } }.toList()
    }

    private fun cd(args: List<String>): CommandOutcome {
        val target = args.firstOrNull() ?: run { currentRelativeDir = ""; return CommandOutcome.Immediate("") }
        val nextRelative = when (target) {
            "." -> currentRelativeDir
            ".." -> currentRelativeDir.substringBeforeLast('/', "")
            "/", "~" -> ""
            else -> if (currentRelativeDir.isEmpty()) target else "$currentRelativeDir/$target"
        }
        val resolved = PathValidator.resolveSafe(projectRoot, nextRelative)
        if (!resolved.exists() || !resolved.isDirectory) {
            return CommandOutcome.Immediate("cd: no such directory: $target", isError = true)
        }
        currentRelativeDir = resolved.relativeTo(projectRoot).path.let { if (it == ".") "" else it }
        return CommandOutcome.Immediate("")
    }

    private fun ls(args: List<String>): CommandOutcome {
        val dir = if (args.isEmpty()) resolveCwd() else PathValidator.resolveSafe(projectRoot, joinCwd(args[0]))
        val entries = dir.listFiles()?.sortedBy { it.name } ?: return CommandOutcome.Immediate("ls: not a directory", isError = true)
        val listing = entries.joinToString("  ") { if (it.isDirectory) "${it.name}/" else it.name }
        return CommandOutcome.Immediate(listing)
    }

    private fun mkdir(args: List<String>): CommandOutcome {
        val name = args.firstOrNull() ?: return CommandOutcome.Immediate("mkdir: missing operand", isError = true)
        val target = PathValidator.resolveSafe(projectRoot, joinCwd(name))
        return if (target.mkdirs()) CommandOutcome.Immediate("") else CommandOutcome.Immediate("mkdir: could not create $name", isError = true)
    }

    private fun rm(args: List<String>): CommandOutcome {
        val recursive = args.contains("-r") || args.contains("-rf")
        val name = args.firstOrNull { !it.startsWith("-") } ?: return CommandOutcome.Immediate("rm: missing operand", isError = true)
        val target = PathValidator.resolveSafe(projectRoot, joinCwd(name))
        if (target == projectRoot) return CommandOutcome.Immediate("rm: refusing to remove project root", isError = true)
        val ok = if (target.isDirectory) {
            if (!recursive) return CommandOutcome.Immediate("rm: $name is a directory (use -r)", isError = true)
            target.deleteRecursively()
        } else target.delete()
        return if (ok) CommandOutcome.Immediate("") else CommandOutcome.Immediate("rm: could not remove $name", isError = true)
    }

    private fun cp(args: List<String>): CommandOutcome {
        if (args.size < 2) return CommandOutcome.Immediate("cp: missing operand", isError = true)
        val src = PathValidator.resolveSafe(projectRoot, joinCwd(args[0]))
        val dest = PathValidator.resolveSafe(projectRoot, joinCwd(args[1]))
        src.copyRecursively(dest, overwrite = true)
        return CommandOutcome.Immediate("")
    }

    private fun mv(args: List<String>): CommandOutcome {
        if (args.size < 2) return CommandOutcome.Immediate("mv: missing operand", isError = true)
        val src = PathValidator.resolveSafe(projectRoot, joinCwd(args[0]))
        val dest = PathValidator.resolveSafe(projectRoot, joinCwd(args[1]))
        return if (src.renameTo(dest)) CommandOutcome.Immediate("") else CommandOutcome.Immediate("mv: failed", isError = true)
    }

    private fun cat(args: List<String>): CommandOutcome {
        val name = args.firstOrNull() ?: return CommandOutcome.Immediate("cat: missing operand", isError = true)
        val target = PathValidator.resolveSafe(projectRoot, joinCwd(name))
        if (!target.exists() || target.isDirectory) return CommandOutcome.Immediate("cat: $name: no such file", isError = true)
        return CommandOutcome.Immediate(target.readText())
    }

    private suspend fun launchNode(args: List<String>): CommandOutcome {
        val id = java.util.UUID.randomUUID().toString()
        runtimeManager.startProcess(
            id = id, label = "node ${args.joinToString(" ")}", command = args,
            workingDirectory = resolveCwd(), useNode = true
        )
        return CommandOutcome.LaunchedProcess(id)
    }

    private suspend fun launchNpm(args: List<String>): CommandOutcome {
        val id = java.util.UUID.randomUUID().toString()
        runtimeManager.startProcess(
            id = id, label = "npm ${args.joinToString(" ")}", command = args,
            workingDirectory = resolveCwd(), useNpm = true
        )
        return CommandOutcome.LaunchedProcess(id)
    }

    private suspend fun launchNpx(args: List<String>): CommandOutcome {
        val id = java.util.UUID.randomUUID().toString()
        runtimeManager.startProcess(
            id = id, label = "npx ${args.joinToString(" ")}", command = listOf("npx") + args,
            workingDirectory = resolveCwd(), useNode = true
        )
        return CommandOutcome.LaunchedProcess(id)
    }

    private fun joinCwd(name: String): String = if (currentRelativeDir.isEmpty()) name else "$currentRelativeDir/$name"
}
