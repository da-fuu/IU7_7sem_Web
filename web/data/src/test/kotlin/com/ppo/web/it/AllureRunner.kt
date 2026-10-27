package com.ppo.web.it

import io.qameta.allure.junit4.AllureJunit4
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner

class AllureRunner(clazz: Class<*>) : BlockJUnit4ClassRunner(clazz) {
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
