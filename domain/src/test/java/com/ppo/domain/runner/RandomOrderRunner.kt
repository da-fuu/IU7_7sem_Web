package com.ppo.domain.runner

import io.qameta.allure.junit4.AllureJunit4
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
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
