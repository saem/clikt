package com.github.ajalt.clikt.parameters

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.testing.splitArgv
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.tables.row
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.contrib.java.lang.system.RestoreSystemProperties
import java.io.File


class EnvvarOptionsTest {
    @Rule
    @JvmField
    val env = EnvironmentVariables()

    @Rule
    @JvmField
    val restoreSystemProperties = RestoreSystemProperties()

    @Test
    fun `explicit envvar`() {
        env["FO"] = "foo"

        class C : CliktCommand() {
            val foo by option(envvar = "FO")
            val bar by option()
            override fun run() {
                foo shouldBe "foo"
                bar shouldBe null
            }
        }

        C().parse(emptyArray())
    }

    @Test
    fun `auto envvar`() {
        env["FO"] = "foo"
        env["FO"] = "foo"
        env["C_BAR"] = "11"

        class C : CliktCommand() {
            val foo by option(envvar = "FO")
            val bar by option().int()
            val baz by option()
            override fun run() {
                foo shouldBe "foo"
                bar shouldBe 11
                baz shouldBe null
            }
        }

        C().context { autoEnvvarPrefix = "C" }.parse(emptyArray())
    }

    @Test
    fun `auto envvar subcommand`() {
        env["FOO"] = "foo"
        env["C_CMD1_BAR"] = "bar"
        env["BAZ"] = "baz"
        env["CMD2_QUX"] = "qux"
        env["CMD2_SUB3_QUZ"] = "quz"

        class C : NoRunCliktCommand() {
            init {
                context { autoEnvvarPrefix = "C" }
            }
        }

        class Sub : CliktCommand(name = "cmd1") {
            val foo by option(envvar = "FOO")
            val bar by option()
            override fun run() {
                foo shouldBe "foo"
                bar shouldBe "bar"
            }
        }

        class Sub2 : CliktCommand() {
            init {
                context { autoEnvvarPrefix = "CMD2" }
            }

            val baz by option(envvar = "BAZ")
            val qux by option()
            override fun run() {
                baz shouldBe "baz"
                qux shouldBe "qux"
            }
        }

        class Sub3 : CliktCommand() {
            val quz by option()
            override fun run() {
                quz shouldBe "quz"
            }
        }

        C().subcommands(Sub().subcommands(Sub2().subcommands(Sub3())))
                .parse(splitArgv("cmd1 sub2 sub3"))
    }

    @Test
    fun `file envvar`() {
        env["FOO"] = "/home"

        class C : CliktCommand() {
            val foo by option(envvar = "FOO").file()
            val bar by option(envvar = "BAR").file().multiple()
            override fun run() {
                foo shouldBe File("/home")
                bar shouldBe listOf(File("/bar"), File("/baz"))
            }
        }

        System.setProperty("os.name", "Microsoft Windows 10 PRO")
        env["BAR"] = "/bar;/baz"
        C().parse(emptyArray())

        System.setProperty("os.name", "OpenBSD")
        env["BAR"] = "/bar:/baz"
        C().parse(emptyArray())
    }

    @Test
    fun `flag envvars`() = forall(
            row(null, null, false, 0),
            row("YES", "3", true, 3),
            row("false", "5", false, 5)
    ) { fv, bv, ef, eb ->

        env["FOO"] = fv
        env["BAR"] = bv

        var called1 = false
        var called2 = false

        class C : CliktCommand() {
            val foo by option(envvar = "FOO").flag("--no-foo").validate { called1 = true }
            val bar by option(envvar = "BAR").counted().validate { called2 = true }
            override fun run() {
                foo shouldBe ef
                bar shouldBe eb
            }
        }

        C().parse(emptyArray())
        called1 shouldBe true
        called2 shouldBe true
    }
}
