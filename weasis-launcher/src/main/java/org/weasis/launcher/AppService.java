package org.weasis.launcher;

import javafx.application.Application;

@FunctionalInterface
public interface AppService {
    Application getWeasisApp();
}
