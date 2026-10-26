package com.ppo.web.data.runner

import com.ppo.web.data.fixtures.CustomFixture
import com.ppo.web.data.fixtures.DatabaseFixtureContext
import io.qameta.allure.junit4.AllureJunit4
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import java.util.Collections
import java.util.Random

class RandomOrderRunner(clazz: Class<*>) : BlockJUnit4ClassRunner(clazz) {
    override fun computeTestMethods(): MutableList<FrameworkMethod> {
        val methods = super.computeTestMethods().toMutableList()
        val seedProperty = System.getProperty("test.random.seed")
        val seed = seedProperty?.toLongOrNull() ?: System.currentTimeMillis()
        Collections.shuffle(methods, Random(seed))
        return methods
    }

    override fun methodBlock(method: FrameworkMethod): Statement {
        val statement = super.methodBlock(method)
        return object : Statement() {
            override fun evaluate() {
                DatabaseFixtureContext.setup(method)
                try {
                    statement.evaluate()
                } finally {
                    DatabaseFixtureContext.clear()
                }
            }
        }
    }

    override fun withBefores(method: FrameworkMethod, target: Any, statement: Statement): Statement {
        val standardBefores = super.withBefores(method, target, statement)
        return object : Statement() {
            override fun evaluate() {
                val fixtureMethods = target.javaClass.methods.filter {
                    it.isAnnotationPresent(CustomFixture::class.java)
                }
                for (m in fixtureMethods) {
                    m.isAccessible = true
                    m.invoke(target)
                }
                standardBefores.evaluate()
            }
        }
    }

    override fun run(notifier: RunNotifier) {
        val listener = AllureJunit4()
        notifier.addListener(listener)
        try {
            super.run(notifier)
        } finally {
            notifier.removeListener(listener)
        }
    }
}
